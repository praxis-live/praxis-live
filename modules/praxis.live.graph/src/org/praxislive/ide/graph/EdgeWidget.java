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
package org.praxislive.ide.graph;

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
 * This class represents a connection widget
 *
 * @author David Kaspar
 */
public class EdgeWidget extends ConnectionWidget {

    private final PraxisGraphScene scene;
    private final SceneListenerImpl sceneListener;
    
    PinWidget srcPin;
    PinWidget dstPin;
    
    private LAFScheme scheme;
    private LAFScheme.Colors schemeColors;

    /**
     * Creates a connection widget with a specific color scheme.
     * @param scene the scene
     */
    public EdgeWidget(PraxisGraphScene scene, PinWidget srcPin, PinWidget dstPin) {
        super (scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();
        this.srcPin = Objects.requireNonNull(srcPin);
        this.dstPin = Objects.requireNonNull(dstPin);
        scheme.installUI (this);
    }
    
    EdgeWidget(PraxisGraphScene scene) {
        super (scene);
        this.scene = scene;
        this.sceneListener = new SceneListenerImpl();
        this.scheme = scene.getLookAndFeel();
        scheme.installUI (this);
    }
    

    @Override
    protected void notifyAdded() {
        scene.addSceneListener(sceneListener);
    }

    @Override
    protected void notifyRemoved() {
        scene.removeSceneListener(sceneListener);
    }
    
    public void setSchemeColors(LAFScheme.Colors colors) {
        this.schemeColors = colors;
        revalidate();
    }
    
    public LAFScheme.Colors getSchemeColors() {
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
     * @param previousState the previous state
     * @param state the new state
     */
    @Override
    public void notifyStateChanged (ObjectState previousState, ObjectState state) {
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
