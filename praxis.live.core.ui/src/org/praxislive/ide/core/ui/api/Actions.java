/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.core.ui.api;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Utility class for working with global actions.
 */
public class Actions {

    @ActionID(
            category = "Edit",
            id = "org.praxislive.ide.core.ui.api.Actions.Duplicate"
    )
    @ActionRegistration(
            displayName = "#CTL_DuplicateAction"
    )
    @ActionReferences({
        @ActionReference(path = "Menu/Edit", position = 1380),
        @ActionReference(path = "Shortcuts", name = "D-D")
    })
    @NbBundle.Messages("CTL_DuplicateAction=Duplicate")
    public static final String DUPLICATE_KEY = "duplicate";

    private Actions() {
    }

}
