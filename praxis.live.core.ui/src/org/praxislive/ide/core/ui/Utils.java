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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.core.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.prefs.Preferences;
import org.praxislive.ide.core.Core;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Neil C Smith
 */
class Utils {
    
    final static URI WEBSITE_LINK = 
            URI.create("http://www.praxislive.org");
    final static URI DOWNLOAD_LINK = 
            WEBSITE_LINK.resolve("/download/");
    final static URI DOCUMENTATION_LINK = 
            WEBSITE_LINK.resolve("/documentation/");
    final static URI ISSUES_LINK = 
            WEBSITE_LINK.resolve("/issues/");
    
    final static String START_PAGE_KEY = 
            "start-page";
    
    private final static Preferences CORE_PREFS =
            Core.getInstance().getInternalPreferences();
    
    private Utils(){}

    static void openExternalLink(URL link) {
        try {
            openExternalLink(link.toURI());
        } catch (URISyntaxException ex) {
            DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message("Unable to open link " + link,
                NotifyDescriptor.ERROR_MESSAGE));
        }
    }
    
    static void openExternalLink(URI link) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(link);
                return;
            } catch (IOException ex) {
                // fall through
            }
        }
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message("Unable to open link " + link,
                NotifyDescriptor.ERROR_MESSAGE));
    }
    
    static boolean isShowStart() {
        return CORE_PREFS.getBoolean("show-start", true);
    }
    
    static void setShowStart(boolean show) {
        CORE_PREFS.putBoolean("show-start", show);
    }
    
    static boolean isCheckForUpdates() {
        return CORE_PREFS.getBoolean("check-for-updates", true);
    }
    
    static void setCheckForUpdates(boolean check) {
        CORE_PREFS.putBoolean("check-for-updates", check);
    }
    
    
}
