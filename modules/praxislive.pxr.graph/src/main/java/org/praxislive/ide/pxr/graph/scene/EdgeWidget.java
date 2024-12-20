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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Objects;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Scene;

/**
 * A connection (edge) widget within a {@link PraxisGraphScene}.
 */
public class EdgeWidget extends ConnectionWidget {

    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;

    PinWidget srcPin;
    PinWidget dstPin;

    private LAFScheme scheme;
    private LAFScheme.Colors schemeColors;

    /**
     * Creates a connection widget.
     *
     * @param scene the scene
     * @param srcPin source pin
     * @param dstPin destination pin
     */
    EdgeWidget(PraxisGraphScene scene, PinWidget srcPin, PinWidget dstPin) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();
        this.srcPin = Objects.requireNonNull(srcPin);
        this.dstPin = Objects.requireNonNull(dstPin);
        scheme.installUI(this);
    }

    EdgeWidget(PraxisGraphScene scene) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();
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

    void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }

    LAFScheme.Colors getSchemeColors() {
        if (schemeColors == null) {
            if (srcPin == null) {
                return scene.getSchemeColors();
            } else {
                return srcPin.getSchemeColors();
            }
        } else {
            return schemeColors;
        }
    }

    /**
     * Implements the widget-state specific look of the widget.
     *
     * @param previousState the previous state
     * @param state the new state
     */
    @Override
    public void notifyStateChanged(ObjectState previousState, ObjectState state) {
        if (!previousState.isSelected() && state.isSelected()) {
            bringToFront();
        } else if (!previousState.isHovered() && state.isHovered()) {
            bringToFront();
        }
        scheme.updateUI(this);
    }

    @Override
    protected Rectangle calculateClientArea() {
        if (scene.isOrthogonalRouting()) {
            return super.calculateClientArea();
        }
        GeneralPath path = generatePath(getControlPoints());
        if (path == null) {
            return new Rectangle();
        } else {
            Rectangle bounds = generatePath(getControlPoints()).getBounds();
            bounds.grow(4, 4);
            return bounds;
        }

    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (scene.isOrthogonalRouting()) {
            return super.isHitAt(localLocation);
        }
        if (!isVisible()) {
            return false;
        }
        GeneralPath path = generatePath(getControlPoints());
        Rectangle localArea = new Rectangle(localLocation.x - 3, localLocation.y - 3, 6, 6);
        return path.intersects(localArea);
    }

    @Override
    protected void paintWidget() {
        if (scene.isOrthogonalRouting()) {
            super.paintWidget();
            return;
        }
        Graphics2D gr = getGraphics();
        gr.setColor(getForeground());

        List<Point> points = getControlPoints();
        GeneralPath path = generatePath(points);

//        for (Point point : points) {
//            path = addToPath(path, point.x, point.y);
//        }
        if (path != null) {
            Stroke previousStroke = gr.getStroke();
            gr.setPaint(getForeground());
            gr.setStroke(getStroke());
            gr.draw(path);
            gr.setStroke(previousStroke);
        }

        AffineTransform previousTransform;

        int last = points.size() - 1;
        for (int index = 0; index <= last; index++) {
            Point point = points.get(index);
            previousTransform = gr.getTransform();
            gr.translate(point.x, point.y);
            if (index == 0 || index == last) {
                getEndPointShape().paint(gr);
            } else {
                getControlPointShape().paint(gr);
            }
            gr.setTransform(previousTransform);
        }
    }

    private GeneralPath generatePath(List<Point> points) {
        if (points.isEmpty()) {
            return null;
        }
        boolean sourceRight = getSourceAnchor()
                .compute(getSourceAnchorEntry()).getDirections()
                .contains(Anchor.Direction.RIGHT);
//        boolean targetRight = getTargetAnchor()
//                .compute(getTargetAnchorEntry()).getDirections()
//                .contains(Anchor.Direction.RIGHT);
        boolean targetRight = !sourceRight;
        GeneralPath path = new GeneralPath();
        Point sourcePoint = points.get(0);
        Point targetPoint = points.get(points.size() - 1);
        path.moveTo(sourcePoint.x, sourcePoint.y);
        double diffX = Math.abs(sourcePoint.x - targetPoint.x);
        diffX = Math.min(diffX, 200);
        diffX = Math.max(diffX, 20);
        double diffY = Math.abs(sourcePoint.y - targetPoint.y) * 0.1;
        diffY = Math.min(diffY, 10);
        path.curveTo(sourcePoint.x + (sourceRight ? diffX : -diffX), sourcePoint.y - diffY,
                targetPoint.x + (targetRight ? diffX : -diffX), targetPoint.y + diffY,
                targetPoint.x, targetPoint.y);

        return path;
    }

    private class SceneListenerImpl implements Scene.SceneListener {

        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {
            scheme.updateUI(EdgeWidget.this);
        }

        @Override
        public void sceneValidated() {
        }

    }

}
