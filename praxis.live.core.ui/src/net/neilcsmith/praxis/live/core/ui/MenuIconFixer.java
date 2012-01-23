/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.core.ui;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.openide.awt.Actions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
@ServiceProvider(service=Actions.ButtonActionConnector.class)
public class MenuIconFixer implements Actions.ButtonActionConnector {

    @Override
    public boolean connect(AbstractButton button, Action action) {
        return false;
    }

    @Override
    public boolean connect(JMenuItem item, Action action, boolean popup) {
        Object icon = action.getValue(Action.SMALL_ICON);
        Object base = action.getValue("iconBase");
        if (base instanceof String) {
            if (icon != null) {
                action.putValue(Action.SMALL_ICON, null);
            }
        }
        return false;
    }
    
}
