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
package org.praxislive.ide.pxr.graph;

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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.DefaultEditorKit;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.PortInfo;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.protocols.ContainerProtocol;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.core.ui.api.Actions;
import org.praxislive.ide.graph.Alignment;
import org.praxislive.ide.graph.EdgeID;
import org.praxislive.ide.graph.EdgeWidget;
import org.praxislive.ide.graph.NodeWidget;
import org.praxislive.ide.graph.ObjectSceneAdaptor;
import org.praxislive.ide.graph.PinID;
import org.praxislive.ide.graph.PinWidget;
import org.praxislive.ide.graph.PraxisGraphScene;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.api.ActionSupport;
import org.praxislive.ide.pxr.api.EditorUtils;
import org.praxislive.ide.pxr.spi.RootEditor;
import java.awt.AWTEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.MenuView;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.core.Connection;
import org.praxislive.core.Value;
import org.praxislive.ide.code.api.SharedCodeInfo;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.pxr.api.ComponentPalette;

/**
 *
 */
public class GraphEditor extends RootEditor {

    private final static Logger LOG = Logger.getLogger(GraphEditor.class.getName());
    final static String ATTR_GRAPH_X = "graph.x";
    final static String ATTR_GRAPH_Y = "graph.y";
    final static String ATTR_GRAPH_MINIMIZED = "graph.minimized";
    final static String ATTR_GRAPH_COMMENT = "graph.comment";
    final static String ATTR_GRAPH_PROPERTIES = "graph.properties";

    private final PraxisProject project;
    private final FileObject file;
    private final RootProxy root;
    private final Map<String, ComponentProxy> knownChildren;
    private final Set<Connection> knownConnections;
    private final ContainerListener containerListener;
    private final ComponentListener infoListener;

    private final PraxisGraphScene<String> scene;
    private final ExplorerManager manager;
    private final Lookup lookup;
    private final ComponentPalette palette;

    private final LocationAction location;
    private final Action goUpAction;
    private final Action deleteAction;
    private final Action sceneCommentAction;
    private final Action exportAction;
    private final Action copyAction;
    private final Action pasteAction;
    private final Action duplicateAction;
    private final JMenuItem addMenu;

    private JComponent panel;
    private JComponent sharedCodePanel;
    private Action sharedCodeAction;
    private JComponent actionPanel;
    private ContainerProxy container;

    private ActionSupport actionSupport;
    private final Point activePoint = new Point();
    private PropertyMode propertyMode = PropertyMode.Default;
    private boolean sync;
    private boolean ignoreAttributeChanges;

    public GraphEditor(PraxisProject project, FileObject file, RootProxy root, String category) {
        this.project = project;
        this.file = file;
        this.root = root;
        knownChildren = new LinkedHashMap<>();
        knownConnections = new LinkedHashSet<>();

        scene = new PraxisGraphScene<>(new ConnectProviderImpl(), new MenuProviderImpl());
        scene.setOrthogonalRouting(false);
        manager = new ExplorerManager();
        if (root instanceof ContainerProxy) {
            container = (ContainerProxy) root;
        }

        deleteAction = new DeleteAction();
        copyAction = new CopyActionPerformer(this, manager);
        pasteAction = new PasteActionPerformer(this, manager);
        duplicateAction = new DuplicateActionPerformer(this, manager);

        palette = ComponentPalette.create(container);

        var rootNode = root.getNodeDelegate();
        manager.setRootContext(rootNode);
        manager.setExploredContext(rootNode, new Node[]{rootNode});

        lookup = new ProxyLookup(ExplorerUtils.createLookup(manager, buildActionMap()),
                Lookups.fixed(palette.controller()));

        addMenu = new MenuView.Menu(
                palette.root(), (Node[] nodes) -> {
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
        });
        addMenu.setIcon(null);
        addMenu.setText("Add");

        scene.addObjectSceneListener(new SelectionListener(),
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
        goUpAction = new GoUpAction();
        location = new LocationAction();
        containerListener = new ContainerListener();
        infoListener = new ComponentListener();

        sceneCommentAction = new CommentAction(scene);
        exportAction = new ExportAction(this, manager);
        setupSceneActions();
    }

    private ActionMap buildActionMap() {
        ActionMap am = new ActionMap();
        deleteAction.setEnabled(false);
        am.put("delete", deleteAction);
        am.put(DefaultEditorKit.copyAction, copyAction);
        am.put(DefaultEditorKit.pasteAction, pasteAction);
        am.put(Actions.DUPLICATE_KEY, duplicateAction);
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
        menu.add(copyAction);
        menu.add(duplicateAction);
        menu.add(deleteAction);
        menu.addSeparator();
        menu.add(exportAction);
        menu.addSeparator();
        menu.add(new CommentAction(widget));
        return menu;
    }

    private JPopupMenu getConnectionPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(deleteAction);
        return menu;
    }

