/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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
import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import net.neilcsmith.praxis.core.interfaces.ComponentInterface;
import net.neilcsmith.praxis.core.interfaces.ContainerInterface;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.core.api.Syncable;
import net.neilcsmith.praxis.live.graph.Alignment;
import net.neilcsmith.praxis.live.graph.EdgeID;
import net.neilcsmith.praxis.live.graph.EdgeWidget;
import net.neilcsmith.praxis.live.graph.NodeWidget;
import net.neilcsmith.praxis.live.graph.ObjectSceneAdaptor;
import net.neilcsmith.praxis.live.graph.PinID;
import net.neilcsmith.praxis.live.graph.PinWidget;
import net.neilcsmith.praxis.live.graph.PraxisGraphScene;
import net.neilcsmith.praxis.live.model.ComponentProxy;
import net.neilcsmith.praxis.live.model.Connection;
import net.neilcsmith.praxis.live.model.ContainerProxy;
import net.neilcsmith.praxis.live.model.ProxyException;
import net.neilcsmith.praxis.live.model.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.ActionSupport;
import net.neilcsmith.praxis.live.pxr.api.EditorUtils;
import net.neilcsmith.praxis.live.pxr.api.PaletteUtils;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
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
import org.netbeans.spi.palette.PaletteController;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.MenuView;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAcceptor;
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
    final static String ATTR_GRAPH_COLORS = "graph.colors";
    final static String ATTR_GRAPH_COMMENT = "graph.comment";
    private final RootProxy root;
    private final Map<String, ComponentProxy> knownChildren;
    private final Set<Connection> knownConnections;
    private final ContainerListener containerListener;
    private final InfoListener infoListener;

    private final PraxisGraphScene<String> scene;
    private final ExplorerManager manager;
    private final Lookup lookup;

    private final LocationAction location;
    private final Action goUpAction;
    private final Action deleteAction;
    private final Action sceneCommentAction;
    private final Action exportAction;
    private final JMenuItem addMenu;

    private JComponent panel;
    private ContainerProxy container;

    private ActionSupport actionSupport;
    private final Point activePoint = new Point();
    private boolean sync;

    private final ColorsAction[] colorsActions;

    public GraphEditor(RootProxy root, String category) {
        this.root = root;
        knownChildren = new LinkedHashMap<>();
        knownConnections = new LinkedHashSet<>();

        scene = new PraxisGraphScene<>(new ConnectProviderImpl(), new MenuProviderImpl());
        manager = new ExplorerManager();
        manager.setRootContext(root.getNodeDelegate());
        if (root instanceof ContainerProxy) {
            container = (ContainerProxy) root;
        }

        deleteAction = new DeleteAction();
        
        PaletteController palette = PaletteUtils.getPalette("core", category);
        
        lookup = new ProxyLookup(ExplorerUtils.createLookup(manager, buildActionMap(manager)),
                Lookups.fixed(palette));
        
        addMenu = new MenuView.Menu(
                palette.getRoot().lookup(Node.class),
                new NodeAcceptor() {
            @Override
            public boolean acceptNodes(Node[] nodes) {
                if (nodes.length == 1) {
                    ComponentType type = nodes[0].getLookup().lookup(ComponentType.class);
                    if (type != null) {
                        EventQueue.invokeLater(() -> acceptComponentType(type));
                        return true;
                    }
                    FileObject fo = nodes[0].getLookup().lookup(FileObject.class);
                    if (fo != null) {
                        EventQueue.invokeLater(() -> acceptImport(fo));
                        return true;
                    }
                }
                return false;
            }
        });
        addMenu.setIcon(null);
        addMenu.setText("Add");
        
        scene.addObjectSceneListener(new SelectionListener(),
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
        goUpAction = new GoUpAction();
        location = new LocationAction();
        containerListener = new ContainerListener();
        infoListener = new InfoListener();

        Colors[] colorsValues = Colors.values();
        colorsActions = new ColorsAction[colorsValues.length];
        for (int i = 0; i < colorsValues.length; i++) {
            colorsActions[i] = new ColorsAction(colorsValues[i]);
        }

        sceneCommentAction = new CommentAction(scene);
        exportAction = new ExportAction(this, manager);
        setupSceneActions();
    }

    private ActionMap buildActionMap(ExplorerManager em) {
        ActionMap am = new ActionMap();
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
        scene.getCommentWidget().getActions().addAction(ActionFactory.createEditAction(new EditProvider() {

            @Override
            public void edit(Widget widget) {
                sceneCommentAction.actionPerformed(new ActionEvent(scene, ActionEvent.ACTION_PERFORMED, "edit"));
            }
        }));
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
        menu.addSeparator();
        menu.add(exportAction);
        menu.addSeparator();
        JMenu colorsMenu = new JMenu("Colors");
        for (ColorsAction action : colorsActions) {
            colorsMenu.add(action);
        }
        menu.add(colorsMenu);
        menu.add(new CommentAction(widget));
        return menu;
    }

    private JPopupMenu getConnectionPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(deleteAction);
        return menu;
    }

    private JPopupMenu getScenePopup() {
        JPopupMenu menu = new JPopupMenu();
//        menu.add(deleteAction);
        menu.add(addMenu);  
        menu.addSeparator();
        JMenu colorsMenu = new JMenu("Colors");
        for (ColorsAction action : colorsActions) {
            colorsMenu.add(action);
        }
        menu.add(colorsMenu);
        menu.add(new CommentAction(scene));
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
        Syncable syncable = container.getLookup().lookup(Syncable.class);
        if (syncable != null) {
            syncable.removeKey(this);
        }
        for (Map.Entry<String, ComponentProxy> child : knownChildren.entrySet()) {
            removeChild(child.getKey(), child.getValue());
        }
        activePoint.setLocation(0, 0);
        knownChildren.clear();
        knownConnections.clear();
        location.address.setText("");
    }

    private void buildScene() {

        container.addPropertyChangeListener(containerListener);
        Syncable syncable = container.getLookup().lookup(Syncable.class);
        if (syncable != null) {
            syncable.addKey(this);
        }

        container.getNodeDelegate().getChildren().getNodes();

        sync(true);

        goUpAction.setEnabled(container.getParent() != null);
        location.address.setText(container.getAddress().toString());

        scene.setSchemeColors(getColorsFromAttribute(container).getSchemeColors());
        scene.setComment(Utils.getAttr(container, ATTR_GRAPH_COMMENT));
        scene.validate();
    }

    private void buildChild(String id, final ComponentProxy cmp) {

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Adding " + cmp.getAddress() + " to graph.");
        }
        String name = cmp instanceof ContainerProxy ? id + "/.." : id;
        NodeWidget widget = scene.addNode(id, name);
        widget.setSchemeColors(getColorsFromAttribute(cmp).getSchemeColors());
        widget.setToolTipText(cmp.getType().toString());
        widget.setPreferredLocation(resolveLocation(id, cmp));
        if ("true".equals(Utils.getAttr(cmp, ATTR_GRAPH_MINIMIZED))) {
            widget.setMinimized(true);
        }
        updateWidgetComment(widget,
                Utils.getAttr(cmp, ATTR_GRAPH_COMMENT, ""),
                cmp instanceof ContainerProxy);
        widget.getActions().addAction(ActionFactory.createEditAction(new EditProvider() {
            @Override
            public void edit(Widget widget) {
                cmp.getNodeDelegate().getPreferredAction().actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit"));
            }
        }));
        final CommentAction commentAction = new CommentAction(widget);
        widget.getCommentWidget().getActions().addAction(ActionFactory.createEditAction(new EditProvider() {

            @Override
            public void edit(Widget widget) {
                commentAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit"));
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
        cmp.addPropertyChangeListener(infoListener);
    }

    private void rebuildChild(String id, ComponentProxy cmp) {
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Rebuilding " + cmp.getAddress() + " in graph.");
        }
        // remove all connections to this component from known connections list
        Iterator<Connection> itr = knownConnections.iterator();
        while (itr.hasNext()) {
            Connection con = itr.next();
            if (con.getChild1().equals(id) || con.getChild2().equals(id)) {
                LOG.finest("Removing connection : " + con);
                itr.remove();
            }
        }
        // match visual state by removing all pins and edges from graph node
        List<PinID<String>> pins = new ArrayList<>(scene.getNodePins(id));
        LOG.finest(pins.toString());
        for (PinID<String> pin : pins) {
            LOG.finest("Removing pin : " + pin);
            scene.removePinWithEdges(pin);
        }
        ComponentInfo info = cmp.getInfo();
        for (String portID : info.getPorts()) {
            PortInfo pi = info.getPortInfo(portID);
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("Building port " + portID);
            }
            buildPin(id, portID, pi);
        }
        syncConnections();
    }

    private void removeChild(String id, ComponentProxy cmp) {
        cmp.removePropertyChangeListener(infoListener);
        scene.removeNodeWithEdges(id);
        // @TODO temporary fix for moving dynamic components?
        activePoint.x = 0;
        activePoint.y = 0;
    }

    private Point resolveLocation(String id, ComponentProxy cmp) {
        int x = activePoint.x;
        int y = activePoint.y;
        try {
            String xStr = Utils.getAttr(cmp, ATTR_GRAPH_X);
            String yStr = Utils.getAttr(cmp, ATTR_GRAPH_Y);
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

    private Colors getColorsFromAttribute(ComponentProxy cmp) {
        String colorsAttr = Utils.getAttr(cmp, ATTR_GRAPH_COLORS);
        if (colorsAttr != null) {
            try {
                return Colors.valueOf(colorsAttr);
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }
        return Colors.Default;
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

    private boolean buildConnection(Connection connection) {
        PinID<String> p1 = new PinID<>(connection.getChild1(), connection.getPort1());
        PinID<String> p2 = new PinID<>(connection.getChild2(), connection.getPort2());
        if (scene.isPin(p1) && scene.isPin(p2)) {
            EdgeWidget widget = scene.connect(connection.getChild1(), connection.getPort1(),
                    connection.getChild2(), connection.getPort2());
            widget.setToolTipText(connection.getChild1() + "!" + connection.getPort1() + " -> "
                    + connection.getChild2() + "!" + connection.getPort2());
            return true;
        } else {
            return false;
        }

    }

    private boolean removeConnection(Connection connection) {
        EdgeID<String> edge = new EdgeID<>(new PinID<>(connection.getChild1(), connection.getPort1()),
                new PinID<>(connection.getChild2(), connection.getPort2()));
        if (scene.isEdge(edge)) {
            scene.disconnect(connection.getChild1(), connection.getPort1(),
                    connection.getChild2(), connection.getPort2());
            return true;
        } else {
            return false;
        }

    }

    private void syncChildren() {
        if (container == null) {
            return;
        }
        List<String> ch = Arrays.asList(container.getChildIDs());
        Set<String> tmp = new LinkedHashSet<String>(knownChildren.keySet());
        tmp.removeAll(ch);
        // tmp now contains children that have been removed from model
        for (String id : tmp) {
            ComponentProxy cmp = knownChildren.remove(id);
            removeChild(id, cmp);

        }
        tmp.clear();
        tmp.addAll(ch);
        tmp.removeAll(knownChildren.keySet());
        // tmp now contains children that have been added to model
        for (String id : tmp) {
            ComponentProxy cmp = container.getChild(id);
            if (cmp != null) {
                buildChild(id, cmp);
                knownChildren.put(id, cmp);
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
            if (buildConnection(con)) {
                knownConnections.add(con);
            } else {
                // leave for later?
            }

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

    private void updateWidgetComment(final NodeWidget widget, final String text, final boolean container) {

        if (!container) {
            widget.setComment(text);
            return;
        }
        // OK, we have a container, trim text
        int delim = text.indexOf("\n\n");
        if (delim >= 0) {
            widget.setComment(text.substring(0, delim) + "...");
        } else {
            widget.setComment(text);
        }
        scene.revalidate();

    }

    private ComponentProxy findComponent(Widget widget) {
        if (widget == scene) {
            return container;
        }
        return findComponent(scene.findObject(widget));
    }

    private ComponentProxy findComponent(Object obj) {
        if (obj instanceof Widget) {
            return findComponent((Widget) obj);
        }
        if (obj instanceof String) {
            return container.getChild(obj.toString());
        }
        return null;
    }

    private void acceptComponentType(final ComponentType type) {
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

    private String getFreeID(ComponentType type) {
        Set<String> existing = new HashSet<String>(Arrays.asList(container.getChildIDs()));
        return EditorUtils.findFreeID(existing, EditorUtils.extractBaseID(type), true);

    }

    private class ContainerListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (sync) {
                if (ContainerInterface.CHILDREN.equals(evt.getPropertyName())) {
                    syncChildren();
                } else if (ContainerInterface.CONNECTIONS.equals(evt.getPropertyName())) {
                    syncConnections();
                }
            }

        }
    }

    private class InfoListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ComponentInterface.INFO.equals(evt.getPropertyName())) {
                Object src = evt.getSource();
                assert src instanceof ComponentProxy;
                if (src instanceof ComponentProxy) {
                    ComponentProxy cmp = (ComponentProxy) src;
                    String id = cmp.getAddress().getID();
                    rebuildChild(id, cmp);
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
                Utils.setAttr(cmp, ATTR_GRAPH_X, x);
                Utils.setAttr(cmp, ATTR_GRAPH_Y, y);
                Utils.setAttr(cmp, ATTR_GRAPH_MINIMIZED,
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
            String msg = count > 1
                    ? "Delete " + count + " components?"
                    : "Delete " + components.get(0) + "?";
            String title = "Confirm deletion";
            NotifyDescriptor desc = new NotifyDescriptor.Confirmation(msg, title, NotifyDescriptor.YES_NO_OPTION);

            return NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(desc));
        }

    }

    private class ColorsAction extends AbstractAction {

        private final Colors colors;

        private ColorsAction(Colors colors) {
            super(colors.name());
            this.colors = colors;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean foundNode = false;
            for (Object obj : scene.getSelectedObjects()) {
                if (obj instanceof String) {
                    ComponentProxy cmp = container.getChild(obj.toString());
                    NodeWidget widget = (NodeWidget) scene.findWidget(obj);
                    widget.setSchemeColors(colors.getSchemeColors());
                    setColorsAttr(cmp);
                    foundNode = true;
                }
            }
            if (!foundNode) {
                scene.setSchemeColors(colors.getSchemeColors());
                setColorsAttr(container);
            }
            scene.revalidate();
        }

        private void setColorsAttr(ComponentProxy cmp) {
            if (colors == Colors.Default) {
                Utils.setAttr(cmp, ATTR_GRAPH_COLORS, null);
            } else {
                Utils.setAttr(cmp, ATTR_GRAPH_COLORS, colors.name());
            }
        }

    }

    private class CommentAction extends AbstractAction {

        private final Widget widget;

        private CommentAction(Widget widget) {
            super("Comment...");
            this.widget = widget;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    String comment = findInitialText(widget);
                    JTextArea editor = new JTextArea(comment);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JScrollPane(editor));
                    panel.setPreferredSize(new Dimension(400, 300));
                    DialogDescriptor dlg = new DialogDescriptor(panel, "Comment");
                    editor.selectAll();
                    editor.requestFocusInWindow();
                    Object result = DialogDisplayer.getDefault().notify(dlg);
                    if (result != NotifyDescriptor.OK_OPTION) {
                        return;
                    }
                    comment = editor.getText();
                    if (widget == scene) {
                        scene.setComment(comment);
                        Utils.setAttr(container, ATTR_GRAPH_COMMENT, comment.isEmpty() ? null : comment);
                    } else if (widget instanceof NodeWidget) {
                        ComponentProxy cmp = findComponent(widget);
                        updateWidgetComment((NodeWidget) widget, comment, cmp instanceof ContainerProxy);
                        Utils.setAttr(cmp, ATTR_GRAPH_COMMENT, comment.isEmpty() ? null : comment);
                        for (Object obj : scene.getSelectedObjects()) {
                            ComponentProxy additional = findComponent(obj);
                            if (additional != null) {
                                NodeWidget n = (NodeWidget) scene.findWidget(obj);
                                if (n != widget) {
                                    updateWidgetComment(n, comment, cmp instanceof ContainerProxy);
                                    Utils.setAttr(additional, ATTR_GRAPH_COMMENT, comment.isEmpty() ? null : comment);
                                }

                            }
                        }
                    }
                    scene.validate();
                }
            };
            EventQueue.invokeLater(runnable);
        }

        private String findInitialText(Widget widget) {
            ComponentProxy cmp = findComponent(widget);
            if (cmp != null) {
                String comment = Utils.getAttr(cmp, ATTR_GRAPH_COMMENT);
                return comment == null ? "" : comment;
            } else {
                return "";
            }
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
                EventQueue.invokeLater(() -> acceptComponentType(type));
                return;
            }
            FileObject file = extractFile(transferable);
            if (file != null) {
                EventQueue.invokeLater(() -> acceptImport(file));
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

    }
}
