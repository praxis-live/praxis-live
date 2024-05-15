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
package org.praxislive.ide.graph;

import java.awt.Point;
import org.netbeans.api.visual.action.ConnectDecorator;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

class PraxisConnectDecorator implements ConnectDecorator {

    private EdgeWidget widget;
    
    @Override
    public ConnectionWidget createConnectionWidget(Scene scene) {
        widget = new EdgeWidget((PraxisGraphScene) scene);
        return widget;
    }

    @Override
    public Anchor createSourceAnchor(Widget sourceWidget) {
        if (sourceWidget instanceof PinWidget) {
            if (widget != null) {
                widget.srcPin = (PinWidget) sourceWidget;
                widget.revalidate();
            }
            return ((PinWidget) sourceWidget).createAnchor();
        } else {
            if (widget != null) {
                widget.srcPin = null;
                widget.revalidate();
            }
            return AnchorFactory.createCenterAnchor(sourceWidget);
        }
        
    }

    @Override
    public Anchor createTargetAnchor(Widget targetWidget) {
        if (targetWidget instanceof PinWidget) {
            return ((PinWidget) targetWidget).createAnchor();
        } else {
            return AnchorFactory.createCenterAnchor(targetWidget);
        }
        
    }

    @Override
    public Anchor createFloatAnchor(Point location) {
        return AnchorFactory.createFixedAnchor(location);
    }
}
