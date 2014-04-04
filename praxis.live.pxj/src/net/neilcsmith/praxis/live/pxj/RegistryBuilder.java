/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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


package net.neilcsmith.praxis.live.pxj;

import java.io.File;
import java.util.logging.Logger;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.OnStart;

/**
 *
 * @author Neil C Smith
 */
@OnStart
public class RegistryBuilder implements Runnable {
   

    @Override
    public void run() {
        File mods = InstalledFileLocator.getDefault().locate("modules", "net.neilcsmith.praxis.core", false);
        if (mods != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Found files :\n");
            for (File mod : mods.listFiles()) {
                sb.append(mod.toURI());
                sb.append("\n");
            }
            sb.append("===============\n");
            Logger.getGlobal().warning(sb.toString());
        }
    }
    
}
