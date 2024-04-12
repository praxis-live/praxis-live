/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 *
 *
 * This class is derived from code in NetBeans Visual Library.
 * Original copyright notice follows.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.praxislive.ide.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Image;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.anchor.PointShape;
import org.netbeans.api.visual.anchor.PointShapeFactory;
import org.openide.util.ImageUtilities;

import java.util.Objects;

public class LAFScheme {

//    public static final String RESOURCES_KEY = "LAFScheme.Resources";

    static Color OFF_WHITE = new Color(241, 249, 253);

    private static final Color DARK_GREY = Color.decode("#191919");

    private static final Border BORDER_MINIMIZE
            = BorderFactory.createOpaqueBorder(2, 2, 2, 2);
    private static final PointShape POINT_SHAPE_IMAGE
            = PointShapeFactory.createImagePointShape(
                    ImageUtilities.loadImage("org/praxislive/ide/graph/resources/vmd-pin.png")); // NOI18N

    private static final Colors DEFAULT_RESOURCES
            = new Colors(new Color(0x748CC0), new Color(0xBACDF0));

    public static class Colors {

        private final Color COLOR_SELECTED;
        private final Color COLOR_NORMAL;

        private final Border BORDER_NODE;
        private final Border BORDER_NODE_FOCUSED;
        private final Border BORDER_NODE_SELECTED;
        private final Border BORDER_NODE_SELECTED_FOCUSED;
        
        private final Border BORDER_HEADER;
        private final Border BORDER_HEADER_SELECTED;
        
        private final Border BORDER_SMALL_NODE;
        private final Border BORDER_SMALL_NODE_FOCUSED;
        private final Border BORDER_SMALL_NODE_SELECTED;
        private final Border BORDER_SMALL_NODE_SELECTED_FOCUSED;
        
        private final Border BORDER_PIN;
        private final Border BORDER_PIN_SELECTED;

