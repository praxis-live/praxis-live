/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
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
 */
package net.neilcsmith.praxis.live.graph;

import java.awt.Color;
import java.awt.Point;
import org.netbeans.api.visual.action.ConnectDecorator;
import org.netbeans.api.visual.anchor.Anchor;
import org.netbeans.api.visual.anchor.AnchorFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PraxisConnectDecorator implements ConnectDecorator {

    @Override
    public ConnectionWidget createConnectionWidget(Scene scene) {
        ConnectionWidget widget = new ConnectionWidget(scene);
        widget.setForeground(Color.WHITE);
        return widget;
    }

    @Override
    public Anchor createSourceAnchor(Widget sourceWidget) {
        if (sourceWidget instanceof PinWidget) {
            return ((PinWidget) sourceWidget).createAnchor();
        } else {
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
