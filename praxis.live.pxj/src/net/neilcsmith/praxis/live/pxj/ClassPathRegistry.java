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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.OnStart;

/**
 *
 * @author Neil C Smith
 */
public class ClassPathRegistry {

    private final static Logger LOG = Logger.getLogger(ClassPathRegistry.class.getName());
    
    private final static ClassPathRegistry INSTANCE = new ClassPathRegistry();
    
    private ClassPath classPath;

    private ClassPathRegistry() {
        init();
    }

    private void init() {
        LOG.log(Level.FINE, "Initializing compile classpath");
        File modDir = InstalledFileLocator.getDefault()
                .locate("modules", "net.neilcsmith.praxis.core", false);
        if (modDir != null && modDir.isDirectory()) {
             List<URL> jars = new ArrayList<>();
            for (File jar : modDir.listFiles()) {
                if (jar.getName().endsWith(".jar")) {
                    URL jarURL = FileUtil.urlForArchiveOrDir(jar);
                    LOG.log(Level.FINE, "Adding {0} to compile classpath", jarURL);
                    jars.add(jarURL);
                }
            }

            File ext = new File(modDir, "ext");
            if (ext.isDirectory()) {
                for (File jar : ext.listFiles()) {
                    if (jar.getName().endsWith(".jar")) {
                        URL jarURL = FileUtil.urlForArchiveOrDir(jar);
                        LOG.log(Level.FINE, "Adding {0} to compile classpath", jarURL);
                        jars.add(jarURL);
                    }
                }
            }
            classPath = ClassPathSupport.createClassPath(jars.toArray(
                    new URL[jars.size()]));  
            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE,
                    new ClassPath[]{classPath});
        } else {
            classPath = ClassPath.EMPTY;
        }
        
    }
    
    ClassPath getCompileClasspath() {
        return classPath;       
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
