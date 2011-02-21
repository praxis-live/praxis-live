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

import java.awt.Color;
import java.awt.Image;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.border.Border;
import org.netbeans.api.visual.border.BorderFactory;


public abstract class LAFScheme {

     private static final Color COLOR_SELECTED = new Color (0x447BCD);
    private static final Color COLOR_HIGHLIGHTED = COLOR_SELECTED.darker ();
    private static final Color COLOR_HOVERED = COLOR_SELECTED.brighter ();
    private static final int MARGIN = 3;
    private static final int ARC = 10;
    private static final int MINI_THICKNESS = 1;

    private static final Border BORDER_NORMAL = BorderFactory.createEmptyBorder (MARGIN, MARGIN);
    private static final Border BORDER_HOVERED = BorderFactory.createRoundedBorder (ARC, ARC, MARGIN, MARGIN, COLOR_HOVERED, COLOR_HOVERED.darker ());
    private static final Border BORDER_SELECTED = BorderFactory.createRoundedBorder (ARC, ARC, MARGIN, MARGIN, COLOR_SELECTED, COLOR_SELECTED.darker ());

    private static final Border MINI_BORDER_NORMAL = BorderFactory.createEmptyBorder (MINI_THICKNESS);
    private static final Border MINI_BORDER_HOVERED = BorderFactory.createRoundedBorder (MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS, COLOR_HOVERED, COLOR_HOVERED.darker ());
    private static final Border MINI_BORDER_SELECTED = BorderFactory.createRoundedBorder (MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS, MINI_THICKNESS, COLOR_SELECTED, COLOR_SELECTED.darker ());



    protected LAFScheme() {
    }

    /**
     * Called to install UI to a node widget.
     * @param widget the node widget
     */
    public abstract void installUI(NodeWidget widget);

    public void revalidateUI(NodeWidget widget) {}

    /**
     * Called to update UI of a node widget. Called from NodeWidget.notifyStateChanged method.
     * @param widget the node widget
     * @param previousState the previous state
     * @param state the new state
     */
    public abstract void updateUI(NodeWidget widget, ObjectState previousState, ObjectState state);

    /**
     * Returns whether the node minimize button is on the right side of the node header.
     * @param widget the node widget
     * @return true, if the button is on the right side; false, if the button is on the left side
     */
    public abstract boolean isNodeMinimizeButtonOnRight(NodeWidget widget);

    /**
     * Returns an minimize-widget image for a specific node widget.
     * @param widget the node widget
     * @return the minimize-widget image
     */
    public abstract Image getMinimizeWidgetImage(NodeWidget widget);

//    /**
//     * Called to create a pin-category widget.
//     * @param widget the node widget
//     * @param categoryDisplayName the category display name
//     * @return the pin-category widget
//     */
//    public abstract Widget createPinCategoryWidget(NodeWidget widget, String categoryDisplayName);

    /**
     * Called to install UI to a connection widget.
     * @param widget the connection widget
     */
    public abstract void installUI(EdgeWidget widget);

    public void revalidateUI(EdgeWidget widget) {}

    /**
     * Called to update UI of a connection widget. Called from EdgeWidget.notifyStateChanged method.
     * @param widget the connection widget
     * @param previousState the previous state
     * @param state the new state
     */
    public abstract void updateUI(EdgeWidget widget, ObjectState previousState, ObjectState state);

    /**
     * Called to install UI to a pin widget.
     * @param widget the pin widget
     */
    public abstract void installUI(PinWidget widget);

    public void revalidateUI(PinWidget widget) {}

    /**
     * Called to update UI of a pin widget. Called from PinWidget.notifyStateChanged method.
     * @param widget the pin widget
     * @param previousState the previous state
     * @param state the new state
     */
    public abstract void updateUI(PinWidget widget, ObjectState previousState, ObjectState state);

    /**
     * Returns a gap size of a node-anchor from a node-widget.
     * @param anchor the node anchor
     * @return the gap size
     */
    public abstract int getAnchorGap();

    public Color getBackgroundColor() {
        return Color.BLACK;
    }
    private static LAFScheme SCHEME_DEFAULT = new DefaultLAFScheme();

    public static LAFScheme getDefault() {
        return SCHEME_DEFAULT;
    }

    public static Border createVMDNodeBorder(Color borderColor, int borderThickness, Color color1, Color color2, Color color3, Color color4, Color color5) {
        return new NodeBorder(borderColor, borderThickness, color1, color2, color3, color4, color5);
    }
}
