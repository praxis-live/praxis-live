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
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.core.ui.api.Actions;
import org.praxislive.ide.pxr.graph.scene.Alignment;
import org.praxislive.ide.pxr.graph.scene.EdgeID;
import org.praxislive.ide.pxr.graph.scene.EdgeWidget;
import org.praxislive.ide.pxr.graph.scene.NodeWidget;
import org.praxislive.ide.pxr.graph.scene.ObjectSceneAdaptor;
import org.praxislive.ide.pxr.graph.scene.PinID;
import org.praxislive.ide.pxr.graph.scene.PinWidget;
import org.praxislive.ide.pxr.graph.scene.PraxisGraphScene;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.api.ActionSupport;
import org.praxislive.ide.pxr.api.EditorUtils;
import org.praxislive.ide.pxr.spi.RootEditor;
import java.awt.AWTEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.model.ObjectSceneEvent;
import org.netbeans.api.visual.model.ObjectSceneEventType;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.nodes.NodeTransfer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;
import org.praxislive.core.Connection;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.core.api.Disposable;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
@Messages({
    "LBL_PropertyModeAction=Properties",
    "LBL_PropertyModeDefault=Default",
    "LBL_PropertyModeShowAll=Show all",
    "LBL_PropertyModeHideAll=Hide all"
})
public final class GraphEditor implements RootEditor {

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
    private final ComponentListener componentListener;
    private final SelectionListener selectionListener;
    private final Map<String, PArray> exposedTools;

    private final PraxisGraphScene<String> scene;
    private final ExplorerManager manager;
    private final Lookup lookup;

    private final LocationAction location;
    private final Action addAction;
    private final Action goUpAction;
    private final Action deleteAction;
    private final Action sceneCommentAction;
    private final Action exportAction;
    private final Action copyAction;
    private final Action pasteAction;
    private final Action duplicateAction;
    private final Action sharedCodeAction;

    private JComponent panel;
    private ContainerProxy container;

    private final Point activePoint = new Point();
    private PropertyMode propertyMode = PropertyMode.Default;
    private boolean sync;
    private boolean ignoreAttributeChanges;

    public GraphEditor(RootProxy proxy, RootEditor.Context context) {
        this.project = context.project().orElseThrow();
        this.file = context.file().orElseThrow();
        this.root = proxy;
        knownChildren = new LinkedHashMap<>();
        knownConnections = new LinkedHashSet<>();
        exposedTools = new HashMap<>();

        scene = new PraxisGraphScene<>(new ConnectProviderImpl(), new MenuProviderImpl());
        scene.setOrthogonalRouting(false);
        manager = context.explorerManager();
        if (root instanceof ContainerProxy c) {
            container = c;
        }

        deleteAction = new DeleteAction();
        copyAction = ActionSupport.createCopyAction(this, manager);
        pasteAction = ActionSupport.createPasteAction(this, manager);
        duplicateAction = ActionSupport.createDuplicateAction(this, manager);
        exportAction = ActionSupport.createExportAction(this, manager);
        sharedCodeAction = context.sharedCodeAction().orElse(null);

        var rootNode = root.getNodeDelegate();
        manager.setRootContext(rootNode);
        manager.setExploredContext(rootNode, new Node[]{rootNode});

        lookup = Lookups.fixed(new PositionTransform.CopyExport(this),
                new PositionTransform.ImportPaste(this));

        addAction = org.openide.awt.Actions.forID("PXR", "org.praxislive.ide.pxr.AddChildAction");

        selectionListener = new SelectionListener();
        scene.addObjectSceneListener(selectionListener,
                ObjectSceneEventType.OBJECT_SELECTION_CHANGED);
        manager.addPropertyChangeListener(selectionListener);
        goUpAction = new GoUpAction();
        location = new LocationAction();
        containerListener = new ContainerListener();
        componentListener = new ComponentListener();

        sceneCommentAction = new CommentAction(scene);
        setupSceneActions();
    }

    private ActionMap buildActionMap(ActionMap parent) {
        ActionMap am = new ActionMap();
        am.setParent(parent);
        deleteAction.setEnabled(false);
        am.put("delete", deleteAction);
        am.put(DefaultEditorKit.copyAction, copyAction);
        am.put(DefaultEditorKit.pasteAction, pasteAction);
        am.put(Actions.DUPLICATE_KEY, duplicateAction);
        return am;
    }

    private void setupSceneActions() {
        scene.getActions().addAction(ActionFactory.createAcceptAction(new AcceptProviderImpl()));
        scene.setCommentEditProvider(widget
                -> sceneCommentAction.actionPerformed(new ActionEvent(scene, ActionEvent.ACTION_PERFORMED, "edit")));
    }