    private JPopupMenu getPinPopup(PinWidget widget) {
        JPopupMenu menu = new JPopupMenu();
        PinID<String> pin = (PinID<String>) scene.findObject(widget);
        boolean enabled = (container.getInfo().controls().contains("ports"));
        Action action = new AddPortToParentAction(this, pin);
        action.setEnabled(enabled);
        menu.add(action);
        return menu;
    }

    private JPopupMenu getScenePopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(addMenu);
        menu.add(pasteAction);
        menu.addSeparator();
        boolean addSep = false;
        for (Action a : container.getNodeDelegate().getActions(false)) {
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

        if (sharedCodeAction != null) {
            menu.add(new JCheckBoxMenuItem(sharedCodeAction));
            menu.addSeparator();
        }

        JMenu propertyModeMenu = new JMenu("Properties");
        propertyModeMenu.add(new JRadioButtonMenuItem(new PropertyModeAction("Default", PropertyMode.Default)));
        propertyModeMenu.add(new JRadioButtonMenuItem(new PropertyModeAction("Show all", PropertyMode.Show)));
        propertyModeMenu.add(new JRadioButtonMenuItem(new PropertyModeAction("Hide all", PropertyMode.Hide)));
        menu.add(propertyModeMenu);

        menu.add(new CommentAction(scene));
        return menu;
    }

