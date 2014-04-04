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

import java.io.IOException;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith
 */
@ServiceProvider(service = ClassPathProvider.class)
public class ClassPathImpl implements ClassPathProvider {

    @Override
    public ClassPath findClassPath(FileObject file, String type) {
        if (file.getAttribute(PXJDataObject.PXJ_DOB_KEY) instanceof PXJDataObject) {
            switch (type) {
                case ClassPath.COMPILE:
                    return ClassPathRegistry.getInstance().getCompileClasspath();
                case ClassPath.SOURCE:
                    Object srcCP = file.getAttribute("source.classpath");
                    if (srcCP instanceof ClassPath) {
                        return (ClassPath) srcCP;
                    } else {
                        ClassPath cp = ClassPathSupport.createClassPath(file.getParent());
                        try {
                            file.setAttribute("source.classpath", cp);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                        return cp;
                    }
            }
        }
        return null;

    }

}
