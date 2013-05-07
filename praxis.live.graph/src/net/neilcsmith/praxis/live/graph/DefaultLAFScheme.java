/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
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
 * Please visit http://neilcsmith.net if you need additional information or
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
package net.neilcsmith.praxis.live.graph;

import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.anchor.PointShape;
import org.netbeans.api.visual.anchor.PointShapeFactory;
import org.openide.util.ImageUtilities;

import java.awt.*;
import org.netbeans.api.visual.widget.Scene;

public class DefaultLAFScheme extends LAFScheme {

    static final Color COLOR_NORMAL = new Color(0xBACDF0);
    private static final Color COLOR_HOVERED = Color.WHITE;
    private static final Color COLOR_SELECTED = new Color(0x748CC0);
    static final Color COLOR_HIGHLIGHTED = new Color(0x316AC5);
//    private static final Color COLOR0 = new Color (169, 197, 235);
    static final Color COLOR1 = new Color(221, 235, 246);
    static final Color COLOR2 = new Color(255, 255, 255);
    static final Color COLOR3 = new Color(214, 235, 255);
    static final Color COLOR4 = new Color(241, 249, 253);
    static final Color COLOR5 = new Color(255, 255, 255);
//    public static final Border BORDER_NODE = new NodeBorder(COLOR_NORMAL, 1, COLOR1, COLOR2, COLOR3, COLOR4, COLOR5);
    static final Border BORDER_NODE = BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR4, null);
    static final Border BORDER_NODE_SELECTED = BorderFactory.createRoundedBorder(8, 8, 0, 0, COLOR_SELECTED, null);
    static final Color BORDER_CATEGORY_BACKGROUND = new Color(0xCDDDF8);
    static final Border BORDER_MINIMIZE = BorderFactory.createRoundedBorder(2, 2, null, COLOR_NORMAL);
//    static final Border BORDER_HEADER = BorderFactory.createOpaqueBorder (2, 8, 2, 8);
    static final Border BORDER_HEADER = BorderFactory.createRoundedBorder(8, 8, 8, 2, COLOR_NORMAL, null);
    static final Border BORDER_HEADER_SELECTED = BorderFactory.createRoundedBorder(8, 8, 8, 2, COLOR_SELECTED, null);
    static final Border BORDER_PIN = BorderFactory.createOpaqueBorder(1, 4, 1, 4);
    private static final Border BORDER_PIN_HOVERED = BorderFactory.createLineBorder(1, 4, 1, 4, Color.DARK_GRAY);
    static final PointShape POINT_SHAPE_IMAGE = PointShapeFactory.createImagePointShape(
            ImageUtilities.loadImage("net/neilcsmith/praxis/live/graph/resources/vmd-pin.png")); // NOI18N

    public DefaultLAFScheme() {
    }

    public void installUI(NodeWidget widget) {
        widget.setBorder(BORDER_NODE);
        widget.setOpaque(false);

        Widget header = widget.getHeader();
        header.setBorder(BORDER_HEADER);
//        header.setBackground (COLOR_SELECTED);
//        header.setOpaque (false);

//        header.setBackground(COLOR_NORMAL);
        header.setOpaque(false);

        Widget minimize = widget.getMinimizeButton();
        minimize.setBorder(BORDER_MINIMIZE);

        Widget pinsSeparator = widget.getPinsSeparator();
        pinsSeparator.setForeground(BORDER_CATEGORY_BACKGROUND);
    }

    @Override
    public void updateUI(NodeWidget widget, ObjectState previousState, ObjectState state) {
        if (!previousState.isSelected() && state.isSelected()) {
            widget.bringToFront();
        } else if (!previousState.isHovered() && state.isHovered()) {
            widget.bringToFront();
        }

        Widget header = widget.getHeader();
//        header.setOpaque (state.isSelected ());
//        header.setBackground(state.isSelected() ? COLOR_SELECTED : COLOR_NORMAL);
        header.setBorder(state.isSelected() || state.isHovered() ? BORDER_HEADER_SELECTED : BORDER_HEADER);
    }

    void updateOnRevalidate(NodeWidget widget, boolean belowLOD) {
        if (belowLOD) {
            widget.setBorder(widget.isSelected() ? BORDER_NODE_SELECTED : BORDER_NODE);         
        } else {
            widget.setBorder(BORDER_NODE);
        }
    }

    @Override
    public boolean isNodeMinimizeButtonOnRight(NodeWidget widget) {
        return false;
    }

    @Override
    public Image getMinimizeWidgetImage(NodeWidget widget) {
        return widget.isMinimized()
                ? ImageUtilities.loadImage("net/neilcsmith/praxis/live/graph/resources/vmd-expand.png") // NOI18N
                : ImageUtilities.loadImage("net/neilcsmith/praxis/live/graph/resources/vmd-collapse.png"); // NOI18N
    }

