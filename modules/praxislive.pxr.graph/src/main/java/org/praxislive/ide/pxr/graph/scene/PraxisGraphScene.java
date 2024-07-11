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
 *
 *
 * This file incorporates code from Apache NetBeans Visual Library, covered by
 * the following terms :
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.praxislive.ide.pxr.graph.scene;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.UIManager;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.graph.GraphPinScene;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.router.ConnectionWidgetCollisionsCollector;
import org.netbeans.api.visual.router.Router;
import org.netbeans.api.visual.router.RouterFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.EventProcessingType;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 * A graph scene providing node widgets with pins and edges. The graph scene is
 * backed by a set of nodes of the given type. A graph cannot contain duplicate
 * nodes.
 *
 * @param <N> node type
 */
public class PraxisGraphScene<N> extends GraphPinScene<N, EdgeID<N>, PinID<N>> {

    private final static double LOD_ZOOM = 0.7;

    private final LayerWidget backgroundLayer = new LayerWidget(this);
    private final LayerWidget mainLayer = new LayerWidget(this);
    private final LayerWidget connectionLayer = new LayerWidget(this);
    private final LayerWidget upperLayer = new LayerWidget(this);

    private final CommentWidget commentWidget;

    private boolean orthogonal;
    private Router router;
    private final WidgetAction moveAction;
    private final PraxisKeyboardMoveAction keyboardMoveAction;
    private final SceneLayout sceneLayout;
    private LAFScheme scheme;
    private WidgetAction menuAction;
    private WidgetAction connectAction;
    private LAFScheme.Colors schemeColors;

//    private int edgeCount = 10;
    /**
     * Create a Praxis graph scene.
     */
    public PraxisGraphScene() {
        this(null, null, null);
    }

    /**
     * Create a Praxis graph scene with a specific look and feel scheme.
     *
     * @param scheme the look and feel scheme
     */
    public PraxisGraphScene(LAFScheme scheme) {
        this(scheme, null, null);
    }

    /**
     * Create a Praxis graph scene with the provided connect and popup menu
     * providers.
     *
     * @param connectProvider connect provider
     * @param popupProvider popup menu provider
     */
    public PraxisGraphScene(ConnectProvider connectProvider, PopupMenuProvider popupProvider) {
        this(null, connectProvider, popupProvider);
    }

    /**
     * Create a Praxis graph scene with a specific look and feel scheme, and the
     * provided connect and popup menu providers.
     *
     * @param scheme the look and feel scheme
     * @param connectProvider connect provider
     * @param popupProvider popup menu provider
     */
    public PraxisGraphScene(LAFScheme scheme, ConnectProvider connectProvider, PopupMenuProvider popupProvider) {
        if (scheme == null) {
            scheme = new LAFScheme();
        }
        this.scheme = scheme;

        setFont(UIManager.getFont("controlFont"));

        setKeyEventProcessingType(EventProcessingType.FOCUSED_WIDGET_AND_ITS_PARENTS);

        addChild(backgroundLayer);
        addChild(mainLayer);
        addChild(connectionLayer);
        addChild(upperLayer);

        PraxisMoveProvider mover = new PraxisMoveProvider(this, backgroundLayer);
        moveAction = ActionFactory.createMoveAction(mover, mover);
        keyboardMoveAction = new PraxisKeyboardMoveAction(mover, mover);

        commentWidget = new CommentWidget(this);
        commentWidget.setPreferredLocation(new Point(32, 32));
        commentWidget.setBorder(BorderFactory.createRoundedBorder(8, 8, 8, 8, Color.LIGHT_GRAY, null));
        commentWidget.setVisible(false);
        mainLayer.addChild(commentWidget);

        setBackground(scheme.getBackgroundColor());

        router = RouterFactory.createDirectRouter();

        getActions().addAction(ActionFactory.createWheelPanAction());
        getActions().addAction(ActionFactory.createMouseCenteredZoomAction(1.2));
        getActions().addAction(ActionFactory.createPanAction());
        getActions().addAction(ActionFactory.createCycleFocusAction(new PraxisCycleFocusProvider()));

        if (connectProvider != null) {
            connectAction = ActionFactory.createConnectAction(new PraxisConnectDecorator(), connectionLayer, connectProvider);
        }
        if (popupProvider != null) {
            menuAction = ActionFactory.createPopupMenuAction(popupProvider);
            getActions().addAction(menuAction);
        }

        getActions().addAction(ActionFactory.createRectangularSelectAction(this, backgroundLayer));

        addSceneListener(new ZoomCorrector());

        sceneLayout = LayoutFactory.createSceneGraphLayout(this, new PraxisGraphLayout<>(true));

    }

