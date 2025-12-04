/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.text.DefaultEditorKit;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.core.ui.api.Actions;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.api.ActionSupport;
import org.praxislive.ide.pxr.spi.RootEditor;

/**
 *
 */
@Messages({
    "LBL_TableNodeColumn=Components"
})
class TableRootEditor implements RootEditor {

    private static final boolean IS_DARK = UIManager.getBoolean("nb.dark.theme");
    private static final Color BACKGROUND
            = findColor("praxis.table.background",
                    IS_DARK ? new Color(0x2b2b2b) : new Color(0xf0f0f0));

    private final RootProxy root;
    private final EditorPanel editorComponent;
    private final ExplorerManager baseEM;
    private final ExplorerManager tableEM;
    private final ExplorerSync explorerSync;
    private final Set<Syncable> syncables;
    private final WeakHashMap<Node, TableNode> tableNodeCache;

    private final Action addAction;
    private final Action copyAction;
    private final Action deleteAction;
    private final Action duplicateAction;
    private final Action pasteAction;
    private final Action sharedCodeAction;

    TableRootEditor(RootProxy root, RootEditor.Context context, List<String> properties) {
        this.root = root;
        this.baseEM = context.explorerManager();
        this.tableEM = new ExplorerManager();
        this.explorerSync = new ExplorerSync();
        this.syncables = new HashSet<>();
        this.tableNodeCache = new WeakHashMap<>();

        this.addAction = org.openide.awt.Actions.forID("PXR", "org.praxislive.ide.pxr.AddChildAction");
        this.copyAction = ActionSupport.createCopyAction(this, baseEM);
        this.deleteAction = ActionSupport.createDeleteAction(this, baseEM);
        this.duplicateAction = ActionSupport.createDuplicateAction(this, baseEM);
        this.pasteAction = ActionSupport.createPasteAction(this, baseEM);
        this.sharedCodeAction = context.sharedCodeAction().orElse(null);

        this.editorComponent = new EditorPanel(initView(properties), this.tableEM);

        Node rootNode = root.getNodeDelegate();
        TableNode tableRoot = new TableNode(rootNode);
        this.tableNodeCache.put(rootNode, tableRoot);
        this.tableEM.setRootContext(tableRoot);
        this.baseEM.addPropertyChangeListener(explorerSync);
        this.tableEM.addPropertyChangeListener(explorerSync);

    }

    private OutlineView initView(List<String> properties) {
        OutlineView ov = new OutlineView(Bundle.LBL_TableNodeColumn());
        for (String property : properties) {
            ov.addPropertyColumn(property, property);
        }
        ov.setDragSource(false);
        ov.setQuickSearchAllowed(false);
        ov.setTreeSortable(false);
        ov.getOutline().setBackground(BACKGROUND);
        return ov;
    }

    @Override
    public JComponent getEditorComponent() {
        return editorComponent;
    }

    @Override
    public void dispose() {
        syncables.forEach(s -> s.removeKey(this));
        syncables.clear();
        baseEM.removePropertyChangeListener(explorerSync);
        tableEM.setRootContext(Node.EMPTY);
        tableNodeCache.clear();
    }

    @Override
    public boolean requestFocus() {
        return editorComponent.view.requestFocusInWindow();
    }

    @Override
    public Set<ToolAction> supportedToolActions() {
        EnumSet<ToolAction> tools = EnumSet.allOf(ToolAction.class);
        tools.remove(ToolAction.CONNECT);
        tools.remove(ToolAction.DISCONNECT);
        return tools;
    }

    private void registerSyncable(Syncable syncable) {
        if (syncables.add(syncable)) {
            syncable.addKey(this);
        }
    }

    private void clearSyncable(Syncable syncable) {
        syncables.remove(syncable);
        syncable.removeKey(this);
    }

    private static Color findColor(String uiKey, Color fallback) {
        Color color = UIManager.getColor(uiKey);
        return color == null ? fallback : color;
    }

    private class EditorPanel extends JPanel implements ExplorerManager.Provider {

        private final OutlineView view;
        private final ExplorerManager explorerManager;

        private EditorPanel(OutlineView view, ExplorerManager explorerManager) {
            this.view = view;
            this.explorerManager = explorerManager;
            setLayout(new BorderLayout());
            add(view, BorderLayout.CENTER);
            ActionMap am = view.getOutline().getActionMap();
            am.put("delete", deleteAction);
            am.put(DefaultEditorKit.copyAction, copyAction);
            am.put(DefaultEditorKit.pasteAction, pasteAction);
            am.put(Actions.DUPLICATE_KEY, duplicateAction);
            // outlineview adds its own copy action to input map - remove
            removeViewCopy(view.getOutline().getInputMap(WHEN_FOCUSED));
            removeViewCopy(view.getOutline().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT));
        }

