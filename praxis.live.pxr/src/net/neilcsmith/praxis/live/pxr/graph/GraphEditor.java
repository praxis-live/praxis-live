/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultEditorKit;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.info.PortInfo;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.graph.Alignment;
import net.neilcsmith.praxis.live.graph.EdgeID;
import net.neilcsmith.praxis.live.graph.EdgeWidget;
import net.neilcsmith.praxis.live.graph.NodeWidget;
import net.neilcsmith.praxis.live.graph.ObjectSceneAdaptor;
import net.neilcsmith.praxis.live.graph.PinID;
import net.neilcsmith.praxis.live.graph.PinWidget;
import net.neilcsmith.praxis.live.graph.PraxisGraphScene;
import net.neilcsmith.praxis.live.pxr.api.ActionSupport;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.EditorUtils;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.StatusDisplayer;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class GraphEditor extends RootEditor {

    private final static Logger LOG = Logger.getLogger(GraphEditor.class.getName());
    final static String ATTR_GRAPH_X = "graph.x";
    final static String ATTR_GRAPH_Y = "graph.y";
    final static String ATTR_GRAPH_MINIMIZED = "graph.minimized";
    private final RootProxy root;
    private final Set<String> knownChildren;
    private final Set<Connection> knownConnections;
    private Action deleteAction;
    private JComponent panel;
    private PraxisGraphScene<String> scene;
    private ExplorerManager manager;
    private ContainerProxy container;
    private Lookup lookup;
    private int lastX;
    private ContainerListener containerListener;
    private ActionSupport actionSupport;
    private Point activePoint = new Point();
    private boolean sync;
    private LocationAction location;
    private Action goUpAction;

    public GraphEditor(RootProxy root, String category) {
        this.root = root;
        knownChildren = new LinkedHashSet<String>();
        knownConnections = new LinkedHashSet<Connection>();

        scene = new PraxisGraphScene<String>(new ConnectProviderImpl(), new MenuProviderImpl());
        manager = new ExplorerManager();
        manager.setRootContext(root.getNodeDelegate());
        if (root instanceof ContainerProxy) {
            container = (ContainerProxy) root;
        }
//        if (container != null) {
//            buildScene();
//        }

        lookup = new ProxyLookup(ExplorerUtils.createLookup(manager, buildActionMap(manager)),
                Lookups.fixed(
                Components.getPalette("core", category)));
        scene.addObjectSceneListener(new SelectionListener(),
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
        goUpAction = new GoUpAction();
        location = new LocationAction();
        setupSceneActions();
    }

    private ActionMap buildActionMap(ExplorerManager em) {
        ActionMap am = new ActionMap();
        deleteAction = new DeleteAction();
        deleteAction.setEnabled(false);
        am.put("delete", deleteAction);

        CopyActionPerformer copyAction = new CopyActionPerformer(this, em);
        am.put(DefaultEditorKit.copyAction, copyAction);
        PasteActionPerformer pasteAction = new PasteActionPerformer(this, em);
        am.put(DefaultEditorKit.pasteAction, pasteAction);

        return am;
    }

    private void setupSceneActions() {
        scene.getActions().addAction(ActionFactory.createAcceptAction(new AcceptProviderImpl()));
    }

    private JPopupMenu getComponentPopup(NodeWidget widget) {
        JPopupMenu menu = new JPopupMenu();
        Object obj = scene.findObject(widget);
        if (obj instanceof String) {
            ComponentProxy cmp = container.getChild(obj.toString());
            if (cmp instanceof ContainerProxy) {
                menu.add(new ContainerOpenAction((ContainerProxy) cmp));
                menu.add(new JSeparator());
            }
            if (cmp != null) {
                boolean addSep = false;
                for (Action a : cmp.getNodeDelegate().getActions(false)) {
                    if (a == null) {
                        menu.add(new JSeparator());
                        addSep = false;
                    } else {
                        menu.add(a);
                        addSep = true;
                    }
                }
                if (addSep) {
                    menu.add(new JSeparator());
                }
            }
        }
        menu.add(deleteAction);
        return menu;
    }

    private JPopupMenu getConnectionPopup() {
        return getScenePopup();
    }

    private JPopupMenu getScenePopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(deleteAction);
        return menu;
    }

    ActionSupport getActionSupport() {
        if (actionSupport == null) {
            actionSupport = new ActionSupport(this);
        }
        return actionSupport;
    }

    ContainerProxy getContainer() {
        return container;
    }

    Point getActivePoint() {
        return new Point(activePoint);
    }

    @Override
    public void componentActivated() {
        if (panel == null) {
            return;
        }
        scene.getView().requestFocusInWindow();
    }

    @Override
    public JComponent getEditorComponent() {
        if (panel == null) {
            JPanel viewPanel = new JPanel(new BorderLayout());
            JComponent view = scene.createView();
            view.addMouseListener(new ActivePointListener());
            JScrollPane scroll = new JScrollPane(
                    view,
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            viewPanel.add(scroll, BorderLayout.CENTER);

            JPanel satellitePanel = new JPanel();
            satellitePanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 0, 25, 25);
            gbc.anchor = GridBagConstraints.SOUTHEAST;
            view = scene.createSatelliteView();
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            holder.add(view);
            satellitePanel.add(holder, gbc);
            satellitePanel.setOpaque(false);

            JLayeredPane layered = new JLayeredPane();
            layered.setLayout(new OverlayLayout(layered));
            layered.add(viewPanel, JLayeredPane.DEFAULT_LAYER);
            layered.add(satellitePanel, JLayeredPane.PALETTE_LAYER);

            panel = new JPanel(new BorderLayout());
            panel.add(layered, BorderLayout.CENTER);

            if (container != null) {
                buildScene();
            }
        }
        return panel;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public Action[] getActions() {
        return new Action[]{goUpAction, location};
    }

    private void clearScene() {
        container.removePropertyChangeListener(containerListener);
        containerListener = null;
        for (String id : container.getChildIDs()) {
            removeChild(id);
        }
        activePoint.setLocation(0, 0);
        knownChildren.clear();
        knownConnections.clear();
        location.address.setText("");
    }

    private void buildScene() {

        containerListener = new ContainerListener();
        container.addPropertyChangeListener(containerListener);

        container.getNodeDelegate().getChildren().getNodes();

        sync(true);

        goUpAction.setEnabled(container.getParent() != null);
        location.address.setText(container.getAddress().toString());
        
    }

    private void buildChild(String id, final ComponentProxy cmp) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Adding " + cmp.getAddress() + " to graph.");
        }
        String name = cmp instanceof ContainerProxy ? id + "/.." : id;
        NodeWidget widget = scene.addNode(id, name);
//        widget.setNodeType(cmp.getType().toString());
        widget.setToolTipText(cmp.getType().toString());
        widget.setPreferredLocation(resolveLocation(id, cmp));       
        if ("true".equals(cmp.getAttribute(ATTR_GRAPH_MINIMIZED))) {
            widget.setMinimized(true);
        }
        widget.getActions().addAction(ActionFactory.createEditAction(new EditProvider() {
            @Override
            public void edit(Widget widget) {
                cmp.getNodeDelegate().getPreferredAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit"));
            }
        }));
        ComponentInfo info = cmp.getInfo();
        for (String portID : info.getPorts()) {
            PortInfo pi = info.getPortInfo(portID);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Building port " + portID);
            }
            buildPin(id, portID, pi);
        }
    }

    private void removeChild(String id) {
        scene.removeNodeWithEdges(id);
        // @TODO temporary fix for moving dynamic components?
        activePoint.x = 0;
        activePoint.y = 0;
    }

    private Point resolveLocation(String id, ComponentProxy cmp) {
        int x = activePoint.x;
        int y = activePoint.y;
        try {
            String xStr = cmp.getAttribute(ATTR_GRAPH_X);
            String yStr = cmp.getAttribute(ATTR_GRAPH_Y);
            if (xStr != null) {
                x = Integer.parseInt(xStr) + x;
            }
            if (yStr != null) {
                y = Integer.parseInt(yStr) + y;
            }
            LOG.log(Level.FINEST, "Resolved location for {0} : {1} x {2}", new Object[]{id, x, y});
        } catch (Exception ex) {
            LOG.log(Level.FINEST, "Cannot resolve location for " + id, ex);

        }
        // @TODO what to do about importing components without positions?
        // if point not set, check for widget at point?
        return new Point(x, y);
    }

    private void buildPin(String cmpID, String pinID, PortInfo info) {
        boolean primary = info.getType().getSimpleName().startsWith("Audio")
                || info.getType().getSimpleName().startsWith("Video");
        PinWidget pin = scene.addPin(cmpID, pinID, "", getPinAlignment(info));
        if (primary) {
            pin.setFont(scene.getDefaultFont().deriveFont(Font.BOLD, 11.0f));
        } else {
            pin.setFont(scene.getDefaultFont().deriveFont(11.0f));
        }
        pin.setToolTipText(pinID + " : " + info.getType().getSimpleName());
    }

    private Alignment getPinAlignment(PortInfo info) {
        switch (info.getDirection()) {
            case IN:
                return Alignment.Left;
            case OUT:
                return Alignment.Right;
            default:
                return Alignment.Center;
        }
    }

    private void buildConnection(Connection connection) {
        EdgeWidget widget = scene.connect(connection.getChild1(), connection.getPort1(),
                connection.getChild2(), connection.getPort2());
        widget.setToolTipText(connection.getChild1() + "!" + connection.getPort1() + " -> "
                + connection.getChild2() + "!" + connection.getPort2());
    }

    private void removeConnection(Connection connection) {
        scene.disconnect(connection.getChild1(), connection.getPort1(),
                connection.getChild2(), connection.getPort2());
    }

    private void syncChildren() {
        if (container == null) {
            return;
        }
        List<String> ch = Arrays.asList(container.getChildIDs());
        Set<String> tmp = new LinkedHashSet<String>(knownChildren);
        tmp.removeAll(ch);
        // tmp now contains children that have been removed from model
        for (String id : tmp) {
            removeChild(id);
            knownChildren.remove(id);
        }
        tmp.clear();
        tmp.addAll(ch);
        tmp.removeAll(knownChildren);
        // tmp now contains children that have been added to model
        for (String id : tmp) {
            ComponentProxy cmp = container.getChild(id);
            if (cmp != null) {
                buildChild(id, cmp);
                knownChildren.add(id);
            }
        }
        scene.validate();
    }

    private void syncConnections() {
        if (container == null) {
            return;
        }
        List<Connection> cons = Arrays.asList(container.getConnections());
        Set<Connection> tmp = new LinkedHashSet<Connection>(knownConnections);
        tmp.removeAll(cons);
        // tmp now contains connections that have been removed from model
        for (Connection con : tmp) {
            removeConnection(con);
            knownConnections.remove(con);
        }
        tmp.clear();
        tmp.addAll(cons);
        tmp.removeAll(knownConnections);
        // tmp now contains connections that have been added to model
        for (Connection con : tmp) {
            buildConnection(con);
            knownConnections.add(con);
        }
        scene.validate();
    }

    void sync(boolean sync) {
        if (sync) {
            this.sync = true;
            syncChildren();
            syncConnections();
        } else {
            this.sync = false;
        }
    }

    private class ContainerListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (sync) {
                if (ContainerProxy.PROP_CHILDREN.equals(evt.getPropertyName())) {
                    syncChildren();
                } else if (ContainerProxy.PROP_CONNECTIONS.equals(evt.getPropertyName())) {
                    syncConnections();
                }
            }

        }
    }

    private class ConnectProviderImpl implements ConnectProvider {

        @Override
        public boolean isSourceWidget(Widget sourceWidget) {
            return sourceWidget instanceof PinWidget;
        }

        @Override
        public ConnectorState isTargetWidget(Widget sourceWidget, Widget targetWidget) {
            if (sourceWidget instanceof PinWidget && targetWidget instanceof PinWidget) {
                return ConnectorState.ACCEPT;
            } else {
                return ConnectorState.REJECT;
            }
        }

        @Override
        public boolean hasCustomTargetWidgetResolver(Scene scene) {
            return false;
        }

        @Override
        public Widget resolveTargetWidget(Scene scene, Point sceneLocation) {
            return null;
        }

        @Override
        public void createConnection(Widget sourceWidget, Widget targetWidget) {
            PinID<String> p1 = (PinID<String>) scene.findObject(sourceWidget);
            PinID<String> p2 = (PinID<String>) scene.findObject(targetWidget);
            try {
                container.connect(new Connection(p1.getParent(), p1.getName(), p2.getParent(), p2.getName()), new Callback() {
                    @Override
                    public void onReturn(CallArguments args) {
                    }

                    @Override
                    public void onError(CallArguments args) {
                    }
                });
            } catch (ProxyException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private class MenuProviderImpl implements PopupMenuProvider {

        @Override
        public JPopupMenu getPopupMenu(Widget widget, Point localLocation) {
            if (widget instanceof NodeWidget) {
                return getComponentPopup((NodeWidget) widget);
            } else if (widget instanceof EdgeWidget) {
                return getConnectionPopup();
            } else if (widget == scene) {
                return getScenePopup();
            } else {
                return null;
            }

        }
    }

    private class ActivePointListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            updateActivePoint(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            updateActivePoint(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            updateActivePoint(e);
        }

        private void updateActivePoint(MouseEvent e) {
            activePoint.setLocation(scene.convertViewToScene(e.getPoint()));
            LOG.log(Level.FINEST, "Updated active point : {0}", activePoint);
        }
    }

    private class SelectionListener extends ObjectSceneAdaptor {

        @Override
        public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
            for (Object obj : previousSelection) {
                if (newSelection.contains(obj)) {
                    continue;
                }
                if (obj instanceof String) {
                    ComponentProxy cmp = container.getChild((String) obj);
                    if (cmp != null) {
                        syncAttributes(cmp, (String) obj);
                    }
                }
            }

            try {
                //inSelection = true; // not needed now not listening to EM?
                if (newSelection.isEmpty()) {
                    LOG.log(Level.FINEST, "newSelection is empty");
                    if (container != null) {
                        manager.setSelectedNodes(new Node[]{container.getNodeDelegate()});
                    } else {
                        manager.setSelectedNodes(new Node[]{manager.getRootContext()});
                    }               
                    deleteAction.setEnabled(false);
                } else {
                    ArrayList<Node> sel = new ArrayList<Node>();
                    for (Object obj : newSelection) {
//                        if (obj instanceof Node) {
//                            sel.add((Node) obj);
//                        }
                        if (obj instanceof String) {
                            ComponentProxy cmp = container.getChild((String) obj);
                            if (cmp != null) {
                                sel.add(cmp.getNodeDelegate());
                                syncAttributes(cmp, (String) obj);
                            }
                        }

                    }
                    LOG.log(Level.FINEST, "newSelection size is " + newSelection.size() + " and node selection size is " + sel.size());
                    if (sel.isEmpty()) {
                        manager.setSelectedNodes(new Node[]{manager.getRootContext()});
                    } else {
                        manager.setSelectedNodes(sel.toArray(new Node[sel.size()]));
                    }
                    deleteAction.setEnabled(true);
                }
            } catch (PropertyVetoException ex) {
                LOG.log(Level.FINEST, "Received PropertyVetoException trying to set selected nodes", ex);
            } finally {
                //inSelection = false;
            }

        }

        private void syncAttributes(ComponentProxy cmp, String id) {
            Widget widget = scene.findWidget(id);
            if (widget instanceof NodeWidget) {
                NodeWidget nodeWidget = (NodeWidget) widget;
                String x = Integer.toString((int) nodeWidget.getLocation().getX());
                String y = Integer.toString((int) nodeWidget.getLocation().getY());
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Setting position attributes of {0} to x:{1} y:{2}",
                            new Object[]{cmp.getAddress(), x, y});
                }
                cmp.setAttribute(ATTR_GRAPH_X, x);
                cmp.setAttribute(ATTR_GRAPH_Y, y);
                cmp.setAttribute(ATTR_GRAPH_MINIMIZED,
                        nodeWidget.isMinimized() ? "true" : null);
            }
        }
    }

    private class DeleteAction extends AbstractAction {

        private DeleteAction() {
            super("Delete");
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            // GRRR! Built in delete action is asynchronous - replace?
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        actionPerformed(e);
                    }
                });
                return;
            }
            assert EventQueue.isDispatchThread();
            Set<?> sel = scene.getSelectedObjects();
            if (sel.isEmpty()) {
                return;
            }
            if (!checkDeletion(sel)) {
                return;
            }
            for (Object obj : sel) {
                if (obj instanceof String) {
                    try {
                        container.removeChild((String) obj, new Callback() {
                            @Override
                            public void onReturn(CallArguments args) {
                            }

                            @Override
                            public void onError(CallArguments args) {
                            }
                        });
                    } catch (ProxyException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else if (obj instanceof EdgeID) {
                    EdgeID edge = (EdgeID) obj;
                    PinID p1 = edge.getPin1();
                    PinID p2 = edge.getPin2();
                    Connection con = new Connection(p1.getParent().toString(), p1.getName(),
                            p2.getParent().toString(), p2.getName());
                    try {
                        container.disconnect(con, new Callback() {
                            @Override
                            public void onReturn(CallArguments args) {
                            }

                            @Override
                            public void onError(CallArguments args) {
                            }
                        });
                    } catch (ProxyException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
        
        private boolean checkDeletion(Set<?> selected) {
            List<String> components = new ArrayList<String>();
            for (Object o : selected) {
                if (o instanceof String) {
                    components.add((String) o);
                }
            }
            if (components.isEmpty()) {
                return true;
            }
            int count = components.size();
            String msg = count > 1 ?
                    "Delete " + count + " components?" :
                    "Delete " + components.get(0) + "?";
            String title = "Confirm deletion";
            NotifyDescriptor desc = new NotifyDescriptor.Confirmation(msg, title, NotifyDescriptor.YES_NO_OPTION);

            return NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(desc));
        }
        
        
    }

    private class GoUpAction extends AbstractAction {

        private GoUpAction() {
            super("Go Up",
                    ImageUtilities.loadImageIcon(
                    "net/neilcsmith/praxis/live/pxr/graph/resources/go-up.png",
                    true));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ContainerProxy parent = container.getParent();
            if (parent != null) {
                clearScene();
                container = parent;
                buildScene();
            }
        }
    }

    private class LocationAction extends AbstractAction implements Presenter.Toolbar {
        
        private final JLabel address = new JLabel();
  
        @Override
        public void actionPerformed(ActionEvent e) {
            
        }

        @Override
        public Component getToolbarPresenter() {
            return address;
        }
    }

    private class ContainerOpenAction extends AbstractAction {

        private final ContainerProxy container;

        private ContainerOpenAction(ContainerProxy container) {
            super("Open");
            this.container = container;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            clearScene();
            GraphEditor.this.container = container;
            buildScene();
        }
    }

    private class AcceptProviderImpl implements AcceptProvider {

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            if (extractType(transferable) != null || extractFile(transferable) != null) {
                return ConnectorState.ACCEPT;
            } else {
                return ConnectorState.REJECT;
            }
        }

        @Override
        public void accept(Widget widget, final Point point, Transferable transferable) {
            activePoint.setLocation(point);
            ComponentType type = extractType(transferable);
            if (type != null) {
                acceptComponentType(type);
                return;
            }
            FileObject file = extractFile(transferable);
            if (file != null) {
                acceptImport(file);
            }
        }

        private void acceptComponentType(final ComponentType type) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                            "ID:", "Enter an ID for " + type);
                    dlg.setInputText(getFreeID(type));
                    Object retval = DialogDisplayer.getDefault().notify(dlg);
                    if (retval == NotifyDescriptor.OK_OPTION) {
                        final String id = dlg.getInputText();
                        try {
                            container.addChild(id, type, new Callback() {
                                @Override
                                public void onReturn(CallArguments args) {
                                    // nothing wait for sync
                                }

                                @Override
                                public void onError(CallArguments args) {
//                                        pointMap.remove(id);
                                    DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating component", NotifyDescriptor.ERROR_MESSAGE));
                                }
                            });
                        } catch (ProxyException ex) {
                            Exceptions.printStackTrace(ex);
                        }
//                            pointMap.put(id, point);

                    }
                }
            };
            SwingUtilities.invokeLater(runnable);
        }

        private void acceptImport(FileObject file) {
            if (getActionSupport().importSubgraph(container, file, new Callback() {
                @Override
                public void onReturn(CallArguments args) {
                    sync(true);
                }

                @Override
                public void onError(CallArguments args) {
                    sync(true);
                }
            })) {
                sync(false);
            }
        }

        private ComponentType extractType(Transferable transferable) {
            Node n = NodeTransfer.node(transferable, NodeTransfer.DND_COPY_OR_MOVE);
            if (n != null) {
                ComponentType t = n.getLookup().lookup(ComponentType.class);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }

        private FileObject extractFile(Transferable transferable) {
            Node n = NodeTransfer.node(transferable, NodeTransfer.DND_COPY_OR_MOVE);
            if (n != null) {
                FileObject dob = n.getLookup().lookup(FileObject.class);
                if (dob != null) {
                    return dob;
                }
            }
            return null;
        }

        private String getFreeID(ComponentType type) {
//            String base = type.toString();
//            base = base.substring(base.lastIndexOf(":") + 1);
//            for (int i = 1; i < 100; i++) {
//                if (container.getChild(base + i) == null) {
//                    return base + i;
//                }
//            }
//            return "";

            Set<String> existing = new HashSet<String>(Arrays.asList(container.getChildIDs()));
            return EditorUtils.findFreeID(existing, EditorUtils.extractBaseID(type), true);

        }
    }
}
