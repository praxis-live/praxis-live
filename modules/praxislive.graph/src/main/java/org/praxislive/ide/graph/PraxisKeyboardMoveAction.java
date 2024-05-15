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
import java.awt.event.KeyEvent;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.MoveStrategy;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;

/**
 *
 */
class PraxisKeyboardMoveAction extends WidgetAction.Adapter {

    private final static int MOVEMENT = 10;
    
    private final MoveStrategy strategy;
    private final MoveProvider provider;
    
    private Widget movingWidget;
    
    PraxisKeyboardMoveAction(MoveStrategy strategy, MoveProvider provider) {
        this.strategy = strategy;
        this.provider = provider;
    }

    @Override
    public State keyPressed(Widget widget, WidgetKeyEvent event) {
        if (!isArrowKey(event)) {
            return State.REJECTED;
        }
        int x = 0;
        int y = 0;
        switch (event.getKeyCode()) {
            case KeyEvent.VK_UP :
                y -= MOVEMENT;
                break;
            case KeyEvent.VK_DOWN :
                y += MOVEMENT;
                break;
            case KeyEvent.VK_LEFT :
                x -= MOVEMENT;
                break;
            case KeyEvent.VK_RIGHT :
                x += MOVEMENT;
                break;
        }
        
        if (movingWidget != widget) {
            movingWidget = widget;
            provider.movementStarted(widget);
        }
        
        Point location = provider.getOriginalLocation(widget);
        Point suggested = new Point(location);
        suggested.x += x;
        suggested.y += y;
        
        provider.setNewLocation(widget, strategy.locationSuggested(widget, location, suggested));
        
        return State.CONSUMED;
        
    }

    @Override
    public State keyReleased(Widget widget, WidgetKeyEvent event) {
        if (widget == movingWidget && isArrowKey(event)) {
            movingWidget = null;
            provider.movementFinished(widget);
            return State.CONSUMED;
        }
        return State.REJECTED;
    }
    
    private boolean isArrowKey(WidgetKeyEvent event) {
        int code = event.getKeyCode();
        return code == KeyEvent.VK_UP ||
                code == KeyEvent.VK_DOWN ||
                code == KeyEvent.VK_LEFT ||
                code == KeyEvent.VK_RIGHT;
    }
    
    
    
    
    
    
}
