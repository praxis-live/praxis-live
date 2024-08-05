/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.beans.BeanInfo;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.openide.awt.Actions;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.code.api.SharedCodeInfo;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.pxr.palette.DefaultComponentPalette;

/**
 *
 */
public class RootEditorTopComponent extends CloneableTopComponent {

    private final static Action START_STOP_ACTION
            = Actions.forID("PXR", StartableRootAction.class.getName());
    private final static Action ROOT_CONFIG_ACTION
            = Actions.forID("PXR", RootConfigAction.class.getName());

    private final PXRDataObject dob;
    private final EditorLookup lookup;
    private final PropertyChangeListener registryListener;
    private final PropertyChangeListener infoListener;
    private final JToolBar toolBar;
    private final ExplorerManager explorerManager;
    private final EditorHolder editorHolder;

    private JComponent editorComponent;
    private RootEditor editor;
    private PXRRootProxy root;
    private PMap editorHint;
    private ActionEditorContext actionContext;
    private DefaultComponentPalette palette;

    public RootEditorTopComponent(PXRDataObject dob) {
        this.setDisplayName(dob.getName());
        this.setIcon(dob.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16));
        this.dob = dob;
        this.explorerManager = new ExplorerManager();
        this.explorerManager.addPropertyChangeListener(evt -> {
            if (root == null || actionContext == null || palette == null) {
                return;
            }
            PXRContainerProxy context
                    = Optional.ofNullable(explorerManager.getExploredContext())
                            .map(n -> n.getLookup().lookup(PXRContainerProxy.class))
                            .orElse(root);
            List<PXRComponentProxy> selection
                    = Stream.of(explorerManager.getSelectedNodes())
                            .map(n -> n.getLookup().lookup(PXRComponentProxy.class))
                            .filter(c -> c != null)
                            .toList();
            palette.context(context);
            actionContext.updateSelection(context, selection);
        });

