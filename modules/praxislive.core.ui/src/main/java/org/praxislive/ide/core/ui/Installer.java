/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
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

import javax.swing.UIManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 */
public class Installer extends ModuleInstall {

    @Override
    public void validate() throws IllegalStateException {
        System.setProperty("netbeans.ps.hideSingleExpansion", "true");
        System.setProperty("ps.quickSearch.disabled.global", "true");
    }

    @Override
    public void restored() {
        WindowManager.getDefault().invokeWhenUIReady(this::configureUI);
    }

    private void configureUI() {
        var defs = UIManager.getDefaults();
        var font = defs.getFont("Label.font");
        if (font != null) {
            int size = font.getSize();
            defs.put("netbeans.ps.rowheight", size * 2);
        }

        WindowManager wm = WindowManager.getDefault();
        TopComponent tc = wm.findTopComponent("projectTabLogical_tc");
        tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/project/ui/resources/projectTab.png", true));
        tc = wm.findTopComponent("CommonPalette");
        tc.setIcon(ImageUtilities.loadImage("org/netbeans/modules/palette/resources/palette.png", true));
    }

}
