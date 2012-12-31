/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.interfaces.ComponentInterface;
import net.neilcsmith.praxis.core.interfaces.ContainerInterface;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeNotFoundException;
import org.openide.nodes.NodeOp;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class ControlAddressCustomEditor extends javax.swing.JPanel
        implements ExplorerManager.Provider {

    private final static Set<String> SYS_CTRLS = new HashSet<String>();
    static {
        SYS_CTRLS.add(ComponentInterface.INFO);
        SYS_CTRLS.add(ContainerInterface.ADD_CHILD);
        SYS_CTRLS.add(ContainerInterface.CHILDREN);
        SYS_CTRLS.add(ContainerInterface.CONNECT);
        SYS_CTRLS.add(ContainerInterface.CONNECTIONS);
        SYS_CTRLS.add(ContainerInterface.DISCONNECT);
        SYS_CTRLS.add(ContainerInterface.REMOVE_CHILD);
    }
    
    private static final Logger LOG = Logger.getLogger(ControlAddressCustomEditor.class.getName());
    private ControlAddress current;
    private ComponentProxy exploredComponent;
    private final ExplorerManager em;
    private final PropertyEnv env;
    private final ControlAddressEditor editor;
    private final Listener listener;
    private final DefaultListModel listModel;

    ControlAddressCustomEditor(ControlAddressEditor editor, ControlAddress current, PropertyEnv env) {
        initComponents();
        if (current != null) {
            addressField.setText(current.toString());
        }
        this.current = current;
        this.env = env;
        this.editor = editor;
//        env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        em = new ExplorerManager();
        listener = new Listener();

        listModel = new DefaultListModel();
        controlList.setModel(listModel);

        em.setRootContext(new AbstractNode(new RootsChildren(), Lookup.EMPTY));
        if (current != null) {
            Node node = findNodeFromAddress(current);
            if (node != null) {
                try {
                    em.setSelectedNodes(new Node[]{node});
                    refreshComponent(node.getLookup().lookup(ComponentProxy.class));
                    controlList.setSelectedValue(current.getID(), true);
                } catch (PropertyVetoException ex) {
                    Exceptions.printStackTrace(ex);
                }


            }
        }

        env.addVetoableChangeListener(listener);
        addressField.getDocument().addDocumentListener(listener);
        em.addPropertyChangeListener(listener);
        controlList.addListSelectionListener(listener);
        controlList.addMouseListener(listener);

        initNodeBrowser();

    }

    private void initNodeBrowser() {
        BeanTreeView view = (BeanTreeView) nodeBrowser;
        view.setRootVisible(false);
        view.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        view.setPopupAllowed(false);
//        view.setQuickSearchAllowed(false);
    }

    private Node findNodeFromAddress(ControlAddress address) {
        LOG.log(Level.FINEST, "Searching for node that matches {0}", address);
        ComponentAddress cmp = address.getComponentAddress();
        String[] parts = new String[cmp.getDepth()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = cmp.getComponentID(i);
        }
        try {
            return NodeOp.findPath(em.getRootContext(), parts);
        } catch (NodeNotFoundException ex) {
            LOG.log(Level.FINEST, "Node not found", ex);
            return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addressField = new javax.swing.JTextField();
        nodeBrowser = new org.openide.explorer.view.BeanTreeView();
        javax.swing.JScrollPane listScroll = new javax.swing.JScrollPane();
        controlList = new javax.swing.JList();
        toolBar = new javax.swing.JToolBar();
        clearButton = new javax.swing.JButton();
        systemToggleButton = new javax.swing.JToggleButton();

        addressField.setText(org.openide.util.NbBundle.getMessage(ControlAddressCustomEditor.class, "ControlAddressCustomEditor.addressField.text")); // NOI18N

        nodeBrowser.setPreferredSize(new java.awt.Dimension(150, 200));

        controlList.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        listScroll.setViewportView(controlList);

        toolBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        clearButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/pxr/resources/clear.png"))); // NOI18N
        clearButton.setText(org.openide.util.NbBundle.getMessage(ControlAddressCustomEditor.class, "ControlAddressCustomEditor.clearButton.text")); // NOI18N
        clearButton.setFocusable(false);
        clearButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        clearButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });
        toolBar.add(clearButton);

        systemToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/pxr/resources/system.png"))); // NOI18N
        systemToggleButton.setText(org.openide.util.NbBundle.getMessage(ControlAddressCustomEditor.class, "ControlAddressCustomEditor.systemToggleButton.text")); // NOI18N
        systemToggleButton.setFocusable(false);
        systemToggleButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        systemToggleButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        systemToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                systemToggleButtonActionPerformed(evt);
            }
        });
        toolBar.add(systemToggleButton);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nodeBrowser, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(listScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 137, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(addressField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(addressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(nodeBrowser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(listScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void clearButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearButtonActionPerformed
        addressField.setText("");
    }//GEN-LAST:event_clearButtonActionPerformed

    private void systemToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemToggleButtonActionPerformed
        refreshList();
    }//GEN-LAST:event_systemToggleButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField addressField;
    private javax.swing.JButton clearButton;
    private javax.swing.JList controlList;
    private javax.swing.JScrollPane nodeBrowser;
    private javax.swing.JToggleButton systemToggleButton;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    private void refreshComponent(ComponentProxy cmp) {
        if (this.exploredComponent == cmp) {
            return;
        }
        this.exploredComponent = cmp;
        refreshList();
    }

    private void refreshList() {
        ComponentProxy cmp = exploredComponent;
        listModel.clear();
        if (cmp != null) {
            boolean sys = systemToggleButton.isSelected();
            for (String id : cmp.getInfo().getControls()) {
                if (sys || !SYS_CTRLS.contains(id)) {
                    listModel.addElement(id);
                }          
            }
        }
    }

    private class Listener extends MouseAdapter implements DocumentListener, VetoableChangeListener,
            PropertyChangeListener, ListSelectionListener {

        private boolean ignore;

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
            if (ignore) {
                return;
            }
            String txt = addressField.getText();
            if (txt.isEmpty()) {
                current = null;
                try {
                    em.setSelectedNodes(new Node[]{});
                } catch (PropertyVetoException ex) {}
                env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
            } else {
                try {
                    current = ControlAddress.valueOf(addressField.getText());
                    env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                } catch (Exception ex) {
                    env.setState(PropertyEnv.STATE_INVALID);
                }
            }
        }

        // called when editing completed
        @Override
        public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
            if (evt.getNewValue().equals(PropertyEnv.STATE_VALID)) {
                if (current == null) {
                    editor.setValue(PString.EMPTY);
                } else {
                    editor.setValue(current);
                }
            }
        }

        // called from node browser
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                LOG.finest("Node selection changed");
                Node[] nodes = em.getSelectedNodes();
                ComponentProxy cmp = null;
                if (nodes.length == 1) {
                    cmp = nodes[0].getLookup().lookup(ComponentProxy.class);
                }
                refreshComponent(cmp);
            }
        }

        // called from control list
        @Override
        public void valueChanged(ListSelectionEvent e) {
            Object sel = controlList.getSelectedValue();
            if (sel == null) {
                return;
            }
            String id = sel.toString();
            if (exploredComponent != null) {
                current = ControlAddress.create(exploredComponent.getAddress(), id);
                ignore = true;
                addressField.setText(current.toString());
                env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
                ignore = false;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
//                LOG.finest("Double click on list");
                int index = controlList.locationToIndex(e.getPoint());
                if (index < 0) {
//                    LOG.finest("No item found at location");
                    return;
                }
                Object id = controlList.getModel().getElementAt(index);
//                LOG.finest("ID found in list : " + id);
                JRootPane rpane = SwingUtilities.getRootPane(controlList);
//                LOG.finest(rpane == null ? "No rootpane found" : "Rootpane found");
                JButton button = rpane.getDefaultButton();
//                LOG.finest(button == null ? "No default button found" : "Default button found");
                if (id instanceof String && button != null) {
//                    LOG.finest("Creating current address");
                    current = ControlAddress.create(exploredComponent.getAddress(), id.toString());
//                    LOG.finest("Clicking default button");
                    button.doClick();
                    e.consume();
                }
            }
        }
    }

    private static class RootsChildren extends Children.Keys<RootProxy> {

        private RootsChildren() {
            setKeys(RootRegistry.getDefault().getRoots());
        }

        @Override
        protected Node[] createNodes(RootProxy key) {
            return new Node[]{new ComponentNode(key.getNodeDelegate())};
        }
    }

    private static class ComponentNode extends FilterNode {

        private ComponentNode(Node node) {
            super(node,
                    node.getChildren() == Children.LEAF
                    ? Children.LEAF : new ComponentChildren(node));
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

        @Override
        public Action getPreferredAction() {
            return null;
        }
    }

    private static class ComponentChildren extends FilterNode.Children {

        private ComponentChildren(Node node) {
            super(node);
        }

        @Override
        protected Node copyNode(Node node) {
            return new ComponentNode(node);
        }
    }
}
