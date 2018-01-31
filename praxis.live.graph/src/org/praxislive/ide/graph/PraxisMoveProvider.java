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
 * @author Neil C Smith (http://neilcsmith.net)
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
