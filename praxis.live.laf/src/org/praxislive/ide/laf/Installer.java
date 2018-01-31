/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.laf;

import java.util.prefs.Preferences;
import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.ImageUtilities;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

//    @Override
//    public void validate() throws IllegalStateException {
//        Preferences prefs = getPreferences();
//        prefs.put("laf", PraxisLiveLookAndFeel.class.getName());
//        prefs.putBoolean("dark.themes.installed", true); //NOI18N
//        UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo("Praxis LIVE", PraxisLiveLookAndFeel.class.getName()));
//    }

    @Override
    public void restored() {

        System.setProperty("netbeans.ps.hideSingleExpansion", "true");
        System.setProperty("ps.quickSearch.disabled.global", "true");

        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {

            @Override
            public void run() {
                WindowManager wm = WindowManager.getDefault();
                TopComponent tc = wm.findTopComponent("projectTabLogical_tc");
                tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/project/ui/resources/projectTab.png", true));
                tc = wm.findTopComponent("CommonPalette");
                tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/palette/resources/palette.png", true));
            }
        });

    }

    private Preferences getPreferences() {
        return NbPreferences.root().node("laf");
    }
}
