/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.neilcsmith.praxis.core.types.PResource;
import net.neilcsmith.praxis.core.types.PString;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.view.ContextTreeView;
import org.openide.explorer.view.ListView;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeNotFoundException;
import org.openide.nodes.NodeOp;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class ResourceCustomEditor extends javax.swing.JPanel
        implements ExplorerManager.Provider {

    private final static Logger LOG = Logger.getLogger(ResourceCustomEditor.class.getName());
    private final Listener listener;
    private final ExplorerManager em;
    private FileObject rootDir;
    private URI current;
    private final PropertyEnv env;
    private boolean ignoreChanges;
    private final ResourceEditor editor;

    /**
     * Creates new form ResourceCustomEditor
     */
    ResourceCustomEditor(ResourceEditor editor, FileObject workingDir, URI current, PropertyEnv env) {
        initComponents();

        this.current = current;
        this.env = env;
        this.editor = editor;
//        env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        em = new ExplorerManager();

        if (workingDir != null) {
            FileObject root = workingDir.getFileObject("resources");
            if (root == null) {
                root = workingDir;
            }
            try {
                em.setRootContext(new FileNode(DataObject.find(root).getNodeDelegate()));
                rootDir = root;
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        if (current != null) {
            uriField.setText(current.toString());
            syncEM();
        }

        listener = new Listener();

        env.addVetoableChangeListener(listener);
        uriField.getDocument().addDocumentListener(listener);
        em.addPropertyChangeListener(listener);

        initContextView((ContextTreeView) contextView);
        initFileListView((ListView) fileView);
    }

    private void initContextView(ContextTreeView view) {
        view.setDefaultActionAllowed(false);
    }

    private void initFileListView(ListView view) {
        view.setPopupAllowed(false);
        view.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//        view.setShowParentNode(true);
    }

    private void syncEM() {
        try {
            if (current != null && "file".equals(current.getScheme())
                    && rootDir != null) {
                FileObject fob = FileUtil.toFileObject(Utilities.toFile(current));
                if (fob != null && FileUtil.isParentOf(rootDir, fob)) {
                    Node sel = null;
                    Node root = em.getRootContext();
                    String relPath = FileUtil.getRelativePath(rootDir, fob);                  
                    String[] path = relPath.split("/");
                    path[path.length-1] = fob.getName();
                    sel = findNode(root, path);
                    if (sel == null) {
                        path[path.length-1] = fob.getNameExt();
                        sel = findNode(root, path);
                    }
                    if (sel != null) {
                        em.setExploredContextAndSelection(sel.getParentNode(),new Node[]{sel});
                    }
                    
                }

            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private Node findNode(Node start, String[] path) {
        Node node = null;
        try {
            node = NodeOp.findPath(start, path);
        } catch (NodeNotFoundException ex) {
        }
        return node;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        uriField = new javax.swing.JTextField();
        contextView = new org.openide.explorer.view.ContextTreeView();
        fileView = new org.openide.explorer.view.ListView();
        jToolBar1 = new javax.swing.JToolBar();
        clearButton = new javax.swing.JButton();
        browseButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        uriField.setText(org.openide.util.NbBundle.getMessage(ResourceCustomEditor.class, "ResourceCustomEditor.uriField.text")); // NOI18N

        jToolBar1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/pxr/resources/clear.png"))); // NOI18N
        clearButton.setText(org.openide.util.NbBundle.getMessage(ResourceCustomEditor.class, "ResourceCustomEditor.clearButton.text")); // NOI18N
        clearButton.setFocusable(false);
        clearButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        clearButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(clearButton);

        browseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/pxr/resources/open.png"))); // NOI18N
        browseButton.setText(org.openide.util.NbBundle.getMessage(ResourceCustomEditor.class, "ResourceCustomEditor.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(browseButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(contextView, javax.swing.GroupLayout.DEFAULT_SIZE, 195, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(fileView, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(uriField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(uriField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contextView, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE)
                    .addComponent(fileView))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File loc = null;
        if (current != null && "file".equals(current.getScheme())) {
            try {
                loc = Utilities.toFile(current);
            } catch (Exception e) {
            }
        }
        if (loc == null && rootDir != null) {
            try {
                loc = FileUtil.toFile(rootDir);
            } catch (Exception e) {
            }
        }
        FileChooserBuilder dlgBld = new FileChooserBuilder(ResourceCustomEditor.class);
        if (loc != null) {
            dlgBld.setDefaultWorkingDirectory(loc).forceUseOfDefaultWorkingDirectory(true);
        }
        dlgBld.setTitle("Choose File").setApproveText("OK").setFileHiding(true);
        File file = dlgBld.showOpenDialog();
        if (file != null) {
            current = file.toURI();
            ignoreChanges = true;
            uriField.setText(current.toString());
            env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            syncEM();
//            env.setState(PropertyEnv.STATE_VALID);
//            editor.setValue(PResource.valueOf(current));
            ignoreChanges = false;
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        uriField.setText("");
    }//GEN-LAST:event_clearButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JButton clearButton;
    private javax.swing.JScrollPane contextView;
    private javax.swing.JScrollPane fileView;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JTextField uriField;
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    private class Listener implements VetoableChangeListener, DocumentListener,
            PropertyChangeListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update(e);
        }

        private void update(DocumentEvent e) {
            if (ignoreChanges) {
                return;
            }
            String txt = uriField.getText();
            if (txt.isEmpty()) {
                current = null;
                env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
//                editor.setValue(PString.EMPTY);
            } else {
                try {
                    current = PResource.valueOf(uriField.getText()).value();
                    env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                    syncEM();
                } catch (Exception ex) {
                    env.setState(PropertyEnv.STATE_INVALID);
                }
            }


        }

        // called when OK button is pressed
        @Override
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            if (PropertyEnv.PROP_STATE.equals(evt.getPropertyName())
                    && PropertyEnv.STATE_VALID.equals(evt.getNewValue())) {
                if (current == null) {
                    editor.setValue(PString.EMPTY);
                } else {
                    editor.setValue(PResource.valueOf(current));
                }
            }
        }

        
        // called from EM
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                Node[] nodes = em.getSelectedNodes();
                if (nodes.length == 1) {
                    FileObject fob = nodes[0].getLookup().lookup(FileObject.class);
                    current = fob.toURI();
                    ignoreChanges = true;
                    uriField.setText(current.toString());
                    ignoreChanges = false;
                }
            }
        }
    }

    private class FileNode extends FilterNode {
        

        FileNode(Node node) {
            super(node, node.isLeaf() ? Children.LEAF : new FileChildren(node));
        }

        @Override
        public Action getPreferredAction() {
            if (getOriginal().isLeaf()) {
                return new FileAction(this);
            } else {
                return super.getPreferredAction();
            }
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }
    }

    private class FileChildren extends FilterNode.Children {

        public FileChildren(Node node) {
            super(node);
        }

        @Override
        protected Node copyNode(Node node) {
            return new FileNode(node);
        }
    }
    
    private class FileAction extends AbstractAction {
        
        private FileNode node;
        
        private FileAction(FileNode node) {
            super("OK");
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                FileObject fob = node.getLookup().lookup(FileObject.class);
                current = fob.toURI();
                env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                SwingUtilities.getRootPane(ResourceCustomEditor.this).getDefaultButton().doClick();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
    }
}
