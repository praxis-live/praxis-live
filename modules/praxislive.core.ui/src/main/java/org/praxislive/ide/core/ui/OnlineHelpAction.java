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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.core.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

@ActionID(
        category = "Help",
        id = "org.praxislive.ide.core.ui.OnlineHelpAction"
)
@ActionRegistration(
        displayName = "#CTL_OnlineHelpAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Help", position = 100),
    @ActionReference(path = "Shortcuts", name = "F1")
})
@Messages("CTL_OnlineHelpAction=Online Help")
@ServiceProvider(service = HelpCtx.Displayer.class, position = 1)
public final class OnlineHelpAction implements ActionListener, HelpCtx.Displayer {

    @Override
    public void actionPerformed(ActionEvent e) {
        Utils.openExternalLink(Utils.DOCUMENTATION_LINK);
    }

    @Override
    public boolean display(HelpCtx help) {
        actionPerformed(null);
        return true;
    }
}
