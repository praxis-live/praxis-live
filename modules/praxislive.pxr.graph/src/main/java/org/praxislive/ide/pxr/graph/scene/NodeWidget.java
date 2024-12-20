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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;
import org.netbeans.api.visual.action.EditProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.model.StateModel;
import org.netbeans.api.visual.widget.ImageWidget;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 * Node widget within a {@link PraxisGraphScene}.
 */
public class NodeWidget extends Widget implements StateModel.Listener, MinimizeAbility {

    private final Widget header;
    private final ImageWidget minimizeWidget;
    private final ImageWidget imageWidget;
    private final LabelWidget nameWidget;
    private final GlyphSetWidget glyphSetWidget;

    private final StateModel stateModel;
    private final LAFScheme scheme;
    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;
    private final CommentWidget commentWidget;
    private final ToolContainerWidget toolsWidget;

    private LAFScheme.Colors schemeColors;

    /**
     * Creates a node widget with a specific color scheme.
     *
     * @param scene the scene
     */
    NodeWidget(PraxisGraphScene<?> scene) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();

        stateModel = new StateModel();

        setLayout(LayoutFactory.createVerticalFlowLayout());
        setMinimumSize(new Dimension(100, 10));

        header = new Widget(scene);
        header.setLayout(LayoutFactory.createHorizontalFlowLayout(LayoutFactory.SerialAlignment.CENTER, 8));
        addChild(header);

        minimizeWidget = new ImageWidget(scene, scheme.getMinimizeWidgetImage(this));
        minimizeWidget.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minimizeWidget.getActions().addAction(new ToggleMinimizedAction());

        header.addChild(minimizeWidget);

        imageWidget = new ImageWidget(scene);
        header.addChild(imageWidget);

        nameWidget = new LabelWidget(scene);
        nameWidget.setForeground(Color.BLACK);
        header.addChild(nameWidget);

        glyphSetWidget = new GlyphSetWidget(scene);
        glyphSetWidget.setMinimumSize(new Dimension(16, 16));
        header.addChild(glyphSetWidget);

        stateModel.addListener(this);

        commentWidget = new CommentWidget(scene);
        commentWidget.setVisible(false);
        toolsWidget = new ToolContainerWidget(scene);
        toolsWidget.setVisible(false);

