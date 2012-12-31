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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.neilcsmith.praxis.live.core.DefaultHubManager;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "System",
id = "net.neilcsmith.praxis.live.core.ui.RestartHubAction")
@ActionRegistration(iconBase = "net/neilcsmith/praxis/live/core/ui/restart.png",
displayName = "#CTL_RestartHubAction")
@ActionReferences({
    @ActionReference(path = "Menu/BuildProject", position = 302),
    @ActionReference(path = "Toolbars/Build", position = 1)
})
@Messages("CTL_RestartHubAction=Restart Hub")
public final class RestartHubAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        DefaultHubManager.getInstance().restart();
    }
}