        private void removeViewCopy(InputMap map) {
            if (Utilities.isMac()) {
                map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), "none");
            } else {
                map.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "none");
            }
            map.put(KeyStroke.getKeyStroke("COPY"), "none");
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return explorerManager;
        }

    }

    private class ExplorerSync implements PropertyChangeListener {

        private boolean ignoreChanges;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ignoreChanges) {
                return;
            }
            try {
                ignoreChanges = true;
                if (evt.getSource() == baseEM) {
                    syncFromBaseEM();
                } else {
                    syncFromTableEM();
                }
            } catch (PropertyVetoException ex) {
                // no op
            } finally {
                ignoreChanges = false;
            }
        }

        private void syncFromBaseEM() throws PropertyVetoException {
            Node context = baseEM.getExploredContext();
            Node[] selection = baseEM.getSelectedNodes();
            TableNode tableContext = tableNodeCache.get(context);
            if (tableContext == null) {
                return;
            }
            TableNode[] tableSelection = Stream.of(selection)
                    .map(tableNodeCache::get)
                    .filter(n -> n != null)
                    .toArray(TableNode[]::new);
            tableEM.setExploredContextAndSelection(tableContext, tableSelection);
        }

        private void syncFromTableEM() throws PropertyVetoException {
            Node[] selection = tableEM.getSelectedNodes();
            Node context = findContext(selection);
            if (context instanceof TableNode tableContext) {
                Node baseContext = tableContext.getOriginal();
                Node[] baseSelection = Stream.of(selection)
                        .filter(TableNode.class::isInstance)
                        .map(TableNode.class::cast)
                        .map(TableNode::getOriginal)
                        .toArray(Node[]::new);
                baseEM.setExploredContextAndSelection(baseContext, baseSelection);
            }
        }

        private Node findContext(Node[] selection) {
            Node root = tableEM.getRootContext();
            if (selection.length == 0) {
                return root;
            } else if (selection.length == 1) {
                Node n = selection[0];
                Node c = n.isLeaf() ? n.getParentNode() : n;
                return c == null ? root : c;
            } else {
                Node p = selection[0].getParentNode();
                if (p == null) {
                    return root;
                }
                for (int i = 1; i < selection.length; i++) {
                    if (selection[i].getParentNode() != p) {
                        return root;
                    }
                }
                return p;
            }
        }

    }

    private class TableNode extends FilterNode {

        private final Syncable syncable;

        TableNode(Node original) {
            super(original, original.isLeaf() ? Children.LEAF : new TableChildren(original));
            syncable = original.getLookup().lookup(Syncable.class);
            if (syncable != null) {
                registerSyncable(syncable);
                addNodeListener(new org.openide.nodes.NodeAdapter() {

                    @Override
                    public void nodeDestroyed(NodeEvent ev) {
                        clearSyncable(syncable);
                    }

                });
            }
            if (!original.isLeaf()) {
                EventQueue.invokeLater(() -> editorComponent.view.expandNode(this));
            }
        }

        @Override
        protected PropertyChangeListener createPropertyChangeListener() {
            return new PropertyChangeAdapter(this) {
                @Override
                protected void propertyChange(FilterNode fn, PropertyChangeEvent ev) {
                    if (editorComponent.view.getOutline().isEditing()) {
                        return;
                    }
                    super.propertyChange(fn, ev);
                }

            };
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> actions = new ArrayList<>();
            if (!isLeaf()) {
                actions.add(addAction);
                actions.add(null);
            }
            actions.addAll(Arrays.asList(super.getActions(context)));
            actions.add(null);
            actions.add(copyAction);
            actions.add(duplicateAction);
            if (!isLeaf()) {
                actions.add(pasteAction);
            }
            actions.add(null);
            actions.add(deleteAction);
            if (sharedCodeAction != null) {
                actions.add(null);
                actions.add(sharedCodeAction);
            }
            return actions.toArray(Action[]::new);
        }

        @Override
        protected Node getOriginal() {
            return super.getOriginal();
        }

    }

    private class TableChildren extends FilterNode.Children {

        TableChildren(Node parent) {
            super(parent);
        }

        @Override
        protected Node[] createNodes(Node key) {
            TableNode tableNode = new TableNode(key);
            tableNodeCache.put(key, tableNode);
            return new Node[]{tableNode};
        }

    }

}