        scheme.installUI(this);

    }

    @Override
    protected void notifyAdded() {
        scene.addSceneListener(sceneListener);
    }

    @Override
    protected void notifyRemoved() {
        scene.removeSceneListener(sceneListener);
    }

    private boolean isMinimizableWidget(Widget widget) {
        if (scene.isMinimizeConnectedPins()) {
            return true;
        } else {
            if (widget instanceof PinWidget pin) {
                return !scene.hasConnections(pin);
            } else {
                return true;
            }
        }
    }

    /**
     * Check the minimized state.
     *
     * @return true, if minimized
     */
    public boolean isMinimized() {
        return stateModel.getBooleanState();
    }

    /**
     * Set the minimized state. This method will show/hide child widgets of this
     * Widget and switches anchors between node and pin widgets.
     *
     * @param minimized if true, then the widget is going to be minimized
     */
    public void setMinimized(boolean minimized) {
        stateModel.setBooleanState(minimized);
    }

    /**
     * Toggles the minimized state. This method will show/hide child widgets of
     * this Widget and switches anchors between node and pin widgets.
     */
    public void toggleMinimized() {
        stateModel.toggleBooleanState();
    }

    /**
     * Called when a minimized state is changed. This method will show/hide
     * child widgets of this Widget and switches anchors between node and pin
     * widgets.
     */
    @Override
    public void stateChanged() {
        boolean minimized = stateModel.getBooleanState();
        Rectangle rectangle = minimized ? new Rectangle() : null;
        for (Widget widget : getChildren()) {
            if (widget != header) {
                getScene().getSceneAnimator().animatePreferredBounds(widget, minimized && isMinimizableWidget(widget) ? rectangle : null);
            }
        }
        minimizeWidget.setImage(scheme.getMinimizeWidgetImage(this));
    }

    /**
     * Called to notify about the change of the widget state.
     *
     * @param previousState the previous state
     * @param state the new state
     */
    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        if ((!previousState.isSelected() && state.isSelected())
                || (!previousState.isHovered() && state.isHovered())) {
            bringToFront();
            commentWidget.bringToFront();
            toolsWidget.bringToFront();
        }
        scheme.updateUI(this);
    }

    /**
     * Sets a node image.
     *
     * @param image the image
     */
    public void setNodeImage(Image image) {
        imageWidget.setImage(image);
        revalidate();
    }

    /**
     * Returns a node name.
     *
     * @return the node name
     */
    public String getNodeName() {
        return nameWidget.getLabel();
    }

    /**
     * Sets a node name.
     *
     * @param nodeName the node name
     */
    public void setNodeName(String nodeName) {
        nameWidget.setLabel(nodeName);
    }

    /**
     * Attaches a pin widget to the node widget.
     *
     * @param widget the pin widget
     */
    public void attachPinWidget(Widget widget) {
        widget.setCheckClipping(true);
        addChild(widget);
        if (stateModel.getBooleanState() && isMinimizableWidget(widget)) {
            widget.setPreferredBounds(new Rectangle());
        }
    }

    /**
     * Sets node glyphs.
     *
     * @param glyphs the list of images
     */
    public void setGlyphs(List<Image> glyphs) {
        glyphSetWidget.setGlyphs(glyphs);
    }

    /**
     * Returns a node name widget.
     *
     * @return the node name widget
     */
    public LabelWidget getNodeNameWidget() {
        return nameWidget;
    }

    /**
     * Collapses the widget.
     */
    @Override
    public void collapseWidget() {
        stateModel.setBooleanState(true);
    }

    /**
     * Expands the widget.
     */
    @Override
    public void expandWidget() {
        stateModel.setBooleanState(false);
    }

    /**
     * Returns a header widget.
     *
     * @return the header widget
     */
    public Widget getHeader() {
        return header;
    }

    /**
     * Returns a minimize button widget.
     *
     * @return the minimize button widget
     */
    public Widget getMinimizeButton() {
        return minimizeWidget;
    }

    /**
     * Set the widget comment. If null or empty the comment will be removed.
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
        scheme.updateUI(this);
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
     * Set an edit provider for the node comment.
     *
     * @param provider comment edit provider
     */
    public void setCommentEditProvider(EditProvider provider) {
        commentWidget.setEditProvider(provider);
    }

    /**
     * Add a widget to the the node tools container.
     *
     * @param tool tool widget
     */
    public void addToolWidget(Widget tool) {
        toolsWidget.addChild(tool);
        toolsWidget.setVisible(true);
    }

    /**
     * Remove a widget from the tools container.
     *
     * @param tool tool widget
     */
    public void removeToolWidget(Widget tool) {
        toolsWidget.removeChild(tool);
        if (toolsWidget.getChildren().isEmpty()) {
            toolsWidget.setVisible(false);
        }
    }

    /**
     * Clear all widgets from the tools container.
     */
    public void clearToolWidgets() {
        toolsWidget.removeChildren();
        toolsWidget.setVisible(false);
    }

    private void positionComment() {
        if (!commentWidget.isVisible()) {
            return;
        }
        Point loc = getLocation();
        Rectangle bounds = getBounds();
        Rectangle commentBounds = commentWidget.getBounds();
        if (loc == null || bounds == null || commentBounds == null) {
            return;
        }
        int offset = commentWidget.getBorder().getInsets().left;
        commentWidget.setPreferredLocation(new Point(loc.x + offset, loc.y - commentBounds.height - 2));
        commentWidget.setMinimumSize(new Dimension(bounds.width, 15));
    }

    private void positionTools() {
        if (!toolsWidget.isVisible()) {
            return;
        }
        Point loc = getLocation();
        Rectangle bounds = getBounds();
        if (loc == null || bounds == null) {
            return;
        }
        toolsWidget.setPreferredLocation(new Point(loc.x, loc.y + bounds.height + 2));
        toolsWidget.setMinimumSize(new Dimension(bounds.width, 15));
    }

    @Override
    protected void paintChildren() {
        if (isBelowLODThreshold()) {
            return;
        }
        super.paintChildren();
    }

    public boolean isBelowLODThreshold() {
        return scene.isBelowLODThreshold();
    }

    CommentWidget getCommentWidget() {
        return commentWidget;
    }

    ToolContainerWidget getToolContainerWidget() {
        return toolsWidget;
    }

    public void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }

    public LAFScheme.Colors getSchemeColors() {
        return schemeColors == null ? scene.getSchemeColors() : schemeColors;
    }

    private class SceneListenerImpl implements Scene.SceneListener {

        @Override
        public void sceneRepaint() {
            // no op
        }

        @Override
        public void sceneValidating() {
            scheme.updateUI(NodeWidget.this);
        }

        @Override
        public void sceneValidated() {
            positionComment();
            positionTools();
        }

    }

    private class ToggleMinimizedAction extends WidgetAction.Adapter {

        @Override
        public State mousePressed(Widget widget, WidgetMouseEvent event) {
            if (event.getButton() == MouseEvent.BUTTON1 || event.getButton() == MouseEvent.BUTTON2) {
                stateModel.toggleBooleanState();
//                return State.CONSUMED; // temporary fix - minimized state saved on de-selection
            }
            return State.REJECTED;
        }
    }

}
