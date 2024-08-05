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
package org.praxislive.ide.pxr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 *
 */
@ActionID(category = "PXR", id = "org.praxislive.ide.pxr.RootConfigAction")
@ActionRegistration(
        displayName = "#CTL_RootConfigAction",
        iconBase = "org/praxislive/ide/pxr/resources/properties.png"
)
@Messages("CTL_RootConfigAction=Configure root")
public class RootConfigAction implements ActionListener {

    private final ActionEditorContext context;

    public RootConfigAction(ActionEditorContext context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Action action = context.root().getNodeDelegate().getPreferredAction();
        action.actionPerformed(ae);
    }
}
