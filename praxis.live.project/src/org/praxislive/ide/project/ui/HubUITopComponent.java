/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.project.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.ListSelectionModel;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
//import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.ListView;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.praxislive.ide.model.HubProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.project.api.PraxisProject;

/**
 * Top component which displays Hub and roots information
 */
@ConvertAsProperties(dtd = "-//org.praxislive.ide.hubui//HubUI//EN",
        autostore = false)
public final class HubUITopComponent extends TopComponent implements ExplorerManager.Provider {

    private final static String SYSTEM_PREFIX = "_";

    private static final String PREFERRED_ID = "HubUITopComponent";
    /**
     * path to the icon used by the component and its open action
     */
    private static final String ICON_PATH = "org/praxislive/ide/project/resources/hub-action.png";

    private static HubUITopComponent instance;

    private final ExplorerManager manager;
    private final TCListener registryListener;

    private HubNode hubNode;

    public HubUITopComponent() {
        manager = new ExplorerManager();
        initComponents();
        ((ListView) rootList).setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setName(NbBundle.getMessage(HubUITopComponent.class, "CTL_HubUITopComponent"));
        setToolTipText(NbBundle.getMessage(HubUITopComponent.class, "HINT_HubUITopComponent"));
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        registryListener = new TCListener();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        rootList = new ListView();
        jToolBar1 = new javax.swing.JToolBar();
        systemRootToggle = new javax.swing.JToggleButton();

        jToolBar1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        systemRootToggle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/praxislive/ide/project/resources/system24.png"))); // NOI18N
        systemRootToggle.setToolTipText(org.openide.util.NbBundle.getMessage(HubUITopComponent.class, "LBL_ShowSystemRoots")); // NOI18N
        systemRootToggle.setActionCommand("showSystemRoots");
        systemRootToggle.setFocusable(false);
        systemRootToggle.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        systemRootToggle.setLabel("");
        systemRootToggle.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        systemRootToggle.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                systemRootToggleActionPerformed(evt);
            }
        });
        jToolBar1.add(systemRootToggle);
        systemRootToggle.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(HubUITopComponent.class, "LBL_ShowSystemRoots")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addComponent(rootList)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(rootList, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void systemRootToggleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_systemRootToggleActionPerformed
    }//GEN-LAST:event_systemRootToggleActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JScrollPane rootList;
    private javax.swing.JToggleButton systemRootToggle;
    // End of variables declaration//GEN-END:variables

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files
     * only, i.e. deserialization routines; otherwise you could get a
     * non-deserialized instance. To obtain the singleton instance, use
     * {@link #findInstance}.
     */
    public static synchronized HubUITopComponent getDefault() {
        if (instance == null) {
            instance = new HubUITopComponent();
        }
        return instance;
    }

    /**
     * Obtain the HubUITopComponent instance. Never call {@link #getDefault}
     * directly!
     */
    public static synchronized HubUITopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(HubUITopComponent.class.getName()).warning(
                    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof HubUITopComponent) {
            return (HubUITopComponent) win;
        }
        Logger.getLogger(HubUITopComponent.class.getName()).warning(
                "There seem to be multiple components with the '" + PREFERRED_ID
                + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        TopComponent.getRegistry().addPropertyChangeListener(registryListener);
        refresh();
    }

    @Override
    public void componentClosed() {
        TopComponent.getRegistry().removePropertyChangeListener(registryListener);
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
        if (instance == null) {
            instance = this;
        }
        instance.readPropertiesImpl(p);
        return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    private void refresh() {
        Project project = null;
        boolean foundMany = false;

        Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
        for (Node node : nodes) {
            Lookup lkp = node.getLookup();
            var found = findProject(lkp);

            if (found != null) {
                if (project != null && project != found) {
                    // more than one
                    project = null;
                    foundMany = true;
                    break;
                } else {
                    project = found;
                }
            }
        }

        if (project == null && !foundMany) {
            var tc = TopComponent.getRegistry().getActivated();
            if (tc != null) {
                project = findProject(tc.getLookup());
            }
        }

        HubProxy hub;
        if (project != null) {
            hub = project.getLookup().lookup(HubProxy.class);
        } else {
            hub = null;
        }
        
        if (hub != null) {
            if (hubNode != null) {
                if (hubNode.hub == hub) {
                    return;
                } else {
                    hubNode.dispose();
                }
            }
            hubNode = new HubNode(hub, hub.getNodeDelegate());
            manager.setRootContext(hubNode);
        } else if (foundMany) {
            if (hubNode == null) {
                return;
            } else {
                hubNode.dispose();
                hubNode = null;
                manager.setRootContext(new AbstractNode(Children.LEAF));
            }
        }
    }

    private Project findProject(Lookup lkp) {
        var project = lkp.lookup(Project.class);
        if (project == null) {
            var dob = lkp.lookup(DataObject.class);
            if (dob != null) {
                project = FileOwnerQuery.getOwner(dob.getPrimaryFile());
            }
        }
        return project;
    }

    private class TCListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(evt.getPropertyName())) {
                refresh();
            }
        }

    }

    private static class HubNode extends FilterNode {

        final HubProxy hub;
        final Roots roots;

        private HubNode(HubProxy hub, Node original) {
            this(hub, original, new Roots(original));
        }

        private HubNode(HubProxy hub, Node original, Roots roots) {
            super(original, roots);
            this.hub = hub;
            this.roots = roots;
        }

        private void dispose() {
            roots.dispose();
        }

    }

    private static class RootNode extends FilterNode {

        private final RootProxy root;

        private RootNode(Node original, RootProxy root) {
            super(original, Children.LEAF);
            this.root = root;
        }

        @Override
        public void destroy() throws IOException {
            super.destroy();
        }

    }

    private static class Roots extends FilterNode.Children {

        private boolean showSystem;

        Roots(Node original) {
            super(original);
        }

        @Override
        protected Node[] createNodes(Node key) {
            RootProxy root = key.getLookup().lookup(RootProxy.class);
            if (root == null || root.getAddress().rootID().startsWith(SYSTEM_PREFIX)) {
                return new Node[0];
            } else {
                return new Node[]{new RootNode(key, root)};
            }

        }

        private void dispose() {
            setKeys(new Node[0]);
        }

    }

}