/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.Point;
import java.util.List;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;

/**
 *
 * @author Neil C Smith
 */
class Utils {
    
    private Utils() {}
    
    static Point findOffset(List<ComponentProxy> cmps) {
        Point pt = new Point();
        Point loc = new Point();
        boolean first = true;
        for (ComponentProxy cmp : cmps) {
            getPosition(cmp, loc);
            if (first) {
                pt.x = loc.x;
                pt.y = loc.y;
                first = false;
            } else {
                pt.x = Math.min(pt.x, loc.x);
                pt.y = Math.min(pt.y, loc.y);
            }
        }
        return pt;
    }

    static void offsetComponents(List<ComponentProxy> cmps, Point offset, boolean replace) {
        Point loc = new Point();
        for (ComponentProxy cmp : cmps) {
            getPosition(cmp, loc);
            int x = replace ? loc.x + offset.x : loc.x - offset.x;
            int y = replace ? loc.y + offset.y : loc.y - offset.y;
            cmp.setAttribute(GraphEditor.ATTR_GRAPH_X, Integer.toString(x));
            cmp.setAttribute(GraphEditor.ATTR_GRAPH_Y, Integer.toString(y));
        }
    }

    static void getPosition(ComponentProxy cmp, Point pt) {
        int x, y;
        String attrX = cmp.getAttribute(GraphEditor.ATTR_GRAPH_X);
        String attrY = cmp.getAttribute(GraphEditor.ATTR_GRAPH_Y);
        try {
            x = attrX == null ? 0 : Integer.parseInt(attrX);
            y = attrY == null ? 0 : Integer.parseInt(attrY);
        } catch (NumberFormatException numberFormatException) {
            x = y = 0;
        }
        pt.x = x;
        pt.y = y;
    }
    
}
