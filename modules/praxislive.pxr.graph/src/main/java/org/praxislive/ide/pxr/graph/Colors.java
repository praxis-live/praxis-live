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
 */
package org.praxislive.ide.pxr.graph;

import org.praxislive.ide.pxr.graph.scene.LAFScheme;

/**
 *
 */
enum Colors {

    Default(LAFScheme.DEFAULT_COLORS), // new LAFScheme.Colors(new Color(0x748cc0), new Color(0xbacdf0))
    Red(LAFScheme.RED),
    Green(LAFScheme.GREEN),
    Blue(LAFScheme.BLUE),
    Purple(LAFScheme.PURPLE),
    Orange(LAFScheme.ORANGE),
    Yellow(LAFScheme.YELLOW);

    private final LAFScheme.Colors schemeColors;

    private Colors(LAFScheme.Colors schemeColors) {
        this.schemeColors = schemeColors;
    }

    LAFScheme.Colors getSchemeColors() {
        return schemeColors;
    }

}