    PraxisGraphScene<String> getScene() {
        return scene;
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

    void resetActivePoint() {
        // @TODO properly layout added components
        activePoint.x = 100;
        activePoint.y = 100;
    }

    void installToActionPanel(JComponent actionComponent) {
        actionPanel.add(actionComponent);
        actionPanel.revalidate();
    }

    void clearActionPanel() {
        for (Component c : actionPanel.getComponents()) {
            actionPanel.remove(c);
        }
        actionPanel.revalidate();
        scene.getView().requestFocusInWindow();
    }

    ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void componentActivated() {
        if (panel == null) {
            return;
        }
        scene.getView().requestFocusInWindow();
    }

    @Override
    public void dispose() {
        super.dispose();
        palette.dispose();
    }

    @Override
    public JComponent getEditorComponent() {
        if (panel == null) {
            JPanel viewPanel = new JPanel(new BorderLayout());
            JComponent sceneView = scene.createView();
            sceneView.addMouseListener(new ActivePointListener());
            JScrollPane scroll = new JScrollPane(
                    sceneView,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            viewPanel.add(scroll, BorderLayout.CENTER);

            JPanel overlayPanel = new JPanel();
            overlayPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(0, 0, 20, 20);
            gbc.anchor = GridBagConstraints.SOUTHEAST;
            JPanel satellitePanel = new JPanel(new BorderLayout());
            satellitePanel.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            satellitePanel.add(scene.createSatelliteView());
            overlayPanel.add(satellitePanel, gbc);
            overlayPanel.setOpaque(false);

            JLayeredPane layered = new JLayeredPane();
            layered.setLayout(new OverlayLayout(layered));
            layered.add(viewPanel, JLayeredPane.DEFAULT_LAYER);
            layered.add(overlayPanel, JLayeredPane.PALETTE_LAYER);

            panel = new JPanel(new BorderLayout());
            panel.add(layered, BorderLayout.CENTER);

            actionPanel = new JPanel(new BorderLayout());
            panel.add(actionPanel, BorderLayout.SOUTH);

            SharedCodeInfo sharedCtxt = root.getLookup().lookup(SharedCodeInfo.class);
            if (sharedCtxt != null) {
                sharedCodePanel = new SharedCodeComponent(this, sharedCtxt.getFolder());
                sharedCodePanel.setVisible(false);
                panel.add(sharedCodePanel, BorderLayout.WEST);
                sharedCodeAction = new SharedCodeToggleAction();
                JToggleButton sharedCodeButton = new JToggleButton(sharedCodeAction);
                gbc = new GridBagConstraints();
                gbc.gridx = 0;
                gbc.gridy = 1;
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.insets = new Insets(0, 20, 20, 0);
                gbc.anchor = GridBagConstraints.SOUTHWEST;
                overlayPanel.add(sharedCodeButton, gbc);
            }

            InputMap im = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap am = panel.getActionMap();
            im.put(KeyStroke.getKeyStroke("typed /"), "select");
            im.put(KeyStroke.getKeyStroke("typed ."), "call");
            im.put(KeyStroke.getKeyStroke("typed ~"), "connect");
            im.put(KeyStroke.getKeyStroke("typed !"), "disconnect");
            im.put(KeyStroke.getKeyStroke("typed @"), "add-component");
            am.put("select", new SelectAction(this));
            am.put("call", new CallAction(this));
            am.put("connect", new ConnectAction(this, false));
            am.put("disconnect", new ConnectAction(this, true));
            am.put("add-component", new AddAction(this));

            im.put(KeyStroke.getKeyStroke("alt shift F"), "format");
            am.put("format", new AbstractAction("format") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scene.layoutScene();
                }
            });
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "escape");
            am.put("escape", goUpAction);

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

    @Override
    public void sync() {
        syncAllAttributes();
    }

    private void clearScene() {
        syncAllAttributes();
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

        try {
            propertyMode = PropertyMode.valueOf(
                    Utils.getAttr(root, ATTR_GRAPH_PROPERTIES, "Default"));
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            propertyMode = PropertyMode.Default;
            Utils.setAttr(root, ATTR_GRAPH_PROPERTIES, null);
        }

        container.getNodeDelegate().getChildren().getNodes();

        syncGraph(true);

        goUpAction.setEnabled(container.getParent() != null);
        location.address.setText(container.getAddress().toString());

        scene.setComment(Utils.getAttr(container, ATTR_GRAPH_COMMENT));
        scene.validate();
    }

    private void buildChild(String id, final ComponentProxy cmp) {
        String name = cmp instanceof ContainerProxy ? id + "/.." : id;
        NodeWidget widget = scene.addNode(id, name);
        widget.setSchemeColors(Utils.colorsForComponent(cmp).getSchemeColors());
        widget.setToolTipText(cmp.getType().toString());
        configureWidgetFromAttributes(widget, cmp);
        if (cmp instanceof ContainerProxy) {
            ContainerOpenAction containerOpenAction = new ContainerOpenAction((ContainerProxy) cmp);
            widget.getActions().addAction(ActionFactory.createEditAction(w -> {
                AWTEvent current = EventQueue.getCurrentEvent();
                if (current instanceof InputEvent && ((InputEvent) current).isShiftDown()) {
                    cmp.getNodeDelegate().getPreferredAction().actionPerformed(
                            new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit", ActionEvent.SHIFT_MASK));
                } else {
                    containerOpenAction.actionPerformed(new ActionEvent(this,
                            ActionEvent.ACTION_PERFORMED,
                            "edit"));
                }
            }));
        } else {
            widget.getActions().addAction(ActionFactory.createEditAction(w -> {
                AWTEvent current = EventQueue.getCurrentEvent();
                int modifiers = (current instanceof InputEvent)
                        ? ((InputEvent) current).getModifiers() : 0;
                cmp.getNodeDelegate().getPreferredAction().actionPerformed(
                        new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit", modifiers)
                );
            }));
        }
        final CommentAction commentAction = new CommentAction(widget);
        widget.getCommentWidget().getActions().addAction(ActionFactory.createEditAction(new EditProvider() {

            @Override
            public void edit(Widget widget) {
                commentAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "edit"));
            }
        }));
        ComponentInfo info = cmp.getInfo();
        for (String portID : info.ports()) {
            PortInfo pi = info.portInfo(portID);
            buildPin(id, cmp, portID, pi);
        }
        cmp.addPropertyChangeListener(infoListener);
        syncConnections();
    }

