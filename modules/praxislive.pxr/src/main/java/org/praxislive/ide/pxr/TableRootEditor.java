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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.util.Lookup;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.spi.RootEditor;

/**
 *
 */
class TableRootEditor implements RootEditor {

    private final RootProxy root;
    private final EditorPanel editorComponent;
    private final ExplorerManager explorerManager;
    private final Lookup lookup;
    private final Set<Syncable> syncables;

    TableRootEditor(RootProxy root, List<String> properties) {
        this.root = root;
        this.explorerManager = new ExplorerManager();
        this.syncables = new HashSet<>();
        this.editorComponent = new EditorPanel(initView(properties), explorerManager);
        this.explorerManager.setRootContext(new TableNode(root.getNodeDelegate()));
        this.lookup = ExplorerUtils.createLookup(explorerManager, editorComponent.getActionMap());
    }

    private OutlineView initView(List<String> properties) {
        OutlineView ov = new OutlineView();
        for (String property : properties) {
            ov.addPropertyColumn(property, property);
        }
        ov.setDragSource(false);
        return ov;
    }

    @Override
    public JComponent getEditorComponent() {
        return editorComponent;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
    @Override
    public void dispose() {
        syncables.forEach(s -> s.removeKey(this));
        syncables.clear();
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
    
    private class EditorPanel extends JPanel implements ExplorerManager.Provider {

        private final OutlineView view;
        private final ExplorerManager explorerManager;

        private EditorPanel(OutlineView view, ExplorerManager explorerManager) {
            this.view = view;
            this.explorerManager = explorerManager;
            setLayout(new BorderLayout());
            add(view, BorderLayout.CENTER);
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return explorerManager;
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
        
        
        
    }

    private class TableChildren extends FilterNode.Children {

        TableChildren(Node parent) {
            super(parent);
        }

        @Override
        protected Node[] createNodes(Node key) {
            return new Node[]{new TableNode(key)};
        }

    }

}
