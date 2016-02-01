/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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

import java.awt.Color;
import net.neilcsmith.praxis.live.graph.LAFScheme;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
enum Colors {

    Default(null), // new LAFScheme.Colors(new Color(0x748cc0), new Color(0xbacdf0))
    Red(new LAFScheme.Colors(new Color(0xff2a2a), new Color(0xff8080))),
    Green(new LAFScheme.Colors(new Color(0xaad400), new Color(0xc6d976))),
    Blue(new LAFScheme.Colors(new Color(0x748cc0), new Color(0xbacdf0))),
    Purple(new LAFScheme.Colors(new Color(0xd42aff), new Color(0xe580ff))),
    Orange(new LAFScheme.Colors(new Color(0xff9126), new Color(0xffb46a))),
    Yellow(new LAFScheme.Colors(new Color(0xf9f900), new Color(0xffff7a))),
    ;

    private final LAFScheme.Colors schemeColors;

    private Colors(LAFScheme.Colors schemeColors) {
        this.schemeColors = schemeColors;
    }
    
    LAFScheme.Colors getSchemeColors() {
        return schemeColors;
    }

}
