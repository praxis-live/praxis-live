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
package org.praxislive.ide.pxr.graph.scene;

import java.awt.Dimension;
import java.awt.Point;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.widget.Widget;

class ToolContainerWidget extends Widget {

    private final PraxisGraphScene<?> scene;

    public ToolContainerWidget(PraxisGraphScene<?> scene) {
        super(scene);
        this.scene = scene;
        setLayout(LayoutFactory.createVerticalFlowLayout(LayoutFactory.SerialAlignment.JUSTIFY, 4));
        setMinimumSize(new Dimension(100, 10));
    }

    @Override
    protected void paintChildren() {
        if (!scene.isBelowLODThreshold()) {
            super.paintChildren();
        }
    }

    @Override
    public boolean isHitAt(Point localLocation) {
        if (scene.isBelowLODThreshold()) {
            return false;
        } else {
            return super.isHitAt(localLocation);
        }
    }
}
