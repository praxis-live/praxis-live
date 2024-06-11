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

import java.awt.Point;
import java.awt.Rectangle;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;

import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.widget.Scene;

/**
 * A pin widget within a {@link PraxisGraphScene}.
 */
public class PinWidget extends Widget {

    public final static String DEFAULT_CATEGORY = "";

    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;
    private final NodeWidget node;
    private final LabelWidget nameWidget;

    private LAFScheme scheme;
    private LAFScheme.Colors schemeColors;
    private Alignment alignment;
    private String category;

    /**
     * Creates a pin widget with a specific name.
     *
     * @param scene the scene
     * @param name pin ID
     */
    public PinWidget(PraxisGraphScene scene, NodeWidget node, String name) {
        super(scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.node = node;
        this.scheme = scene.getLookAndFeel();
        this.schemeColors = node.getSchemeColors();
        this.alignment = Alignment.Center;
        this.category = DEFAULT_CATEGORY;
//        setLayout(LayoutFactory.createOverlayLayout());
        setLayout(LayoutFactory.createVerticalFlowLayout());
        addChild(nameWidget = new LabelWidget(scene));
//        nameWidget.setForeground(Color.BLACK);
        nameWidget.setLabel(name);
        nameWidget.setAlignment(LabelWidget.Alignment.CENTER);
        scheme.installUI(this);
    }

    /**
     * Called to notify about the change of the widget state.
     *
     * @param previousState the previous state
     * @param state the new state
     */
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        scheme.updateUI(this);
    }

    @Override
    protected void notifyAdded() {
        scene.addSceneListener(sceneListener);
    }

    @Override
    protected void notifyRemoved() {
        scene.removeSceneListener(sceneListener);
    }

    /**
     * Returns a pin name widget.
     *
     * @return the pin name widget
     */
    public Widget getPinNameWidget() {
        return nameWidget;
    }

    /**
     * Returns a pin name.
     *
     * @return the pin name
     */
    public String getName() {
        return nameWidget.getLabel();
    }

    public void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }

    public LAFScheme.Colors getSchemeColors() {
        return schemeColors == null ? node.getSchemeColors() : schemeColors;
    }

    public void setAlignment(Alignment alignment) {
        if (alignment == null) {
            throw new NullPointerException();
        }
        nameWidget.setAlignment(getLabelAlignment(alignment));
        this.alignment = alignment;
        scheme.updateUI(this);
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    private LabelWidget.Alignment getLabelAlignment(Alignment alignment) {
        switch (alignment) {
            case Left:
                return LabelWidget.Alignment.LEFT;
            case Right:
                return LabelWidget.Alignment.RIGHT;
            default:
                return LabelWidget.Alignment.CENTER;
        }
    }

    public Anchor createAnchor() {
        return new AlignedAnchor();
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (scene.isBelowLODThreshold()) {
            return false;
        } else {
            return super.isHitAt(localLocation);
        }
    }

    private class AlignedAnchor extends Anchor {

        private AlignedAnchor() {
            super(PinWidget.this);
        }

        @Override
        public Result compute(Entry entry) {
            Rectangle bounds = convertLocalToScene(getBounds());
            int centerY = bounds.y + bounds.height / 2;
            int gap = scheme.getAnchorGap();
            boolean right;
            switch (alignment) {
                case Left:
                    right = false;
                    break;
                case Right:
                    right = true;
                    break;
                default:
                    Point opposite = getOppositeSceneLocation(entry);
                    if (opposite.x > bounds.x) {
                        right = true;
                    } else {
                        right = false;
                    }

            }
            if (right) {
                return new Anchor.Result(
                        new Point(bounds.x + bounds.width + gap, centerY),
                        Direction.RIGHT);
            } else {
                return new Anchor.Result(
                        new Point(bounds.x - gap, centerY),
                        Direction.LEFT);
            }
        }
    }

    private class SceneListenerImpl implements Scene.SceneListener {

        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {
            scheme.updateUI(PinWidget.this);
        }

        @Override
        public void sceneValidated() {
        }

    }

}