    private JPopupMenu getComponentPopup(NodeWidget widget) {
        List<Action> actions = new ArrayList<>();
        Object obj = scene.findObject(widget);
        if (obj instanceof String id) {
            ComponentProxy cmp = container.getChild(id);
            if (cmp instanceof ContainerProxy container) {
                actions.add(new ContainerOpenAction(container));
                actions.add(null);
            }
            if (cmp != null) {
                actions.addAll(Arrays.asList(cmp.getNodeDelegate().getActions(false)));
            }
        }
        actions.add(null);
        actions.add(copyAction);
        actions.add(duplicateAction);
        actions.add(deleteAction);
        actions.add(null);
        actions.add(exportAction);
        actions.add(null);
        actions.add(new CommentAction(widget));
        return Utilities.actionsToPopup(actions.toArray(Action[]::new), getEditorComponent());
    }

    private JPopupMenu getConnectionPopup() {
        return Utilities.actionsToPopup(new Action[]{deleteAction}, getEditorComponent());
    }

    private JPopupMenu getPinPopup(PinWidget widget) {
        PinID<String> pin = (PinID<String>) scene.findObject(widget);
        boolean enabled = (container.getInfo().controls().contains("ports"));
        Action action = new AddPortToParentAction(this, pin);
        action.setEnabled(enabled);
        return Utilities.actionsToPopup(new Action[]{action}, getEditorComponent());
    }

    private JPopupMenu getScenePopup() {
        List<Action> actions = new ArrayList<>();
        actions.add(addAction);
        actions.add(pasteAction);
        actions.add(null);
        Action[] containerActions = container.getNodeDelegate().getActions(true);
        if (containerActions.length != 0) {
            actions.addAll(Arrays.asList(containerActions));
            actions.add(null);
        }

        if (sharedCodeAction != null) {
            actions.add(sharedCodeAction);
            actions.add(null);
        }

        actions.add(new PropertyModeAction());
        actions.add(new CommentAction(scene));
        return Utilities.actionsToPopup(actions.toArray(Action[]::new), getEditorComponent());

    }

