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
package org.praxislive.ide.core.api;

import java.util.prefs.Preferences;
import org.praxislive.ide.core.Core;

/**
 * Various utility methods for accessing information and configuring the IDE.
 */
public final class IDE {

    private IDE() {
    }

    /**
     * The running version of the IDE.
     *
     * @return current version
     */
    public static String getVersion() {
        return Core.getInstance().getVersion();
    }

    /**
     * The latest available version of the IDE. This information relies on
     * online checking. Will return {@link #getVersion()} if no additional
     * information is available.
     *
     * @return latest available version
     */
    public static String getLatestAvailableVersion() {
        return Core.getInstance().getLatestAvailableVersion();
    }

    /**
     * Get the global preferences.
     *
     * @return global preferences
     */
    public static Preferences getPreferences() {
        return Core.getInstance().getPreferences();
    }

}
