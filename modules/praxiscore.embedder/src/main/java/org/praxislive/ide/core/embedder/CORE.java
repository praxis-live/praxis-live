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
package org.praxislive.ide.core.embedder;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.OnStart;
import org.openide.util.BaseUtilities;
import org.openide.util.Exceptions;


/**
 *
 */
public final class CORE {

    private static final System.Logger LOG = 
            System.getLogger(CORE.class.getName());
    
    private CORE() {
        // static utility class
    }

    public static File launcherFile() throws IOException {
        File installDir = installDir();
        File binDir = new File(installDir, "bin");
        File launcher;
        if (BaseUtilities.isWindows()) {
            launcher = new File(binDir, "praxis.cmd");
        } else {
            launcher = new File(binDir, "praxis");
        }
        if (launcher.exists()) {
            return launcher;
        } else {
            throw new IOException("No CORE launcher found");
        }
    }
    
    public static File modulesDir() throws IOException {
        File installDir = installDir();
        File modulesDir = new File(installDir, "mods");
        if (modulesDir.isDirectory()) {
            return modulesDir;
        } else {
            throw new IOException("No CORE modules directory found");
        }
    }
    
    public static File installDir() throws IOException {
        File modDir = InstalledFileLocator.getDefault().
                locate("modules", "org.praxislive.ide.core.embedder", false);
        if (modDir == null) {
            throw new IOException("Invalid embedder module location");
        }
        File installDir = modDir.getParentFile().getParentFile();
        File coreDir = new File(installDir, "praxiscore");
        if (!coreDir.isDirectory()) {
            throw new IOException("No embedded praxiscore directory found");
        }
        return coreDir;
    }
    
    @OnStart
    public static class ClasspathEnvTask implements Runnable {

        @Override
        public void run() {
            try {
                File installDir = installDir();
                File modsDir = new File(installDir, "mods");
                List<String> files = new ArrayList<>();
                for (File module : modsDir.listFiles()) {
                    if (module.getName().endsWith(".jar")) {
                        LOG.log(Level.DEBUG, () -> 
                                "Adding " + module + " to compile classpath."
                        );
                        files.add(module.getAbsolutePath());
                    }
                }
                if (files.isEmpty()) {
                    return;
                }
                
                String modulepath = files.stream()
                        .collect(Collectors.joining(File.pathSeparator));
                
                LOG.log(Level.DEBUG, () -> 
                        "Setting compile classpath to :\n" + modulepath);
                
                System.setProperty("jdk.module.path", modulepath);
                
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            
        }
        
    }
    
}
