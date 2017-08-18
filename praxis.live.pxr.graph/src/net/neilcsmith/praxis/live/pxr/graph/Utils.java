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

import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import net.neilcsmith.praxis.live.model.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.Attributes;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith
 */
class Utils {

    private Utils() {
    }

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
//            ((PXRComponentProxy)cmp).setAttribute(GraphEditor.ATTR_GRAPH_X, Integer.toString(x));
//            ((PXRComponentProxy)cmp).setAttribute(GraphEditor.ATTR_GRAPH_Y, Integer.toString(y));
            setAttr(cmp, GraphEditor.ATTR_GRAPH_X, Integer.toString(x));
            setAttr(cmp, GraphEditor.ATTR_GRAPH_Y, Integer.toString(y));
        }
    }

    static void getPosition(ComponentProxy cmp, Point pt) {
        int x, y;
//        String attrX = ((PXRComponentProxy)cmp).getAttribute(GraphEditor.ATTR_GRAPH_X);
//        String attrY = ((PXRComponentProxy)cmp).getAttribute(GraphEditor.ATTR_GRAPH_Y);
        String attrX = getAttr(cmp, GraphEditor.ATTR_GRAPH_X);
        String attrY = getAttr(cmp, GraphEditor.ATTR_GRAPH_Y);
        try {
            x = attrX == null ? 0 : Integer.parseInt(attrX);
            y = attrY == null ? 0 : Integer.parseInt(attrY);
        } catch (NumberFormatException nfe) {
            x = y = 0;
        }
        pt.x = x;
        pt.y = y;
    }

    static void setAttr(ComponentProxy cmp, String key, String value) {
        Attributes attrs = cmp.getLookup().lookup(Attributes.class);
        if (attrs == null) {
            return;
        }
        attrs.setAttribute(key, value);
    }

    static String getAttr(ComponentProxy cmp, String key) {
        Attributes attrs = cmp.getLookup().lookup(Attributes.class);
        if (attrs == null) {
            return null;
        }
        return attrs.getAttribute(key);
    }

    static String getAttr(ComponentProxy cmp, String key, String def) {
        String ret = getAttr(cmp, key);
        return ret == null ? def : ret;
    }

    static Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append('.');
                    break;
                case '|':
                    regex.append('|');
                    break;
                case '_':
                    regex.append('_');
                    break;
                case '-':
                    regex.append("\\-");
                    break;
                default:
                    if (Character.isJavaIdentifierPart(c)) {
                        regex.append(c);
                    } else {
                        throw new IllegalArgumentException();
                    }
            }
        }
        return Pattern.compile(regex.toString());
    }

    static void configureFocusActionKeys(JTextField textField, boolean primary) {
        if (!primary) {
            textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), JTextField.notifyAction);
            textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JTextField.notifyAction);
        }
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                textField.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
    }
    
    static String nodesToGlob(Node[] nodes) {
        return Stream.of(nodes).map(Node::getName).collect(Collectors.joining("|"));
    }
    


}
