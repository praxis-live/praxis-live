/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
 */
package org.praxislive.ide.pxr.graph;

import java.util.Locale;
import org.praxislive.ide.core.ui.api.TypeColor;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.graph.scene.LAFScheme;

/**
 *
 */
class Utils {

    private Utils() {
    }

    static LAFScheme.Colors colorsForPortType(String type) {
        return LAFScheme.Colors.forTypeColor(typeColorForString(type, true));
    }

    static LAFScheme.Colors colorsForComponent(ComponentProxy cmp) {
        if (cmp instanceof ContainerProxy) {
            return LAFScheme.Colors.forTypeColor(TypeColor.Orange);
        } else {
            return LAFScheme.Colors.forTypeColor(
                    typeColorForString(cmp.getType().toString(), false));
        }
    }

    private static TypeColor typeColorForString(String string, boolean port) {
        String test = string.toLowerCase(Locale.ROOT);
        if (test.startsWith("audio")) {
            return TypeColor.Green;
        }
        if (test.startsWith("video")) {
            return TypeColor.Purple;
        }
        if (port) {
            if (test.startsWith("control")) {
                return TypeColor.Blue;
            }
            if (test.startsWith("ref")) {
                return TypeColor.Cyan;
            }
        } else {
            if (test.startsWith("core")) {
                return TypeColor.Blue;
            }
            if (test.startsWith("data")) {
                return TypeColor.Red;
            }
        }
        return TypeColor.Magenta;
    }

}