    private void rebuildChild(String id, ComponentProxy cmp) {
        // remove all connections to this component from known connections list
        Iterator<Connection> itr = knownConnections.iterator();
        while (itr.hasNext()) {
            Connection con = itr.next();
            if (con.sourceComponent().equals(id) || con.targetComponent().equals(id)) {
                itr.remove();
            }
        }
        // match visual state by removing all pins and edges from graph node
        List<PinID<String>> pins = new ArrayList<>(scene.getNodePins(id));
        for (PinID<String> pin : pins) {
            scene.removePinWithEdges(pin);
        }
        ComponentInfo info = cmp.getInfo();
        for (String portID : info.ports()) {
            PortInfo pi = info.portInfo(portID);
            buildPin(id, cmp, portID, pi);
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

    private void configureWidgetFromAttributes(NodeWidget widget, ComponentProxy cmp) {
        widget.setPreferredLocation(resolveLocation(cmp));
        if ("true".equals(Utils.getAttr(cmp, ATTR_GRAPH_MINIMIZED))) {
            widget.setMinimized(true);
        }
        updateWidgetComment(widget,
                Utils.getAttr(cmp, ATTR_GRAPH_COMMENT, ""),
                cmp instanceof ContainerProxy);
        scene.validate();
    }

    private Point resolveLocation(ComponentProxy cmp) {
        int x = activePoint.x;
        int y = activePoint.y;
        try {
            String xStr = Utils.getAttr(cmp, ATTR_GRAPH_X);
            String yStr = Utils.getAttr(cmp, ATTR_GRAPH_Y);
            if (xStr != null) {
                x = Integer.parseInt(xStr);
            }
            if (yStr != null) {
                y = Integer.parseInt(yStr);
            }
        } catch (Exception ex) {

        }
        // @TODO what to do about importing components without positions?
        // if point not set, check for widget at point?
        return new Point(x, y);
    }

    private void buildPin(String cmpID, ComponentProxy cmp, String pinID, PortInfo info) {
        boolean primary = info.portType().startsWith("Audio")
                || info.portType().startsWith("Video");
        PinWidget pin = scene.addPin(cmpID, pinID, getPinAlignment(info));
        pin.setSchemeColors(Utils.colorsForPortType(info.portType()).getSchemeColors());
        Font font = pin.getFont();
        if (primary) {
            pin.setFont(font.deriveFont(Font.BOLD));
        } else {
//            pin.setFont(font.deriveFont(font.getSize2D() * 0.85f));
        }
        String category = info.properties().getString("category", "");
        if (category.isEmpty()) {
            pin.setToolTipText(pinID + " : " + info.portType());
        } else {
            pin.setToolTipText(pinID + " : " + info.portType() + " : " + category);
        }

        if (propertyMode == PropertyMode.Hide) {
            return;
        }

        ControlInfo control = cmp.getInfo().controls().contains(pinID)
                ? cmp.getInfo().controlInfo(pinID) : null;
        if (control != null && (control.controlType() == ControlInfo.Type.Property
                || control.controlType() == ControlInfo.Type.ReadOnlyProperty)
                && (propertyMode == PropertyMode.Show
                || control.properties().getBoolean("preferred", false))) {
            Node.Property<?> matchingProp = Utils.findMatchingProperty(cmp, pinID);
            if (matchingProp != null) {
                pin.addChild(new PropertyWidget(scene, control, cmp.getNodeDelegate(), matchingProp));
            }
        }

    }

    private Alignment getPinAlignment(PortInfo info) {
        switch (info.direction()) {
            case IN:
                return Alignment.Left;
            case OUT:
                return Alignment.Right;
            default:
                return Alignment.Center;
        }
    }

    private boolean buildConnection(Connection connection) {
        PinID<String> p1 = new PinID<>(connection.sourceComponent(), connection.sourcePort());
        PinID<String> p2 = new PinID<>(connection.targetComponent(), connection.targetPort());
        if (scene.isPin(p1) && scene.isPin(p2)) {
            PinWidget pw1 = (PinWidget) scene.findWidget(p1);
            PinWidget pw2 = (PinWidget) scene.findWidget(p2);
            if (pw1.getAlignment() == Alignment.Left && pw2.getAlignment() == Alignment.Right) {
                EdgeWidget widget = scene.connect(connection.targetComponent(), connection.targetPort(),
                        connection.sourceComponent(), connection.sourcePort());
                widget.setToolTipText(connection.targetComponent() + "!" + connection.targetPort() + " -> "
                        + connection.sourceComponent() + "!" + connection.sourcePort());
            } else {
                EdgeWidget widget = scene.connect(connection.sourceComponent(), connection.sourcePort(),
                        connection.targetComponent(), connection.targetPort());
                widget.setToolTipText(connection.sourceComponent() + "!" + connection.sourcePort() + " -> "
                        + connection.targetComponent() + "!" + connection.targetPort());
            }
            return true;
        } else {
            return false;
        }

    }

    private boolean removeConnection(Connection connection) {
        EdgeID<String> edge = new EdgeID<>(new PinID<>(connection.sourceComponent(), connection.sourcePort()),
                new PinID<>(connection.targetComponent(), connection.targetPort()));
        if (scene.isEdge(edge)) {
            scene.disconnect(connection.sourceComponent(), connection.sourcePort(),
                    connection.targetComponent(), connection.targetPort());
            return true;
        } else {
            return false;
        }

    }

    private void syncChildren(boolean updateSelection) {
        if (container == null) {
            return;
        }
        List<String> ch = container.children().collect(Collectors.toList());
        Set<String> tmp = new LinkedHashSet<>(knownChildren.keySet());
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
        if (updateSelection && !tmp.isEmpty()) {
            scene.userSelectionSuggested(tmp, false);
            scene.setFocusedObject(tmp.iterator().next());
        }
        scene.validate();
    }

    private void syncConnections() {
        if (container == null) {
            return;
        }
        List<Connection> cons = container.connections().collect(Collectors.toList());
        Set<Connection> tmp = new LinkedHashSet<>(knownConnections);
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

    void syncGraph(boolean sync) {
        syncGraph(sync, false);
    }

    void syncGraph(boolean sync, boolean updateSelection) {
        if (sync) {
            this.sync = true;
            syncChildren(updateSelection);
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

    private void syncAllAttributes() {
        if (container == null) {
            return;
        }
        container.children().map(id -> container.getChild(id))
                .forEach(this::syncAttributes);
    }

    private void syncAttributes(ComponentProxy cmp) {
        ignoreAttributeChanges = true;
        Widget widget = scene.findWidget(cmp.getAddress().componentID());
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
        ignoreAttributeChanges = false;
    }

    void acceptComponentType(final ComponentType type) {
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                "ID:", "Enter an ID for " + type);
        dlg.setInputText(getFreeID(type));
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            final String id = dlg.getInputText();
            container.addChild(id, type)
                    .thenRun(() -> syncGraph(true, true))
                    .exceptionally(ex -> {
                        syncGraph(true);
                        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating component", NotifyDescriptor.ERROR_MESSAGE));
                        return null;
                    });

            syncGraph(false);

        }
    }

    void acceptImport(FileObject file) {
        List<String> warnings = new ArrayList<>(1);
        if (getActionSupport().importSubgraph(container, file, warnings, new Callback() {
            @Override
            public void onReturn(List<Value> args) {
                syncGraph(true, true);
                if (!warnings.isEmpty()) {
                    String errors = warnings.stream().collect(Collectors.joining("\n"));
                    DialogDisplayer.getDefault().notify(
                            new NotifyDescriptor.Message(errors, NotifyDescriptor.WARNING_MESSAGE));
                }
            }

            @Override
            public void onError(List<Value> args) {
                syncGraph(true);
                String errors = warnings.stream().collect(Collectors.joining("\n"));
                DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(errors, NotifyDescriptor.ERROR_MESSAGE));
            }
        })) {
            syncGraph(false);
        }
    }

    private String getFreeID(ComponentType type) {
        Set<String> existing = container.children().collect(Collectors.toSet());
        return EditorUtils.findFreeID(existing, EditorUtils.extractBaseID(type), true);

    }

    private class ContainerListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (sync) {
                if (ContainerProtocol.CHILDREN.equals(evt.getPropertyName())) {
                    syncChildren(false);
                } else if (ContainerProtocol.CONNECTIONS.equals(evt.getPropertyName())) {
                    syncConnections();
                }
            }

        }
    }