    PraxisGraphScene<String> getScene() {
        return scene;
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

    ExplorerManager getExplorerManager() {
        return manager;
    }

    @Override
    public void componentActivated() {
        requestFocus();
    }

    @Override
    public void dispose() {
        manager.removePropertyChangeListener(selectionListener);
        Disposable.dispose(addAction);
        Disposable.dispose(copyAction);
        Disposable.dispose(duplicateAction);
        Disposable.dispose(exportAction);
        Disposable.dispose(pasteAction);
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
            panel.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    scene.getView().requestFocusInWindow();
                }

            });
            panel.add(layered, BorderLayout.CENTER);

            if (sharedCodeAction != null) {
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
            ActionMap am = buildActionMap(panel.getActionMap());

            im.put(KeyStroke.getKeyStroke("alt shift F"), "format");
            am.put("format", new AbstractAction("format") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    scene.layoutScene();
                }
            });
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "escape");
            am.put("escape", goUpAction);

            panel.setActionMap(am);

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
    public List<Action> getActions() {
        return List.of(goUpAction, location);
    }

    @Override
    public boolean requestFocus() {
        if (panel == null) {
            return false;
        }
        return scene.getView().requestFocusInWindow();
    }

    @Override
    public Set<ToolAction> supportedToolActions() {
        return EnumSet.allOf(ToolAction.class);
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
        exposedTools.clear();
        knownConnections.clear();
        location.address.setText("");
    }

    private void buildScene() {
        container.addPropertyChangeListener(containerListener);
        manager.setExploredContext(container.getNodeDelegate());
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
        CommentAction commentAction = new CommentAction(widget);
        widget.setCommentEditProvider(w
                -> commentAction.actionPerformed(new ActionEvent(w, ActionEvent.ACTION_PERFORMED, "edit")));

        ComponentInfo info = cmp.getInfo();
        for (String portID : info.ports()) {
            PortInfo pi = info.portInfo(portID);
            buildPin(id, cmp, portID, pi);
        }
        cmp.addPropertyChangeListener(componentListener);
        syncConnections();
        configureExposedTools(widget, cmp);
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
        Widget w = scene.findWidget(cmp.getAddress().componentID());
        if (w instanceof NodeWidget node) {
            configureWidgetFromAttributes(node, cmp);
            configureExposedTools(node, cmp);
        }
    }

    private void removeChild(String id, ComponentProxy cmp) {
        cmp.removePropertyChangeListener(componentListener);
        scene.removeNodeWithEdges(id);
        // @TODO temporary fix for moving dynamic components?
        activePoint.x = 0;
        activePoint.y = 0;
    }

    private void configureExposedTools(NodeWidget widget, ComponentProxy cmp) {
        String id = cmp.getID();
        String key = "expose";
        PArray expose = Utils.getAttrValue(cmp, PArray.class, key);
        if (expose == null) {
            expose = Optional.ofNullable(cmp.getInfo().properties().get(key))
                    .flatMap(PArray::from)
                    .orElse(PArray.EMPTY);
        }
        PArray known = exposedTools.getOrDefault(id, PArray.EMPTY);
        if (Objects.equals(expose, known)) {
            return;
        }
        exposedTools.put(key, expose);
        widget.clearToolWidgets();
        ComponentInfo info = cmp.getInfo();
        List<String> controls = expose.stream()
                .map(Value::toString)
                .filter(c -> info.controls().contains(c))
                .toList();
        if (!controls.isEmpty()) {
            widget.addToolWidget(new ExposedControls(scene, cmp, controls));
        }

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
        Task task = ActionSupport.createImportTask(this, container, file);
        task.addPropertyChangeListener(e -> {
            if (task.getState() == Task.State.ERROR) {
                List<String> log = task.log();
                String msg;
                if (log.isEmpty()) {
                    msg = "Import error";
                } else {
                    msg = log.stream().collect(Collectors.joining("\n"));
                    DialogDisplayer.getDefault().notifyLater(
                            new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE));
                }
            }
        });
        task.execute();
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
                } else if (ComponentProtocol.META.equals(evt.getPropertyName())) {
                    String comment = Utils.getAttr(container, ATTR_GRAPH_COMMENT);
                    comment = comment == null ? "" : comment;
                    if (!comment.equals(scene.getComment())) {
                        scene.setComment(comment);
                        scene.validate();
                    }
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
                            configureExposedTools(node, cmp);
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

    private class SelectionListener extends ObjectSceneAdaptor implements PropertyChangeListener {

        private boolean ignoreChanges;

        @Override
        public void selectionChanged(ObjectSceneEvent event, Set<Object> previousSelection, Set<Object> newSelection) {
            for (Object obj : previousSelection) {
                if (newSelection.contains(obj)) {
                    continue;
                }
                if (obj instanceof String id) {
                    ComponentProxy cmp = container.getChild(id);
                    if (cmp != null) {
                        syncAttributes(cmp);
                    }
                }
            }

            if (ignoreChanges) {
                return;
            }

            try {
                ignoreChanges = true;
                if (newSelection.isEmpty()) {
                    if (container != null) {
                        manager.setSelectedNodes(new Node[]{container.getNodeDelegate()});
                    } else {
                        manager.setSelectedNodes(new Node[]{manager.getRootContext()});
                    }
                    deleteAction.setEnabled(false);
                } else {
                    ArrayList<Node> sel = new ArrayList<>();
                    for (Object obj : newSelection) {
                        if (obj instanceof String id) {
                            ComponentProxy cmp = container.getChild(id);
                            if (cmp != null) {
                                sel.add(cmp.getNodeDelegate());
                                syncAttributes(cmp);
                            }
                        }
                    }
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
                ignoreChanges = false;
            }

        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ignoreChanges) {
                return;
            }
            try {
                ignoreChanges = true;
                Node context = manager.getExploredContext();
                Node[] selection = manager.getSelectedNodes();
                ContainerProxy container = context.getLookup().lookup(ContainerProxy.class);
                if (GraphEditor.this.container != container) {
                    if (GraphEditor.this.container != null) {
                        clearScene();
                    }
                    GraphEditor.this.container = container;
                    if (container == null) {
                        return;
                    }
                    buildScene();
                }
                Set<String> selectedChildren = Stream.of(selection)
                        .map(n -> n.getLookup().lookup(ComponentProxy.class))
                        .filter(c -> c != null && c != container)
                        .map(c -> c.getID())
                        .filter(id -> id != null)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                scene.userSelectionSuggested(selectedChildren, false);
                if (!selectedChildren.isEmpty()) {
                    scene.setFocusedObject(selectedChildren.iterator().next());
                } else {
                    scene.setFocusedObject(null);
                }
                deleteAction.setEnabled(!selectedChildren.isEmpty());
            } finally {
                ignoreChanges = false;
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
            List<String> children = sel.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
            List<Connection> connections = sel.stream()
                    .filter(EdgeID.class::isInstance)
                    .map(o -> {
                        EdgeID<?> edge = (EdgeID) o;
                        PinID<?> p1 = edge.getPin1();
                        PinID<?> p2 = edge.getPin2();
                        return Connection.of(
                                p1.getParent().toString(), p1.getName(),
                                p2.getParent().toString(), p2.getName());
                    })
                    .toList();
            Task.run(ActionSupport.createDeleteTask(GraphEditor.this, container, children, connections));
        }

    }

    private class PropertyModeAction extends AbstractAction implements Presenter.Popup {

        private final PropertyMode mode;

        private PropertyModeAction() {
            this(Bundle.LBL_PropertyModeAction(), null);
        }

        private PropertyModeAction(String text, PropertyMode mode) {
            super(text);
            this.mode = mode;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (container == null || mode == null || propertyMode == mode) {
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

        @Override
        public JMenuItem getPopupPresenter() {
            JMenu submenu = new JMenu(Bundle.LBL_PropertyModeAction());
            submenu.add(new JRadioButtonMenuItem(
                    new PropertyModeAction(
                            Bundle.LBL_PropertyModeDefault(),
                            PropertyMode.Default)));
            submenu.add(new JRadioButtonMenuItem(
                    new PropertyModeAction(
                            Bundle.LBL_PropertyModeShowAll(),
                            PropertyMode.Show)));
            submenu.add(new JRadioButtonMenuItem(
                    new PropertyModeAction(
                            Bundle.LBL_PropertyModeHideAll(),
                            PropertyMode.Hide)));
            return submenu;
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
