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
package org.praxislive.ide.core.embedder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.BaseUtilities;
import org.openide.util.Lookup;

/**
 * Utility class for locating bundled PraxisCORE runtime. A {@link Locator} may
 * be registered in the global lookup to override the default locator.
 *
 * The default locator expects the runtime to be packaged inside a
 * {@code praxiscore} directory adjacent to the cluster containing this module.
 * The default locator expects the CORE modules to be inside a {@code mods}
 * directory inside the install directory. The default locator expects the
 * launcher to be within a {@code bin} directory inside the install directory,
 * and named {@code praxis} (Linux/macOS) or {@code praxis.cmd} (Windows).
 */
public final class CORE {

    private CORE() {
        // static utility class
    }

    /**
     * Locate the launcher file.
     *
     * @return path to launcher
     * @throws IOException if launcher not found
     */
    public static Path launcherFile() throws IOException {
        return locator().findLauncher();
    }

    /**
     * Locate the modules directory.
     *
     * @return path to modules directory
     * @throws IOException if modules not found
     */
    public static Path modulesDir() throws IOException {
        return locator().findModulesDir();
    }

    /**
     * Locate the CORE install directory.
     *
     * @return path to install directory
     * @throws IOException if CORE installation not found
     */
    public static Path installDir() throws IOException {
        return locator().findInstallDir();
    }

    private static Locator locator() {
        Locator locator = Lookup.getDefault().lookup(Locator.class);
        return locator == null ? DefaultLocator.INSTANCE : locator;
    }

    /**
     * Service provider interface for locating the CORE installation, modules
     * and launcher file.
     */
    public static interface Locator {

        /**
         * Locate the launcher file.
         *
         * @return path to launcher
         * @throws IOException if launcher not found
         */
        public Path findLauncher() throws IOException;

        /**
         * Locate the modules directory.
         *
         * @return path to modules directory
         * @throws IOException if modules not found
         */
        public Path findModulesDir() throws IOException;

        /**
         * Locate the CORE install directory.
         *
         * @return path to install directory
         * @throws IOException if CORE installation not found
         */
        public Path findInstallDir() throws IOException;

    }

    private static final class DefaultLocator implements Locator {

        private static final DefaultLocator INSTANCE = new DefaultLocator();

        @Override
        public Path findLauncher() throws IOException {
            Path installDir = findInstallDir();
            Path binDir = installDir.resolve("bin");
            Path launcher;
            if (BaseUtilities.isWindows()) {
                launcher = binDir.resolve("praxis.cmd");
            } else {
                launcher = binDir.resolve("praxis");
            }
            if (Files.exists(launcher)) {
                return launcher;
            } else {
                throw new IOException("No CORE launcher found");
            }
        }

        @Override
        public Path findModulesDir() throws IOException {
            Path installDir = findInstallDir();
            Path modulesDir = installDir.resolve("mods");
            if (Files.isDirectory(modulesDir)) {
                return modulesDir;
            } else {
                throw new IOException("No CORE modules directory found");
            }
        }

        @Override
        public Path findInstallDir() throws IOException {
            try {
                File modDir = InstalledFileLocator.getDefault().
                        locate("modules", "org.praxislive.ide.core.embedder", false);
                Path installDir = modDir.toPath().getParent().getParent();
                Path coreDir = installDir.resolve("praxiscore");
                if (Files.isDirectory(coreDir)) {
                    return coreDir;
                }
            } catch (Exception ex) {
                throw new IOException("No embedded praxiscore directory found", ex);
            }
            throw new IOException("No embedded praxiscore directory found");
        }

    }

}
