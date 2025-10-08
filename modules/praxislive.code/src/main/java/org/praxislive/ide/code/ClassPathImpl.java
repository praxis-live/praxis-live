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

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ClassPathProvider.class)
public final class ClassPathImpl implements ClassPathProvider {

    public ClassPathImpl() {
    }

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        PathRegistry.Info info = PathRegistry.getDefault().findInfo(file);
        if (info != null) {
            if (ClassPath.SOURCE.equals(type)) {
                return info.classpath();
            } else {
                ClassPathProvider cpp = info.project().getLookup().lookup(ClassPathProvider.class);
                if (cpp != null) {
                    return cpp.findClassPath(file, type);
                }
            }
        }
        return null;
    }

}