    private class ComponentListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ComponentProtocol.INFO.equals(evt.getPropertyName())) {
                Object src = evt.getSource();
                assert src instanceof ComponentProxy;
                if (src instanceof ComponentProxy) {
                    ComponentProxy cmp = (ComponentProxy) src;
                    String id = cmp.getAddress().componentID();
                    rebuildChild(id, cmp);
                }
            } else if (ComponentProtocol.META.equals(evt.getPropertyName())) {
                if (!ignoreAttributeChanges) {
                    Object src = evt.getSource();
                    assert src instanceof ComponentProxy;
                    if (src instanceof ComponentProxy cmp) {
                        Widget w = scene.findWidget(cmp.getAddress().componentID());
                        if (w instanceof NodeWidget node) {
                            configureWidgetFromAttributes(node, cmp);
                        }
                    }
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
            PinWidget pw1 = (PinWidget) sourceWidget;
            PinWidget pw2 = (PinWidget) targetWidget;
            if (pw1.getAlignment() == Alignment.Left || pw2.getAlignment() == Alignment.Right) {
                PinWidget tmp = pw2;
                pw2 = pw1;
                pw1 = tmp;
            }
            PinID<String> p1 = (PinID<String>) scene.findObject(pw1);
            PinID<String> p2 = (PinID<String>) scene.findObject(pw2);
            container.connect(Connection.of(
                    p1.getParent(),
                    p1.getName(),
                    p2.getParent(),
                    p2.getName())
            );
        }
    }

    private class MenuProviderImpl implements PopupMenuProvider {

        @Override
        public JPopupMenu getPopupMenu(Widget widget, Point localLocation) {
            if (widget instanceof NodeWidget) {
                return getComponentPopup((NodeWidget) widget);
            } else if (widget instanceof EdgeWidget) {
                return getConnectionPopup();
            } else if (widget instanceof PinWidget) {
                return getPinPopup((PinWidget) widget);
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
                        syncAttributes(cmp);
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
                                syncAttributes(cmp);
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
                    container.removeChild((String) obj);
                } else if (obj instanceof EdgeID) {
                    EdgeID edge = (EdgeID) obj;
                    PinID p1 = edge.getPin1();
                    PinID p2 = edge.getPin2();
                    Connection con = Connection.of(p1.getParent().toString(), p1.getName(),
                            p2.getParent().toString(), p2.getName());
                    container.disconnect(con);
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

    private class PropertyModeAction extends AbstractAction {

        private final PropertyMode mode;

        private PropertyModeAction(String text, PropertyMode mode) {
            super(text);
            this.mode = mode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (container == null || propertyMode == mode) {
                return;
            }
            clearScene();
            Utils.setAttr(root, ATTR_GRAPH_PROPERTIES, mode.toString());
            buildScene();
        }

        @Override
        public Object getValue(String key) {
            if (Action.SELECTED_KEY.equals(key)) {
                return propertyMode == mode;
            } else {
                return super.getValue(key);
            }
        }

    }

    private class SharedCodeToggleAction extends AbstractAction {

        private SharedCodeToggleAction() {
            super("Shared Code");
            putValue(Action.SELECTED_KEY, sharedCodePanel.isVisible());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            sharedCodePanel.setVisible(Boolean.TRUE.equals(getValue(Action.SELECTED_KEY)));
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
                            "org/praxislive/ide/pxr/graph/resources/go-up.png",
                            true));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ContainerProxy parent = container.getParent();
            if (parent != null) {
                clearScene();
                String childID = container.getAddress().componentID();
                container = parent;
                buildScene();
                scene.setSelectedObjects(Collections.singleton(childID));
                scene.setFocusedObject(childID);
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
