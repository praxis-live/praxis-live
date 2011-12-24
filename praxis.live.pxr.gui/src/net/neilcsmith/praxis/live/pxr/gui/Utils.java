/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.gui;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.IDEUtil;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.gui.Keys;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class Utils {

    private final static Logger LOG = Logger.getLogger(Utils.class.getName());

    private Utils() {
    }

    static void enableAll(JComponent component) {
        component.setEnabled(true);
        for (Component cmp : component.getComponents()) {
            if (cmp instanceof JComponent) {
                enableAll((JComponent) cmp);
            }
        }
    }

    static void disableAll(JComponent component) {
        component.setEnabled(false);
        for (Component cmp : component.getComponents()) {
            if (cmp instanceof JComponent) {
                disableAll((JComponent) cmp);
            }
        }
    }

    static ComponentProxy findComponentProxy(RootProxy root, ComponentAddress address) {
        if (address.getRootID().equals(root.getAddress().getRootID())) {
            ComponentProxy comp = root;
            for (int i = 1; i < address.getDepth(); i++) {
                if (comp instanceof ContainerProxy) {
                    comp = ((ContainerProxy) comp).getChild(address.getComponentID(i));
                } else {
                    return null;
                }
            }
            return comp;
        }
        return null;
    }

    static ComponentProxy findComponentProxy(RootProxy root, JComponent cmp) {
        Object o = cmp.getClientProperty(Keys.Address);
        if (o instanceof ComponentAddress) {
            return findComponentProxy(root, (ComponentAddress) o);
        }
        return null;
    }

    static JComponent findAddressedComponent(Component cmp) {
        do {
            if (cmp instanceof JComponent
                    && ((JComponent) cmp).getClientProperty(Keys.Address) != null) {
                return (JComponent) cmp;
            }
            cmp = cmp.getParent();
        } while (cmp != null);
        return null;
    }

    static ComponentAddress getComponentAddress(Component cmp) {
        if (cmp instanceof JComponent) {
            Object ad = ((JComponent) cmp).getClientProperty(Keys.Address);
            if (ad instanceof ComponentAddress) {
                return (ComponentAddress) ad;
            }
        }
        return null;
    }

    static JComponent findContainerComponent(RootProxy root, Component cmp) {
        ComponentProxy pxy;
        do {
            cmp = findAddressedComponent(cmp);
            if (cmp instanceof JComponent) {
                pxy = findComponentProxy(root, (JComponent) cmp);
                if (pxy instanceof ContainerProxy) {
                    return (JComponent) cmp;
                }
            }
            cmp = cmp.getParent();
        } while (cmp != null);
        return null;
    }

    static int[] getGridPosition(JComponent container, JComponent component) {
        HashMap<Object, int[]> gridPositions = IDEUtil.getGridPositions(container);
        int[] result = gridPositions.get(component);
        if (LOG.isLoggable(Level.FINE)) {
            if (result == null) {
                LOG.log(Level.FINE, "getGridPosition() null for {0} in {1}",
                        new Object[]{Utils.getComponentAddress(component),
                            Utils.getComponentAddress(container)});
            } else {
                LOG.log(Level.FINEST, "getGridPosition() is {0} for {1} in {2}",
                        new Object[]{Arrays.toString(result),
                            Utils.getComponentAddress(component),
                            Utils.getComponentAddress(container)});
            }
        }
        return result;
    }

    static int[] getGridPosition(JComponent container, int mouseX, int mouseY) {
        int[] result = new int[]{0, 0};
        int[][] axisSizes = IDEUtil.getColumnSizes(container);
        int[] sizes = axisSizes[1];
        int i = 0;
        int accum = 0;
        for (; i < sizes.length; i++) {
            accum += sizes[i];
            if (accum > mouseX) {
                break;
            }
        }
        i = i / 2;
        if (i < axisSizes[0].length) {
            result[0] = axisSizes[0][i];
        } else {
            result[0] = axisSizes[0][axisSizes[0].length - 1] + 1;
        }

        axisSizes = IDEUtil.getRowSizes(container);
        sizes = axisSizes[1];
        i = 0;
        accum = 0;
        for (; i < sizes.length; i++) {
            accum += sizes[i];
            if (accum > mouseY) {
                break;
            }
        }
        i = i / 2;
        if (i < axisSizes[0].length) {
            result[1] = axisSizes[0][i];
        } else {
            result[1] = axisSizes[0][axisSizes[0].length - 1] + 1;
        }

        return result;

    }

    static CC getConstraints(JComponent cmp) {
        Object val = cmp.getClientProperty(Keys.LayoutConstraint);
        if (val instanceof CC) {
            return (CC) val;
        } else {
            return new CC();
        }
    }

    static void updateConstraints(JComponent cmp, CC cc) {
        cmp.putClientProperty(Keys.LayoutConstraint, null);
        cmp.putClientProperty(Keys.LayoutConstraint, cc);
    }

    static boolean isOccupied(JComponent container, int x, int y, int width, int height, Set<JComponent> ignored) {
        return !requiredSpace(container, x, y, width, height, ignored).isEmpty();
    }

    static void ensureSpace(JComponent container, int x, int y, int width, int height, Set<JComponent> ignored, boolean vertical) {
        Rectangle space = requiredSpace(container, x, y, width, height, ignored);
        if (space.isEmpty()) {
            return;
        }
        if (vertical) {
            moveChildren(container, space.y, (y + height) - space.y, vertical, ignored);
        } else {
            moveChildren(container, space.x, (x + width) - space.x, vertical, ignored);
        }

    }

    static void move(RootProxy root, JComponent cmp, int dx, int dy) {
        changeBounds(root, cmp, dx, dy, 0, 0, Math.abs(dy) > Math.abs(dx));
    }
    
    static void resize(RootProxy root, JComponent cmp, int dSpanX, int dSpanY) {
        changeBounds(root, cmp, 0, 0, dSpanX, dSpanY, Math.abs(dSpanY) > Math.abs(dSpanX));
    }
    
    private static void changeBounds(RootProxy root, JComponent cmp, int dx, int dy, int dSpanX, int dSpanY, boolean vertical) {
        JComponent container = findContainerComponent(root, cmp);
        int[] pos = getGridPosition(container, cmp);
        if (pos == null) {
            LOG.log(Level.FINE, "changeBounds() can't find component {0} in {1}",
                    new Object[]{getComponentAddress(cmp),
                        getComponentAddress(container)});
            return;
        }
        CC cc = getConstraints(cmp);
        Set<JComponent> ignore = Collections.singleton(cmp);

        int x = pos[0] + dx;
        int y = pos[1] + dy;
        int spanX = pos[2] + dSpanX;
        int spanY = pos[3] + dSpanY;
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (spanX < 1) {
            spanX = 1;
        }
        if (spanY < 1) {
            spanY = 1;
        }
        ensureSpace(container, x, y, spanX, spanY, ignore, vertical);
        cc.setCellX(x);
        cc.setCellY(y);
        cc.setSpanX(spanX);
        cc.setSpanY(spanY);
        updateConstraints(cmp, cc);
        compactGrid(container);
    }

    private static Rectangle requiredSpace(JComponent container,
            int x, int y, int width, int height,
            Set<JComponent> ignored) {
        HashMap<Object, int[]> gridPositions = IDEUtil.getGridPositions(container);
        if (gridPositions == null || gridPositions.isEmpty()) {
            return new Rectangle();
        }
        Rectangle space = new Rectangle(x, y, width, height);
        Rectangle intersection = new Rectangle();
        Rectangle cmp = new Rectangle();

        for (Map.Entry<Object, int[]> entry : gridPositions.entrySet()) {
            if (ignored.contains(entry.getKey())) {
                continue;
            }
            int[] pos = entry.getValue();
            if (pos.length < 4) {
                continue;
            }
            cmp.x = pos[0];
            cmp.y = pos[1];
            cmp.width = pos[2];
            cmp.height = pos[3];
            if (cmp.intersects(space)) {
                if (intersection.isEmpty()) {
                    intersection.setBounds(cmp);
                } else {
                    intersection.add(cmp);
                }
            }
        }
        return intersection;
    }

    static void compactGrid(JComponent container) {
        container.validate();
        compactAxis(container, true);
        compactAxis(container, false);
    }

    private static void compactAxis(JComponent container, boolean vertical) {
        int[] ax = getAxisIndexes(container, vertical);
        int exp = 0;
        int i = 0;
        int overall = 0;
        int cur;

        while (i < ax.length && overall < 50) {
            overall++;
            cur = ax[i];
            if (cur != exp) {
                if (LOG.isLoggable(Level.FINE)) {
                    String orientation = vertical ? "Rows" : "Columns";
                    LOG.fine("Found gap in " + orientation + " , removing " + exp + " -> " + cur);
                }
                moveChildren(container, cur, exp - cur, vertical, Collections.<JComponent>emptySet());
                ax = getAxisIndexes(container, vertical);
                exp = 0;
                i = 0;
            } else {
                i++;
                exp++;
            }
        }

    }

    private static int[] getAxisIndexes(JComponent container, boolean vertical) {
//        int[][] axes = vertical ? IDEUtil.getRowSizes(container) : IDEUtil.getColumnSizes(container);
//        if (axes == null) {
//            if (LOG.isLoggable(Level.FINE)) {
//                String orientation = vertical ? "Rows" : "Columns";
//                LOG.log(Level.FINE, "No {0} found in {1}", new Object[]{
//                            orientation, Utils.getComponentAddress(container)});
//            }
//            return new int[0];
//        } else {
//            if (LOG.isLoggable(Level.FINE)) {
//                String orientation = vertical ? "Rows" : "Columns";
//                LOG.log(Level.FINE, "Found {0} {1} in {2}", new Object[]{
//                            orientation, Arrays.toString(axes[0]), Utils.getComponentAddress(container)});
//            }
//            return axes[0];
//        }
        
        HashMap<Object, int[]> gridPositions = IDEUtil.getGridPositions(container);
        if (gridPositions == null || gridPositions.isEmpty()) {
            if (LOG.isLoggable(Level.FINE)) {
                String orientation = vertical ? "Rows" : "Columns";
                LOG.log(Level.FINE, "No {0} found in {1}", new Object[]{
                            orientation, Utils.getComponentAddress(container)});
            }
            return new int[0];
        }
        TreeSet<Integer> axes = new TreeSet<Integer>();
        int val, span;
        for (Map.Entry<Object, int[]> entry : gridPositions.entrySet()) {
            int[] pos = entry.getValue();
            if (pos.length < 4) {
                continue;
            }
            
            if (vertical) {
                val = pos[1];
                span = pos[3];
            } else {
                val = pos[0];
                span = pos[2];
            }
            
            axes.add(val);
            while (span > 1) {
                LOG.fine("Adding span");
                val++;
                axes.add(val);
                span--;
            }
        }
        
        int[] result = new int[axes.size()];
        int i = 0;
        for (Integer ax : axes) {
            result[i++] = ax;
        }
        if (LOG.isLoggable(Level.FINE)) {
            String orientation = vertical ? "Rows" : "Columns";
            LOG.log(Level.FINE, "Found {0} {1} in {2}", new Object[]{
                        orientation, Arrays.toString(result), Utils.getComponentAddress(container)});
        }
        return result;
    }

    private static void moveChildren(JComponent container, int from, int diff, boolean vertical, Set<JComponent> ignored) {

        for (Component cmp : container.getComponents()) {
            if (cmp instanceof JComponent) {
                if (ignored.contains(cmp)) {
                    continue;
                }
                JComponent jc = (JComponent) cmp;
                Object val = jc.getClientProperty(Keys.LayoutConstraint);
                if (val instanceof CC) {
                    CC cc = (CC) val;
                    if (vertical) {
                        int y = cc.getCellY();
                        if (y >= from) {
                            cc.setCellY(y + diff);
                        }
                    } else {
                        int x = cc.getCellX();
                        if (x >= from) {
                            cc.setCellX(x + diff);
                        }
                    }
                    jc.putClientProperty(Keys.LayoutConstraint, null);
                    jc.putClientProperty(Keys.LayoutConstraint, cc);
                }
            }
        }
        container.validate();

    }
}