    /**
     * Add a node with the given name. Returns the node widget for further
     * customization.
     *
     * @param node node
     * @param name name of node
     * @return node widget representation
     */
    public NodeWidget addNode(N node, String name) {
        NodeWidget n = (NodeWidget) super.addNode(node);
        n.setNodeName(name);
        return n;
    }

    @Override
    protected void detachNodeWidget(N node, Widget widget) {
        ((NodeWidget) widget).getCommentWidget().removeFromParent();
        super.detachNodeWidget(node, widget);
    }

    /**
     * Add a pin with the given name to a node. The pin will be centrally
     * aligned. Returns the pin widget attached to the node widget for further
     * customization.
     *
     * @param node node to add pin to
     * @param name name of pin
     * @return pin widget representation
     */
    public PinWidget addPin(N node, String name) {
        return addPin(new PinID<>(node, name),
                Alignment.Center);
    }

    /**
     * Add a pin with the given name and alignment to a node. Returns the pin
     * widget attached to the node widget for further customization.
     *
     * @param node node to add pin to
     * @param name name of pin
     * @param alignment pin alignment
     * @return pin widget representation
     */
    public PinWidget addPin(N node, String name, Alignment alignment) {
        return addPin(new PinID<>(node, name), alignment);
    }

    /**
     * Add a pin with the given ID and alignment. Returns the pin widget for
     * further customization.
     *
     * @param pin pin ID
     * @param alignment pin alignment
     * @return pin widget representation
     */
    public PinWidget addPin(PinID<N> pin, Alignment alignment) {
        if (pin == null || alignment == null) {
            throw new NullPointerException();
        }
        PinWidget p = (PinWidget) super.addPin(pin.getParent(), pin);
        p.setAlignment(alignment);
        return p;
    }

    /**
     * Add a connection between the given pins. Returns the edge widget for
     * further customization.
     *
     * @param node1 first node
     * @param pin1 first pin name
     * @param node2 second node
     * @param pin2 second pin name
     * @return edge widget representation
     */
    public EdgeWidget connect(N node1, String pin1, N node2, String pin2) {
        return connect(new PinID<>(node1, pin1),
                new PinID<>(node2, pin2));
    }

    /**
     * Add a connection between the given pins. Returns the edge widget for
     * further customization.
     *
     * @param p1 first pin ID
     * @param p2 second pin ID
     * @return edge widget representation
     */
    public EdgeWidget connect(PinID<N> p1, PinID<N> p2) {
        EdgeID<N> d = new EdgeID<>(p1, p2);
        EdgeWidget e = (EdgeWidget) addEdge(d);
        setEdgeSource(d, p1);
        setEdgeTarget(d, p2);
        return e;
    }

    /**
     * Disconnect the given pins.
     *
     * @param node1 first node
     * @param pin1 first pin name
     * @param node2 second node
     * @param pin2 second pin name
     */
    public void disconnect(N node1, String pin1, N node2, String pin2) {
        PinID<N> p1 = new PinID<>(node1, pin1);
        PinID<N> p2 = new PinID<>(node2, pin2);
        EdgeID<N> d = new EdgeID<>(p1, p2);
        removeEdge(d);
    }

    /**
     * Get the look and feel scheme.
     *
     * @return LAF scheme
     */
    public LAFScheme getLookAndFeel() {
        return scheme;
    }