        public Colors(Color highlight, Color normal) {
            COLOR_SELECTED = Objects.requireNonNull(highlight);
            COLOR_NORMAL = Objects.requireNonNull(normal);
            BORDER_NODE = BorderFactory.createCompositeBorder(
                    BorderFactory.createEmptyBorder(2),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, DARK_GREY, COLOR_NORMAL));
            BORDER_NODE_FOCUSED = BorderFactory.createCompositeBorder(
                    BorderFactory.createResizeBorder(2, OFF_WHITE, true),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, DARK_GREY, COLOR_NORMAL));
            BORDER_NODE_SELECTED = BorderFactory.createCompositeBorder(
                    BorderFactory.createEmptyBorder(2),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, DARK_GREY, COLOR_SELECTED));
            BORDER_NODE_SELECTED_FOCUSED = BorderFactory.createCompositeBorder(
                    BorderFactory.createResizeBorder(2, OFF_WHITE, true),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, DARK_GREY, COLOR_SELECTED));
            
            BORDER_HEADER = BorderFactory.createRoundedBorder(8, 8, 8, 4, COLOR_NORMAL, COLOR_NORMAL);
            BORDER_HEADER_SELECTED = BorderFactory.createRoundedBorder(8, 8, 8, 4, COLOR_SELECTED, COLOR_SELECTED);
            
            BORDER_SMALL_NODE = BorderFactory.createCompositeBorder(
                    BorderFactory.createEmptyBorder(3),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR_NORMAL, null));
            BORDER_SMALL_NODE_FOCUSED = BorderFactory.createCompositeBorder(
                    BorderFactory.createResizeBorder(3, OFF_WHITE, true),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR_NORMAL, null));
            BORDER_SMALL_NODE_SELECTED = BorderFactory.createCompositeBorder(
                    BorderFactory.createEmptyBorder(3),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR_SELECTED, null));
            BORDER_SMALL_NODE_SELECTED_FOCUSED = BorderFactory.createCompositeBorder(
                    BorderFactory.createResizeBorder(3, OFF_WHITE, true),
                    BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR_SELECTED, null));
            
            BORDER_PIN = BorderFactory.createRoundedBorder(8, 8, 8, 4, null, null);
            BORDER_PIN_SELECTED = BorderFactory.createRoundedBorder(8, 8, 8, 4, null, COLOR_NORMAL);
        }

    }

    protected void installUI(NodeWidget widget) {
        widget.setOpaque(false);
        widget.getHeader().setOpaque(false);
        widget.getMinimizeButton().setBorder(BORDER_MINIMIZE);
        updateUI(widget);
    }

    protected void updateUI(NodeWidget widget) {
        ObjectState state = widget.getState();
        Colors colors = widget.getSchemeColors();
        if (colors == null) {
            colors = DEFAULT_RESOURCES;
        }
        if (widget.isBelowLODThreshold()) {
            if (state.isSelected()) {
                if (state.isFocused()) {
                    widget.setBorder(colors.BORDER_SMALL_NODE_SELECTED_FOCUSED);
                } else {
                    widget.setBorder(colors.BORDER_SMALL_NODE_SELECTED);
                }
            } else {
                if (state.isFocused()) {
                    widget.setBorder(colors.BORDER_SMALL_NODE_FOCUSED);
                } else {
                    widget.setBorder(colors.BORDER_SMALL_NODE);
                }
            }
        } else {
            if (state.isSelected()) {
                if (state.isFocused()) {
                    widget.setBorder(colors.BORDER_NODE_SELECTED_FOCUSED);
                } else {
                    widget.setBorder(colors.BORDER_NODE_SELECTED);
                }
            } else {
                if (state.isFocused()) {
                    widget.setBorder(colors.BORDER_NODE_FOCUSED);
                } else {
                    widget.setBorder(colors.BORDER_NODE);
                }
            }
        }
        Widget header = widget.getHeader();
        header.setBorder(state.isSelected() || state.isHovered()
                ? colors.BORDER_HEADER_SELECTED : colors.BORDER_HEADER);
        Widget comment = widget.getCommentWidget();
        if (comment != null) {
            comment.setBorder(colors.BORDER_HEADER);
        }
    }

    protected boolean isNodeMinimizeButtonOnRight(NodeWidget widget) {
        return false;
    }

    protected Image getMinimizeWidgetImage(NodeWidget widget) {
        return widget.isMinimized()
                ? ImageUtilities.loadImage("org/praxislive/ide/graph/resources/vmd-expand.png") // NOI18N
                : ImageUtilities.loadImage("org/praxislive/ide/graph/resources/vmd-collapse.png"); // NOI18N
    }

    protected void installUI(EdgeWidget widget) {
        widget.setSourceAnchorShape(AnchorShape.NONE);
        widget.setTargetAnchorShape(AnchorShape.NONE);
        widget.setPaintControlPoints(true);
        widget.setForeground(OFF_WHITE);
        updateUI(widget);
    }

    protected void updateUI(EdgeWidget widget) {
        ObjectState state = widget.getState();
        LAFScheme.Colors colors = widget.getSchemeColors();
        if (colors == null) {
            colors = DEFAULT_RESOURCES;
        }
        if (state.isHovered() || state.isSelected()) {
            widget.setEndPointShape(PointShape.SQUARE_FILLED_BIG);
        } else {
            widget.setControlPointShape(PointShape.NONE);
            widget.setEndPointShape(PointShape.SQUARE_FILLED_SMALL);
        }

        if (state.isHovered() || state.isSelected()) {
            widget.bringToFront();
            widget.setForeground(colors.COLOR_SELECTED);
            widget.setStroke(new BasicStroke(4));
        } else {
            widget.setForeground(colors.COLOR_SELECTED);
            widget.setStroke(new BasicStroke(2));
        }

        widget.setControlPointCutDistance(5);

    }

    protected void installUI(PinWidget widget) {
        widget.setOpaque(false);
        updateUI(widget);
    }

    protected void updateUI(PinWidget widget) {
        ObjectState state = widget.getState();
//        widget.setBorder(state.isHovered()
//                ? BORDER_PIN_HOVERED : BORDER_PIN);
        LAFScheme.Colors colors = widget.getSchemeColors();
        if (colors == null) {
            colors = DEFAULT_RESOURCES;
        }
        if (state.isHovered()) {
            widget.setBorder(colors.BORDER_PIN_SELECTED);
            widget.getPinNameWidget().setForeground(colors.COLOR_NORMAL);
        } else {
            widget.setBorder(colors.BORDER_PIN);
            widget.getPinNameWidget().setForeground(colors.COLOR_NORMAL);
        }

    }

    protected int getAnchorGap() {
        return 8;
    }

    protected Color getBackgroundColor() {
        return DARK_GREY;
    }
}
