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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.core.api;

import java.util.prefs.Preferences;
import org.praxislive.ide.core.Core;

/**
 *
 */
public final class IDE {
    
    private IDE() {}
    
    public static String getVersion() {
        return Core.getInstance().getVersion();
    }

    public static String getLatestAvailableVersion() {
        return Core.getInstance().getLatestAvailableVersion();
    }
    
    public static Preferences getPreferences() {
        return Core.getInstance().getPreferences();
    }
    
}