    /**
     * Set the scheme colours.
     *
     * @param schemeColors scheme colours
     */
    public void setSchemeColors(LAFScheme.Colors schemeColors) {
        this.schemeColors = schemeColors;
        revalidate();
    }

    /**
     * Get the scheme colours.
     *
     * @return scheme colours
     */
    public LAFScheme.Colors getSchemeColors() {
        return schemeColors;
    }

    /**
     * Set whether to use orthogonal routing (as opposed to curved edges).
     *
     * @param orthogonal use orthogonal routing
     */
    public void setOrthogonalRouting(boolean orthogonal) {
        if (this.orthogonal != orthogonal) {
            this.orthogonal = orthogonal;
            setRouter(orthogonal
                    ? RouterFactory.createOrthogonalSearchRouter(mainLayer, upperLayer)
                    : RouterFactory.createDirectRouter());
        }
    }

    /**
     * Query whether the graph is using orthogonal routing.
     *
     * @return using orthogonal routing
     */
    public boolean isOrthogonalRouting() {
        return orthogonal;
    }

    void setRouter(Router router) {
        this.router = router;
        for (EdgeID<N> e : getEdges()) {
            ((ConnectionWidget) findWidget(e)).setRouter(router);
        }
        revalidate();
    }

    Router getRouter() {
        return router;
    }

    @Override
    public void userSelectionSuggested(Set<?> suggestedSelectedObjects, boolean invertSelection) {

        if (suggestedSelectedObjects.size() == 1 && isPin(suggestedSelectedObjects.iterator().next())) {
            suggestedSelectedObjects = Collections.emptySet();
        } else if (!suggestedSelectedObjects.isEmpty()) {
            Set<Object> selection = new LinkedHashSet<Object>(suggestedSelectedObjects.size());
            for (Object obj : suggestedSelectedObjects) {
                if (isPin(obj)) {
                    continue;
                }
                selection.add(obj);
            }
            suggestedSelectedObjects = selection;
        }
        super.userSelectionSuggested(suggestedSelectedObjects, invertSelection);
    }

    /**
     * Implements attaching a widget to a node. The widget is NodeWidget and has
     * object-hover, select, popup-menu and move actions.
     *
     * @param node the node
     * @return the widget attached to the node
     */
    @Override
    protected Widget attachNodeWidget(N node) {
        NodeWidget widget = new NodeWidget(this);
        mainLayer.addChild(widget);

        widget.getHeader().getActions().addAction(createObjectHoverAction());
        widget.getActions().addAction(createSelectAction());
        widget.getActions().addAction(moveAction);
        widget.getActions().addAction(keyboardMoveAction);
        if (menuAction != null) {
            widget.getActions().addAction(menuAction);
        }
        return widget;
    }

    /**
     * Implements attaching a widget to a pin.
     *
     * @param node the node
     * @param pin the pin
     * @return the widget attached to the pin
     */
    @Override
    protected Widget attachPinWidget(N node, PinID<N> pin) {
        NodeWidget nodeWidget = (NodeWidget) findWidget(node);
        PinWidget widget = new PinWidget(this, nodeWidget, pin.getName());
        nodeWidget.attachPinWidget(widget);
        widget.getActions().addAction(createObjectHoverAction());
        if (connectAction != null) {
            widget.getActions().addAction(connectAction);
        }
        if (menuAction != null) {
            widget.getActions().addAction(menuAction);
        }
        return widget;
    }

    /**
     * Implements attaching a widget to an edge.
     *
     * @param edge the edge
     * @return the widget attached to the edge
     */
    @Override
    protected Widget attachEdgeWidget(final EdgeID<N> edge) {
        PinWidget src = (PinWidget) findWidget(edge.getPin1());
        PinWidget dst = (PinWidget) findWidget(edge.getPin2());
        EdgeWidget edgeWidget = new EdgeWidget(this, src, dst);
        edgeWidget.setRouter(router);
        connectionLayer.addChild(edgeWidget);
        edgeWidget.getActions().addAction(createObjectHoverAction());
        edgeWidget.getActions().addAction(createSelectAction());
        if (menuAction != null) {
            edgeWidget.getActions().addAction(menuAction);
        }
        return edgeWidget;
    }