//    @Override
//    public Widget createPinCategoryWidget (NodeWidget widget, String categoryDisplayName) {
//        return createPinCategoryWidgetCore (widget, categoryDisplayName, true);
//    }
    @Override
    public void installUI(EdgeWidget widget) {
        widget.setSourceAnchorShape(AnchorShape.NONE);
        widget.setTargetAnchorShape(AnchorShape.NONE);
        widget.setPaintControlPoints(true);
    }

    @Override
    public void updateUI(EdgeWidget widget, ObjectState previousState, ObjectState state) {
        if (state.isHovered()) {
            widget.setForeground(COLOR_HOVERED);
        } else if (state.isSelected()) {
            widget.setForeground(COLOR_SELECTED);
        } else if (state.isHighlighted()) {
            widget.setForeground(COLOR_HIGHLIGHTED);
        } else if (state.isFocused()) {
            widget.setForeground(COLOR_HOVERED);
        } else {
            widget.setForeground(COLOR_NORMAL);
        }

        if (state.isSelected()) {
//            widget.setControlPointShape (PointShape.SQUARE_FILLED_SMALL);
            widget.setEndPointShape(PointShape.SQUARE_FILLED_BIG);
        } else {
            widget.setControlPointShape(PointShape.NONE);
            widget.setEndPointShape(POINT_SHAPE_IMAGE);
        }

        if (state.isHovered() || state.isSelected()) {
            widget.bringToFront();
            widget.setStroke(new BasicStroke(3));
        } else {
            widget.setStroke(new BasicStroke());
        }

        widget.setControlPointCutDistance(5);

    }

    @Override
    public void installUI(PinWidget widget) {
        widget.setBorder(BORDER_PIN);
//        widget.setBackground (COLOR_SELECTED);
        widget.setOpaque(false);
    }

    @Override
    public void updateUI(PinWidget widget, ObjectState previousState, ObjectState state) {
//        widget.setOpaque (state.isSelected ());
        widget.setBorder(state.isFocused() || state.isHovered() ? BORDER_PIN_HOVERED : BORDER_PIN);
//        LookFeel lookFeel = getScene ().getLookFeel ();
//        setBorder (BorderFactory.createCompositeBorder (BorderFactory.createEmptyBorder (8, 2), lookFeel.getMiniBorder (state)));
//        setForeground (lookFeel.getForeground (state));
    }

    @Override
    public int getAnchorGap() {
        return 8;
    }
//    static Widget createPinCategoryWidgetCore (NodeWidget widget, String categoryDisplayName, boolean changeFont) {
//        Scene scene = widget.getScene ();
//        LabelWidget label = new LabelWidget (scene, categoryDisplayName);
//        label.setOpaque (true);
//        label.setBackground (BORDER_CATEGORY_BACKGROUND);
//        label.setForeground (Color.GRAY);
//        if (changeFont) {
//            Font fontPinCategory = scene.getDefaultFont ().deriveFont (10.0f);
//            label.setFont (fontPinCategory);
//        }
//        label.setAlignment (LabelWidget.Alignment.CENTER);
//        label.setCheckClipping (true);
//        return label;
//    }
}