        lookup = new EditorLookup(Lookups.singleton(dob),
                ExplorerUtils.createLookup(explorerManager, getActionMap()),
                dob.getLookup());
        associateLookup(lookup);
        setLayout(new BorderLayout());
        toolBar = new ToolBar();
        add(toolBar, BorderLayout.NORTH);
        registryListener = evt -> checkRoot();
        infoListener = evt -> {
            if (ComponentProtocol.INFO.equals(evt.getPropertyName())) {
                checkEditorHint();
            }
        };
        editorHint = PMap.EMPTY;
        editorHolder = new EditorHolder(explorerManager);
        add(editorHolder);
    }

    @Override
    public void requestFocus() {
        if (editor != null) {
            editor.requestFocus();
        } else {
            super.requestFocus();
        }
    }

    @Override
    public boolean requestFocusInWindow() {
        if (editor != null) {
            return editor.requestFocus();
        } else {
            return super.requestFocusInWindow();
        }
    }

    @Override
    protected void componentOpened() {
        assert EventQueue.isDispatchThread();
        root = null;
        editorHint = PMap.EMPTY;
        var registry = PXRRootRegistry.registryForFile(dob.getPrimaryFile());
        if (registry != null) {
            root = registry.getRootByFile(dob.getPrimaryFile());
            registry.addPropertyChangeListener(registryListener);
            if (root != null) {
                root.addPropertyChangeListener(infoListener);
                editorHint = findEditorHint();
            }
        }
        install(root);
    }

    @Override
    protected void componentShowing() {
        if (editor != null) {
            editor.componentShowing();
        }
    }

    @Override
    protected void componentActivated() {
        if (editor != null) {
            editor.componentActivated();
        }
    }

    @Override
    protected void componentDeactivated() {
        if (editor != null) {
            editor.componentDeactivated();
        }
    }

    @Override
    protected void componentHidden() {
        if (editor != null) {
            editor.componentHidden();
        }
    }

    @Override
    protected void componentClosed() {
        syncEditor();
        var registry = PXRRootRegistry.registryForFile(dob.getPrimaryFile());
        if (registry != null) {
            registry.removePropertyChangeListener(registryListener);
        }
        if (root != null) {
            root.removePropertyChangeListener(infoListener);
        }
        uninstall(root);
    }

    @Override
    protected CloneableTopComponent createClonedObject() {
        return new RootEditorTopComponent(dob);
    }

    void syncEditor() {
        if (editor != null) {
            editor.sync();
        }
    }

    private void checkRoot() {
        PXRRootProxy root = PXRRootRegistry.findRootForFile(dob.getPrimaryFile());
        if (root == this.root) {
            return;
        }
        if (root == null) {
            if (this.root != null) {
                this.root.removePropertyChangeListener(infoListener);
                this.root = null;
            }
            close();
        } else {
            if (this.root != null) {
                this.root.removePropertyChangeListener(infoListener);
            }
            uninstall(this.root);
            this.root = root;
            this.editorHint = findEditorHint();
            root.addPropertyChangeListener(infoListener);
            install(root);
        }
    }

    private void checkEditorHint() {
        PMap hint = findEditorHint();
        if (!Objects.equals(editorHint, hint)) {
            editorHint = hint;
            uninstall(root);
            install(root);
        }

    }

    private PMap findEditorHint() {
        return Optional.ofNullable(root)
                .map(r -> r.getInfo().properties().get(ComponentInfo.KEY_DISPLAY_HINT))
                .flatMap(PMap::from)
                .orElse(PMap.EMPTY);
    }

    private void actionContextSelect(PXRContainerProxy container, List<PXRComponentProxy> children) {
        try {
            Node context = container.getNodeDelegate();
            Node[] selection = children.stream()
                    .map(ComponentProxy::getNodeDelegate)
                    .toArray(Node[]::new);
            explorerManager.setExploredContextAndSelection(context, selection);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private void install(PXRRootProxy root) {
        SharedCodePanel sharedCodePanel = null;
        ToolActions toolActions = null;
        if (root == null) {
            explorerManager.setRootContext(Node.EMPTY);
            editor = new BlankEditor(dob);
            lookup.setAdditional(editor.getLookup());
            initToolbar(List.of());
        } else {
            explorerManager.setRootContext(root.getNodeDelegate());
            sharedCodePanel = findSharedCode(root);
            EditorContext context = new EditorContext(this, root,
                    sharedCodePanel == null ? null : sharedCodePanel.getToggleAction()
            );
            editor = findEditor(context, root);
            palette = DefaultComponentPalette.create(root);
            actionContext = new ActionEditorContext(editor, root,
                    palette.root(), this::actionContextSelect);
            toolActions = new ToolActions(actionContext, editor.supportedToolActions());
            lookup.setAdditional(
                    Lookups.fixed(actionContext, palette.controller()),
                    Lookups.exclude(editor.getLookup(), ActionMap.class));
            initToolbar(buildActions(editor));
        }
        editorComponent = editor.getEditorComponent();
        editorHolder.install(editorComponent, sharedCodePanel, toolActions);
        if (isVisible()) {
            editor.componentShowing();
            editor.componentActivated();
        }

    }

    private SharedCodePanel findSharedCode(PXRRootProxy root) {
        SharedCodeInfo sharedInfo = root.getLookup().lookup(SharedCodeInfo.class);
        if (sharedInfo == null) {
            return null;
        }
        return new SharedCodePanel(sharedInfo.getFolder());
    }

    private RootEditor findEditor(EditorContext context, PXRRootProxy root) {

        String editorType = editorHint.getString("type", "");
        if ("table".equals(editorType)) {
            List<String> props = Optional.ofNullable(editorHint.get("properties"))
                    .flatMap(PArray::from)
                    .map(a -> a.asListOf(String.class))
                    .orElse(List.of());
            return new TableRootEditor(root, context, props);
        }

        return Lookup.getDefault().lookupAll(RootEditor.Provider.class).stream()
                .flatMap(p -> p.createEditor(root, context).stream())
                .findFirst()
                .orElseGet(() -> new TableRootEditor(root, context, List.of()));

    }

    private List<Action> buildActions(RootEditor editor) {
        List<Action> editorActions = editor.getActions();
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(START_STOP_ACTION);
        actions.add(ROOT_CONFIG_ACTION);
        if (!editorActions.isEmpty()) {
            actions.add(null);
            actions.addAll(editorActions);
        }
        return actions;
    }

    private void uninstall(PXRRootProxy root) {
        editorHolder.uninstall();
        editorComponent = null;
        editor.dispose();
        editor = null;
        lookup.removeAdditional();
        if (palette != null) {
            palette.dispose();
            palette = null;
        }
        explorerManager.setRootContext(Node.EMPTY);
        actionContext = null;
    }

    private void initToolbar(List<Action> actions) {
        toolBar.removeAll();
        Lookup context = getLookup();
        for (Action action : actions) {
            if (action instanceof ContextAwareAction) {
                action = ((ContextAwareAction) action).createContextAwareInstance(context);
            }
            Component c;
            if (action instanceof Presenter.Toolbar) {
                c = ((Presenter.Toolbar) action).getToolbarPresenter();
            } else if (action == null) {
                c = new JToolBar.Separator();
            } else {
                JButton button = new JButton();
                Actions.connect(button, action);
                c = button;
            }
            if (c instanceof AbstractButton) {
                c.setFocusable(false);
            }
            toolBar.add(c);
        }
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    private static class EditorHolder extends JPanel implements ExplorerManager.Provider {

        private final ExplorerManager em;

        private EditorHolder(ExplorerManager em) {
            this.em = em;
            setLayout(new BorderLayout());
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return em;
        }

        private void install(JComponent editorComponent,
                SharedCodePanel sharedCodePanel,
                ToolActions toolActions) {
            removeAll();
            add(editorComponent, BorderLayout.CENTER);
            if (sharedCodePanel != null) {
                add(sharedCodePanel, BorderLayout.WEST);
            }
            if (toolActions != null) {
                toolActions.installActions(
                        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT),
                        getActionMap());
                add(toolActions.getActionPanel(), BorderLayout.SOUTH);
            }
        }

        private void uninstall() {
            removeAll();
            resetKeyboardActions();
        }

    }

    private static class BlankEditor implements RootEditor {

        private final PXRDataObject dob;

        private BlankEditor(PXRDataObject dob) {
            this.dob = dob;
        }

        @Override
        public Lookup getLookup() {
            return Lookups.singleton(dob.getNodeDelegate());
        }

        @Override
        public JComponent getEditorComponent() {
            JComponent editor = new JLabel("<Build " + dob.getName() + " to edit>", JLabel.CENTER);
            editor.setFocusable(true);
            return editor;
        }
    }

    private static final class EditorLookup extends ProxyLookup {

        private final List<Lookup> permanent;

        private EditorLookup(Lookup... lookups) {
            super(lookups);
            this.permanent = List.of(lookups);
        }

        private void setAdditional(Lookup... lookups) {
            if (lookups.length == 0) {
                setLookups(permanent.toArray(Lookup[]::new));
            } else {
                setLookups(Stream.concat(permanent.stream(),
                        Stream.of(lookups))
                        .toArray(Lookup[]::new));
            }
        }

        private void removeAdditional() {
            setAdditional();
        }
    }

    private static final class ToolBar extends JToolBar {

        ToolBar() {
            super("editorToolbar");
            setFocusable(false);
            setFloatable(false);
            setRollover(true);
            setBorder(BorderFactory.createEtchedBorder());
        }
    }

    private static final class EditorContext implements RootEditor.Context {

        private final RootEditorTopComponent container;
        private final FileObject file;
        private final PraxisProject project;
        private final Action showSharedCodeAction;

        EditorContext(RootEditorTopComponent container,
                PXRRootProxy root,
                Action showSharedCodeAction) {
            this.container = container;
            this.file = root.getSource().getPrimaryFile();
            this.project = root.getProject();
            this.showSharedCodeAction = showSharedCodeAction;
        }

        @Override
        public TopComponent container() {
            return container;
        }

        @Override
        public ExplorerManager explorerManager() {
            return container.explorerManager;
        }

        @Override
        public Optional<FileObject> file() {
            return Optional.of(file);
        }

        @Override
        public Optional<PraxisProject> project() {
            return Optional.of(project);
        }

        @Override
        public Optional<Action> sharedCodeAction() {
            return Optional.ofNullable(showSharedCodeAction);
        }

    }
}
