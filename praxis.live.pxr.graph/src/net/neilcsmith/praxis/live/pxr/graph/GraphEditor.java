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
package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.border.LineBorder;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.ControlPort;
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
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
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
    private final RootProxy root;
    private final Set<String> knownChildren;
    private final Set<Connection> knownConnections;
    private final Map<String, Point> pointMap;
//    private final Map<EdgeID<String>, Connection> edgeToConnection;
    private Action deleteAction;
    private JComponent panel;
    private PraxisGraphScene<String> scene;
    private ExplorerManager manager;
    private ContainerProxy container;
    private Lookup lookup;
    private int lastX;
    private ContainerListener containerListener;

    GraphEditor(RootProxy root, String category) {
        this.root = root;
        knownChildren = new LinkedHashSet<String>();
        knownConnections = new LinkedHashSet<Connection>();
        pointMap = new HashMap<String, Point>(1);
//        edgeToConnection = new HashMap<EdgeID<String>, Connection>();

        scene = new PraxisGraphScene<String>(new ConnectProviderImpl(), new MenuProviderImpl());
        manager = new ExplorerManager();
        manager.setRootContext(root.getNodeDelegate());
        if (root instanceof ContainerProxy) {
            container = (ContainerProxy) root;
        }
        if (container != null) {
            buildScene();
        }


        lookup = new ProxyLookup(ExplorerUtils.createLookup(manager, new ActionMap()),
                Lookups.fixed(
                /*GraphNavigator.HINT,
                new NavigatorBridge(),*/
                Components.getPalette("core", category)));
        scene.addObjectSceneListener(new SelectionListener(),
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
        setupSceneActions();
    }

    private void setupSceneActions() {
        deleteAction = new DeleteAction();
        deleteAction.setEnabled(false);
        InputMap im = new InputMap();
        im.put(KeyStroke.getKeyStroke("DELETE"), "DELETE");
        ActionMap am = new ActionMap();
        am.put("DELETE", deleteAction);
        scene.getActions().addAction(ActionFactory.createActionMapAction(im, am));
        scene.getActions().addAction(ActionFactory.createAcceptAction(new TypeAcceptProvider()));
    }

    private JPopupMenu getComponentPopup() {
        return getScenePopup();
    }

    private JPopupMenu getConnectionPopup() {
        return getScenePopup();
    }

    private JPopupMenu getScenePopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(deleteAction);
        return menu;
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
//        if (panel == null) {
//            panel = new JPanel(new BorderLayout());
//            JScrollPane scroll = new JScrollPane(
//                    scene.createView(),
//                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
//                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
//            panel.add(scroll, BorderLayout.CENTER);
//        }
//        return panel;
        if (panel == null) {
            JPanel viewPanel = new JPanel(new BorderLayout());
            JScrollPane scroll = new JScrollPane(
                    scene.createView(),
                    JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                    JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            viewPanel.add(scroll, BorderLayout.CENTER);
            
            JPanel satellitePanel = new JPanel();
            satellitePanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0,0,25,25);
            gbc.anchor = GridBagConstraints.SOUTHEAST;
            JComponent view = scene.createSatelliteView();
            JPanel holder = new JPanel(new BorderLayout());
            holder.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            holder.add(view);
            satellitePanel.add(holder, gbc);
            satellitePanel.setOpaque(false);
            
            panel = new JLayeredPane();
            panel.setLayout(new OverlayLayout(panel));
            panel.add(viewPanel, JLayeredPane.DEFAULT_LAYER);
            panel.add(satellitePanel, JLayeredPane.PALETTE_LAYER);
                  
        }
        return panel;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private void clearScene() {
        container.removePropertyChangeListener(containerListener);
        containerListener = null;
        for (String id : scene.getNodes()) {
            scene.removeNodeWithEdges(id);
        }
        lastX = 0;
        knownChildren.clear();
        knownConnections.clear();
    }

    private void buildScene() {

        containerListener = new ContainerListener();
        container.addPropertyChangeListener(containerListener);

        container.getNodeDelegate().getChildren().getNodes();

        syncChildren();
        syncConnections();

    }

    private void buildChild(String id, ComponentProxy cmp) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Adding " + cmp.getAddress() + " to graph.");
        }
        NodeWidget widget = scene.addNode(id, id);
