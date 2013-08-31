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

import org.openide.modules.ModuleInstall;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.windows.WindowSystemEvent;
import org.openide.windows.WindowSystemListener;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        WindowManager.getDefault().addWindowSystemListener(new WindowSystemListener() {
            @Override
            public void beforeLoad(WindowSystemEvent event) {
            }

            @Override
            public void afterLoad(WindowSystemEvent event) {
            }

            @Override
            public void beforeSave(WindowSystemEvent event) {
                boolean show = Utils.isShowStart();
                TopComponent start = WindowManager.getDefault().findTopComponent("StartTopComponent");
                if (start != null) {
                    if (show) {
                        start.open();
                        start.requestActive();
                    } else {
                        start.close();
                    }
                }

            }

            @Override
            public void afterSave(WindowSystemEvent event) {
            }
        });
    }
}
