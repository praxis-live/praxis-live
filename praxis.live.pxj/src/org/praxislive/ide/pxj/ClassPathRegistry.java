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
package org.praxislive.ide.pxj;

import java.io.File;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;
import org.openide.modules.OnStart;
import org.openide.util.Exceptions;
import org.praxislive.ide.core.embedder.CORE;

/**
 *
 */
public class ClassPathRegistry {

    private final static System.Logger LOG
            = System.getLogger(ClassPathRegistry.class.getName());

    private final static ClassPathRegistry INSTANCE = new ClassPathRegistry();

    private ClassPath classPath;
    private ClassPath bootClassPath;

    private ClassPathRegistry() {
        init();
    }

    private void init() {
        LOG.log(Level.DEBUG, "Initializing compile classpath");
        try {
            File modDir = CORE.modulesDir();
            List<URL> jars = new ArrayList<>();
            for (File jar : modDir.listFiles()) {
                if (jar.getName().endsWith(".jar")) {
                    URL jarURL = FileUtil.urlForArchiveOrDir(jar);
                    LOG.log(Level.DEBUG, "Adding {0} to compile classpath", jarURL);
                    jars.add(jarURL);
                }
            }

            classPath = ClassPathSupport.createClassPath(jars.toArray(
                    new URL[jars.size()]));
            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE,
                    new ClassPath[]{classPath});
            
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            classPath = ClassPath.EMPTY;
        }

        bootClassPath = JavaPlatform.getDefault().getBootstrapLibraries();
        GlobalPathRegistry.getDefault().register(ClassPath.BOOT, new ClassPath[]{bootClassPath});

    }

    ClassPath getCompileClasspath() {
        return classPath;
    }

    ClassPath getBootClasspath() {
        return bootClassPath;
    }

    static ClassPathRegistry getInstance() {
        return INSTANCE;
    }

    @OnStart
    public static class Initializer implements Runnable {

        @Override
        public void run() {
            getInstance();
        }

    }

}
