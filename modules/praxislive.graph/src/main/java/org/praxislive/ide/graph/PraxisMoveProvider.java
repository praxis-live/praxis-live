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
package org.praxislive.ide.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.AlignWithMoveDecorator;
import org.netbeans.api.visual.action.AlignWithWidgetCollector;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.MoveStrategy;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 */
class PraxisMoveProvider extends AlignWithSupport implements MoveProvider, MoveStrategy {

    private final Map<Widget, Point> locations;
    private final MoveProvider defaultProvider;
    private final PraxisGraphScene<?> scene;
    private final boolean outerBounds = false;

    PraxisMoveProvider(PraxisGraphScene<?> scene,
            LayerWidget interactionLayer) {
        super(new NodeCollector(scene),
                interactionLayer,
                new Decorator());
        this.scene = scene;
        locations = new HashMap<Widget, Point>();
        defaultProvider = ActionFactory.createDefaultMoveProvider();
    }

    @Override
    public void movementStarted(Widget widget) {
        show();
    }

    @Override
    public void movementFinished(Widget widget) {
        hide();
        locations.clear();
    }

    @Override
    public Point getOriginalLocation(Widget widget) {
        locations.put(widget, defaultProvider.getOriginalLocation(widget));
        for (Object obj : scene.getSelectedObjects()) {
            if (!scene.isNode(obj)) {
                continue;
            }
            Widget additional = scene.findWidget(obj);
            if (additional == widget) {
                continue;
            }
            locations.put(additional, defaultProvider.getOriginalLocation(additional));
        }
        return defaultProvider.getOriginalLocation(widget);
    }

    @Override
    public void setNewLocation(Widget widget, Point location) {
        defaultProvider.setNewLocation(widget, location);
        Point primary = locations.get(widget);
        if (primary == null || locations.size() == 1) {
            return;
        }
        int dx = location.x - primary.x;
        int dy = location.y - primary.y;
        for (Map.Entry<Widget, Point> loc : locations.entrySet()) {
            Widget additional = loc.getKey();
            if (additional == widget) {
                continue;
            }
            Point pt = new Point(loc.getValue());
            pt.translate(dx, dy);
            defaultProvider.setNewLocation(additional, pt);
        }
    }

    @Override
    public Point locationSuggested(Widget widget, Point originalLocation, Point suggestedLocation) {
        Point widgetLocation = widget.getLocation();
        Rectangle widgetBounds = outerBounds ? widget.getBounds() : widget.getClientArea();
        Rectangle bounds = widget.convertLocalToScene(widgetBounds);
        bounds.translate(suggestedLocation.x - widgetLocation.x, suggestedLocation.y - widgetLocation.y);
        Insets insets = widget.getBorder().getInsets();
        if (!outerBounds) {
            suggestedLocation.x += insets.left;
            suggestedLocation.y += insets.top;
        }
        Point point = super.locationSuggested(widget, bounds, widget.getParentWidget().convertLocalToScene(suggestedLocation), true, true, true, true);
        if (!outerBounds) {
            point.x -= insets.left;
            point.y -= insets.top;
        }
        return widget.getParentWidget().convertSceneToLocal(point);
    }

    private static class Decorator implements AlignWithMoveDecorator {

        private static final BasicStroke STROKE = new BasicStroke(1.0f, BasicStroke.JOIN_BEVEL,
                BasicStroke.CAP_BUTT, 5.0f, new float[]{2.0f, 2.0f}, 0.0f);
        private static final Color COLOR = new Color(127, 127, 127);

        @Override
        public ConnectionWidget createLineWidget(Scene scene) {
            ConnectionWidget widget = new ConnectionWidget(scene);
            widget.setStroke(STROKE);
            widget.setForeground(COLOR);
            return widget;
        }

    }

    static class NodeCollector implements AlignWithWidgetCollector {

        private final PraxisGraphScene<?> scene;

        public NodeCollector(PraxisGraphScene<?> scene) {
            this.scene = scene;
        }

        @Override
        public List<Rectangle> getRegions(Widget movingWidget) {
            List<Rectangle> regions = new ArrayList<>();
            List<Object> nodes = new ArrayList<>(scene.getNodes());
            nodes.removeAll(scene.getSelectedObjects());
            for (Object node : nodes) {
                Widget widget = scene.findWidget(node);
                if (widget != null && widget != movingWidget) {
                    regions.add(widget.convertLocalToScene(widget.getClientArea()));
                }
            }
            return regions;
        }
    }

}