//        widget.setNodeType(cmp.getType().toString());
        widget.setToolTipText(cmp.getType().toString());
        widget.setPreferredLocation(resolveLocation(id, cmp));
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
    }

    private Point resolveLocation(String id, ComponentProxy cmp) {
        Point res = pointMap.remove(id);
        if (res != null) {
            cmp.setAttribute(ATTR_GRAPH_X, String.valueOf(res.x));
            cmp.setAttribute(ATTR_GRAPH_Y, String.valueOf(res.y));
            return res;
        }
        int x, y;
        x = lastX == 0 ? 20 : lastX + 200;
        y = 150;
        try {
            String xStr = cmp.getAttribute(ATTR_GRAPH_X);
            String yStr = cmp.getAttribute(ATTR_GRAPH_Y);
            if (xStr != null) {
                x = Integer.parseInt(xStr);
            }
            if (yStr != null) {
                y = Integer.parseInt(yStr);
            }
        } catch (Exception ex) {
        }
        lastX = x;
        return new Point(x, y);
    }

    private void buildPin(String cmpID, String pinID, PortInfo info) {
        boolean control = ControlPort.class.isAssignableFrom(info.getType());
        PinWidget pin = scene.addPin(cmpID, pinID, "", getPinAlignment(info));
        if (!control) {
            pin.setFont(pin.getFont().deriveFont(Font.BOLD));
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

    private class ContainerListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ContainerProxy.PROP_CHILDREN.equals(evt.getPropertyName())) {
                syncChildren();
            } else if (ContainerProxy.PROP_CONNECTIONS.equals(evt.getPropertyName())) {
                syncConnections();
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
                return getComponentPopup();
            } else if (widget instanceof EdgeWidget) {
                return getConnectionPopup();
            } else if (widget == scene) {
                return getScenePopup();
            } else {
                return null;
            }

        }
    }

    private class SelectionListener extends ObjectSceneAdaptor {

        @Override
        public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
            for (Object obj : previousSelection) {
                if (newSelection.contains(obj)) {
                    continue;
                }
                ComponentProxy cmp = container.getChild(obj.toString());
                if (cmp != null) {
                    Widget widget = scene.findWidget(obj);
                    if (widget != null) {
                        String x = Integer.toString((int) widget.getLocation().getX());
                        String y = Integer.toString((int) widget.getLocation().getY());
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Setting position attributes of " + cmp.getAddress() + " to x:" + x + " y:" + y);
                        }
                        cmp.setAttribute(ATTR_GRAPH_X, x);
                        cmp.setAttribute(ATTR_GRAPH_Y, y);
                    }
                }

            }

            try {
                //inSelection = true; // not needed now not listening to EM?
                if (newSelection.isEmpty()) {
                    LOG.log(Level.FINEST, "newSelection is empty");
                    manager.setSelectedNodes(new Node[]{manager.getRootContext()});
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
    }

    private class DeleteAction extends AbstractAction {

        private DeleteAction() {
            super("Delete");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Set<?> sel = scene.getSelectedObjects();
            if (sel.isEmpty()) {
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
    }

    private class TypeAcceptProvider implements AcceptProvider {

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            return extractType(transferable) == null ? ConnectorState.REJECT : ConnectorState.ACCEPT;
        }

        @Override
        public void accept(Widget widget, Point point, Transferable transferable) {
            ComponentType type = extractType(transferable);
            if (type != null) {
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
                                pointMap.remove(id);
                                DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating component", NotifyDescriptor.ERROR_MESSAGE));
                            }
                        });
                    } catch (ProxyException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                    pointMap.put(id, point);
                }
            }

        }

        private ComponentType extractType(Transferable transferable) {
            Node n = NodeTransfer.node(transferable, NodeTransfer.DND_COPY);
            if (n != null) {
                ComponentType t = n.getLookup().lookup(ComponentType.class);
                if (t != null) {
                    return t;
                }
            }
            return null;
        }

        private String getFreeID(ComponentType type) {
            String base = type.toString();
            base = base.substring(base.lastIndexOf(":") + 1);
            for (int i=1; i<100; i++) {
                if (container.getChild(base + i) == null) {
                    return base + i;
                }
            }
            return "";
        }
    }

    private class NavigatorBridge implements GraphNavigator.Bridge {

        @Override
        public JComponent getNavigatorComponent() {
            return scene.createSatelliteView();
        }
    }
}
