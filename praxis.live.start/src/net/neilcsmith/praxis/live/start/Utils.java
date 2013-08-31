/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.start;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbPreferences;

/**
 *
 * @author Neil C Smith
 */
class Utils {
    
    private Utils(){}

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
        return NbPreferences.forModule(Utils.class).getBoolean("showStart", true);
    }
    
    static void setShowStart(boolean show) {
        NbPreferences.forModule(Utils.class).putBoolean("showStart", show);
    }
    
}
