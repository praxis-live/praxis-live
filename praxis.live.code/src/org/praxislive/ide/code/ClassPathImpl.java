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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.code;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 */
@ServiceProvider(service = ClassPathProvider.class)
public class ClassPathImpl implements ClassPathProvider {
    

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        var info = PathRegistry.getDefault().findInfo(file);
        if (info != null) {
            if (ClassPath.SOURCE.equals(type)) {
                return info.classpath();
            } else {
                var cpp = info.project().getLookup().lookup(ClassPathProvider.class);
                if (cpp != null) {
                    return cpp.findClassPath(file, type);
                }
            }
        }
        return null;
    }

}
