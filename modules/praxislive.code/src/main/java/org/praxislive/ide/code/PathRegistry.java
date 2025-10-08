/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.code;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.praxislive.ide.code.api.SharedCodeInfo;
import org.praxislive.ide.project.api.PraxisProject;

public class PathRegistry {

    private static final PathRegistry INSTANCE = new PathRegistry();

    private final Map<FileObject, Info> registry;

    private PathRegistry() {
        registry = new HashMap<>();
    }

    public void register(PraxisProject project, FileObject root) {
        registerImpl(project, root, null);
    }

    public void register(PraxisProject project, FileObject root, SharedCodeInfo shared) {
        registerImpl(project, root, shared);
    }

    public void unregister(PraxisProject project, FileObject root) {
        unregisterImpl(project, root);
    }

    synchronized Info findInfo(FileObject file) {
        for (var f = file; f != null; f = f.getParent()) {
            var info = registry.get(f);
            if (info != null) {
                return info;
            }
        }
        return null;
    }

    private synchronized void registerImpl(PraxisProject project, FileObject root, SharedCodeInfo shared) {

        var classpath = ClassPathSupport.createClassPath(root);

        if (shared != null) {
            var sharedInfo = findInfo(shared.getFolder());
            if (sharedInfo != null) {
                classpath = ClassPathSupport.createProxyClassPath(classpath,
                        sharedInfo.classpath);
            }
        }
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[]{classpath});
        var info = new Info(project, classpath);
        registry.put(root, info);

    }

    private synchronized void unregisterImpl(Project project, FileObject root) {
        var info = registry.remove(root);
        if (info != null) {
            GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE,
                    new ClassPath[]{info.classpath()});
        }
    }

    public static PathRegistry getDefault() {
        return INSTANCE;
    }

    static class Info {

        private final PraxisProject project;
        private final ClassPath classpath;

        public Info(PraxisProject project, ClassPath classpath) {
            this.project = project;
            this.classpath = classpath;
        }

        ClassPath classpath() {
            return classpath;
        }

        PraxisProject project() {
            return project;
        }

    }

}
