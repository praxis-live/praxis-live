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
package org.praxislive.ide.core.ui.api;

import javax.swing.ActionMap;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 * Utility class for working with global actions.
 * <p>
 * For action keys, register the performer action with the key into the active
 * component's {@link ActionMap} to link to the globally registered action.
 */
@NbBundle.Messages({
    "CTL_DuplicateAction=Duplicate",
    "CTL_SelectAllAction=Select All",
    "CTL_SelectNoneAction=Select None",
    "CTL_ZoomInAction=Zoom In",
    "CTL_ZoomOutAction=Zoom Out",
    "CTL_ZoomResetAction=Reset Zoom"
})
public class Actions {

    /**
     * Duplicate action (key).
     */
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
    public static final String DUPLICATE_KEY = "duplicate";

    /**
     * Select All action (key).
     */
    @ActionID(
            category = "Edit",
            id = "org.praxislive.ide.core.ui.api.Actions.SelectAll"
    )
    @ActionRegistration(
            displayName = "#CTL_SelectAllAction",
            iconBase = "org/praxislive/ide/core/ui/resources/select-all.png"
    )
    @ActionReferences({
        @ActionReference(path = "Menu/Edit", position = 1430),
        @ActionReference(path = "Shortcuts", name = "D-A")
    })
    public static final String SELECT_ALL_KEY = "select-all";

    /**
     * Select None action (key).
     */
    @ActionID(
            category = "Edit",
            id = "org.praxislive.ide.core.ui.api.Actions.SelectNone"
    )
    @ActionRegistration(
            displayName = "#CTL_SelectNoneAction",
            iconBase = "org/praxislive/ide/core/ui/resources/select-none.png"
    )
    @ActionReferences({
        @ActionReference(path = "Menu/Edit", position = 1435),
        @ActionReference(path = "Shortcuts", name = "DS-A")
    })
    public static final String SELECT_NONE_KEY = "select-none";

    /**
     * Zoom In action (key).
     */
    @ActionID(
            category = "View",
            id = "org.praxislive.ide.core.ui.api.Actions.ZoomIn"
    )
    @ActionRegistration(
            displayName = "#CTL_ZoomInAction",
            iconBase = "org/praxislive/ide/core/ui/resources/zoom-in.png"
    )
    @ActionReferences({
        @ActionReference(path = "Shortcuts", name = "D-ADD"),
        @ActionReference(path = "Shortcuts", name = "D-EQUALS"),
        @ActionReference(path = "Shortcuts", name = "D-PLUS")
    })
    public static final String ZOOM_IN_KEY = "zoom-in";

    /**
     * Zoom Out action (key).
     */
    @ActionID(
            category = "View",
            id = "org.praxislive.ide.core.ui.api.Actions.ZoomOut"
    )
    @ActionRegistration(
            displayName = "#CTL_ZoomOutAction",
            iconBase = "org/praxislive/ide/core/ui/resources/zoom-out.png"
    )
    @ActionReferences({
        @ActionReference(path = "Shortcuts", name = "D-MINUS"),
        @ActionReference(path = "Shortcuts", name = "D-SUBTRACT")
    })
    public static final String ZOOM_OUT_KEY = "zoom-out";

    /**
     * Zoom Reset action (key).
     */
    @ActionID(
            category = "View",
            id = "org.praxislive.ide.core.ui.api.Actions.ZoomReset"
    )
    @ActionRegistration(
            displayName = "#CTL_ZoomResetAction",
            iconBase = "org/praxislive/ide/core/ui/resources/zoom-reset.png"
    )
    public static final String ZOOM_RESET_KEY = "zoom-reset";

    private Actions() {
    }

}