    /**
     * Attaches an anchor of a source pin an edge. The anchor is a ProxyAnchor
     * that switches between the anchor attached to the pin widget directly and
     * the anchor attached to the pin node widget based on the minimize-state of
     * the node.
     *
     * @param edge the edge
     * @param oldSourcePin the old source pin
     * @param sourcePin the new source pin
     */
    @Override
    protected void attachEdgeSourceAnchor(EdgeID<N> edge, PinID<N> oldSourcePin, PinID<N> sourcePin) {
        ((EdgeWidget) findWidget(edge)).setSourceAnchor(getPinAnchor(sourcePin));
    }

    /**
     * Attaches an anchor of a target pin an edge. The anchor is a ProxyAnchor
     * that switches between the anchor attached to the pin widget directly and
     * the anchor attached to the pin node widget based on the minimize-state of
     * the node.
     *
     * @param edge the edge
     * @param oldTargetPin the old target pin
     * @param targetPin the new target pin
     */
    @Override
    protected void attachEdgeTargetAnchor(EdgeID<N> edge, PinID<N> oldTargetPin, PinID<N> targetPin) {
        ((EdgeWidget) findWidget(edge)).setTargetAnchor(getPinAnchor(targetPin));
    }

    private Anchor getPinAnchor(PinID<N> pin) {
        if (pin == null) {
            return null;
        }
        PinWidget p = (PinWidget) findWidget(pin);
        return p.createAnchor();
    }

    /**
     * Query whether the graph is zoomed out below the level of detail
     * threshold. Pins and node names may not be visible below the threshold.
     *
     * @return below level of detail threshold
     */
    public boolean isBelowLODThreshold() {
        return getZoomFactor() < LOD_ZOOM;
    }

    /**
     * Set a comment text to be displayed on the graph. A null or empty comment
     * will remove the display.
     *
     * @param comment comment text
     */
    public void setComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            // remove comment
            commentWidget.setText("");
            commentWidget.setVisible(false);
        } else {
            // add comment
            commentWidget.setText(comment);
            commentWidget.setVisible(true);
        }
    }

    /**
     * Get the comment text.
     *
     * @return comment text
     */
    public String getComment() {
        return commentWidget.getText();
    }

    /**
     * Get the widget used to display any comment.
     *
     * @return comment widget
     */
    public Widget getCommentWidget() {
        return commentWidget;
    }

    public void layoutScene() {
        sceneLayout.invokeLayout();
    }

    private class ZoomCorrector implements SceneListener {

        private final double minZoom = 0.2;
        private final double maxZoom = 2;

        @Override
        public void sceneRepaint() {
            // no op
        }

        @Override
        public void sceneValidating() {
            double zoom = getZoomFactor();
            if (zoom < minZoom) {
                setZoomFactor(minZoom);
            } else if (zoom > maxZoom) {
                setZoomFactor(maxZoom);
            }
        }

        @Override
        public void sceneValidated() {
            // no op
        }

    }

    private static class WidgetCollector implements ConnectionWidgetCollisionsCollector {

        @Override
        public void collectCollisions(ConnectionWidget connectionWidget, List<Rectangle> verticalCollisions, List<Rectangle> horizontalCollisions) {
            // anchor widget is pin - get node.
            Widget w1 = connectionWidget.getSourceAnchor().getRelatedWidget().getParentWidget();
            Widget w2 = connectionWidget.getTargetAnchor().getRelatedWidget().getParentWidget();
            Rectangle rect;

            rect = w1.getBounds();
            rect = w1.convertLocalToScene(rect);
            rect.grow(10, 10);
            verticalCollisions.add(rect);
            horizontalCollisions.add(rect);

            rect = w2.getBounds();
            rect = w2.convertLocalToScene(rect);
            rect.grow(10, 10);
            verticalCollisions.add(rect);
            horizontalCollisions.add(rect);
        }
    }
}
