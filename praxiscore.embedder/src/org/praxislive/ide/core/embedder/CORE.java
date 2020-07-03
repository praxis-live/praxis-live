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
package org.praxislive.ide.core.embedder;

import java.io.File;
import java.io.IOException;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.BaseUtilities;

/**
 *
 */
public final class CORE {

    private CORE() {
        // static utility class
    }

    public static File launcherFile() throws IOException {
        File modDir = InstalledFileLocator.getDefault().
                locate("modules", "org.praxislive.ide.core.embedder", false);
        if (modDir == null) {
            throw new IOException("No modules directory found");
        }
        File installDir = modDir.getParentFile().getParentFile();
        File coreDir = new File(installDir, "praxiscore");
        if (!coreDir.isDirectory()) {
            throw new IOException("No embedded praxiscore directory found");
        }
        File binDir = new File(coreDir, "bin");
        File launcher;
        if (BaseUtilities.isWindows()) {
            launcher = new File(binDir, "praxis.bat");
        } else {
            launcher = new File(binDir, "praxis");
        }
        if (launcher.exists()) {
            return launcher;
        } else {
            throw new IOException("No launcher found");
        }
    }
    
}
