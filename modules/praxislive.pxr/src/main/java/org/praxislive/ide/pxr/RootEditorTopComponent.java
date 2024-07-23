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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.openide.awt.Actions;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Value;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
public class RootEditorTopComponent extends CloneableTopComponent {

    private final static Action START_STOP_ACTION = new StartableRootAction();
    private final static Action ROOT_CONFIG_ACTION = new RootConfigAction();

    private final PXRDataObject dob;
    private final EditorLookup lookup;
    private final PropertyChangeListener registryListener;
    private final PropertyChangeListener infoListener;
    private final JToolBar toolBar;

    private JComponent editorPanel;
    private RootEditor editor;
    private PXRRootProxy root;
    private PMap editorHint;

    public RootEditorTopComponent(PXRDataObject dob) {
        this.setDisplayName(dob.getName());
        this.setIcon(dob.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16));
        this.dob = dob;
        lookup = new EditorLookup(Lookups.singleton(dob), dob.getLookup());
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
            requestFocusInWindow();
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

//    @Override
//    public void requestFocus() {
//        super.requestFocus();
//        if (editorPanel != null) {
//            editorPanel.requestFocus();
//        }
//    }
//
//    @Override
//    public boolean requestFocusInWindow() {
//        super.requestFocusInWindow();
//        if (editorPanel != null) {
//            return editorPanel.requestFocusInWindow();
//        } else {
//            return false;
//        }
//    }
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

    PMap findEditorHint() {
        return Optional.ofNullable(root)
                .map(r -> r.getInfo().properties().get(ComponentInfo.KEY_DISPLAY_HINT))
                .flatMap(PMap::from)
                .orElse(PMap.EMPTY);
    }

    private void install(PXRRootProxy root) {
        if (root == null) {
            editor = new BlankEditor(dob);
            lookup.setAdditional(editor.getLookup());
            initToolbar(List.of());
        } else {
            editor = findEditor(root);
            lookup.setAdditional(
                    Lookups.singleton(new PXRRootContext(root)),
                    editor.getLookup());
            initToolbar(buildActions(editor));
        }
        editorPanel = editor.getEditorComponent();
        add(editorPanel);
        if (isVisible()) {
            editor.componentShowing();
            editor.componentActivated();
        }

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
        remove(editorPanel);
        editorPanel = null;
        editor.dispose();
        editor = null;
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

    private RootEditor findEditor(PXRRootProxy root) {

        String editorType = editorHint.getString("type", "");
        if ("table".equals(editorType)) {
            List<String> props = Optional.ofNullable(editorHint.get("properties"))
                    .flatMap(PArray::from)
                    .map(a -> a.asListOf(String.class))
                    .orElse(List.of());
            return new TableRootEditor(root, props);
        }

        EditorContext context = new EditorContext(this, root);

        return Lookup.getDefault().lookupAll(RootEditor.Provider.class).stream()
                .flatMap(p -> p.createEditor(root, context).stream())
                .findFirst()
                .orElse(new TableRootEditor(root, List.of()));

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

        private Lookup[] permanent;

        private EditorLookup(Lookup... lookups) {
            super(lookups);
            this.permanent = lookups;
        }

        private void setAdditional(Lookup... lookups) {
            if (lookups == null || lookups.length == 0) {
                setLookups(permanent);
            } else {
                List<Lookup> lst = new ArrayList<Lookup>();
                lst.addAll(Arrays.asList(permanent));
                lst.addAll(Arrays.asList(lookups));
                setLookups(lst.toArray(new Lookup[lst.size()]));
            }
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

        EditorContext(RootEditorTopComponent container, PXRRootProxy root) {
            this.container = container;
            this.file = root.getSource().getPrimaryFile();
            this.project = root.getProject();
        }

        @Override
        public TopComponent container() {
            return container;
        }

        @Override
        public Optional<FileObject> file() {
            return Optional.of(file);
        }

        @Override
        public Optional<PraxisProject> project() {
            return Optional.of(project);
        }

    }
}
