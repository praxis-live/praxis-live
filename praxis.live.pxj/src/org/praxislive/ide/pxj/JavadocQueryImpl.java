/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith - http://www.neilcsmith.net
 */
@ServiceProvider(service = JavadocForBinaryQueryImplementation.class)
public class JavadocQueryImpl implements JavadocForBinaryQueryImplementation {

    static final URL JAVADOC_ARCHIVE; 
    private static final JavadocForBinaryQuery.Result RESULT;

    static {
        File javadocZip = InstalledFileLocator.getDefault()
                .locate("docs/core-javadoc.zip", "org.praxislive.ide.pxj", true);
        if (javadocZip != null) {
            JAVADOC_ARCHIVE = FileUtil.urlForArchiveOrDir(javadocZip);
            RESULT = new JavadocForBinaryQuery.Result() {
                @Override
                public URL[] getRoots() {
                    return new URL[]{JAVADOC_ARCHIVE};
                }

                @Override
                public void addChangeListener(ChangeListener l) {
                }

                @Override
                public void removeChangeListener(ChangeListener l) {
                }
                
            };
        } else {
            JAVADOC_ARCHIVE = null;
            RESULT = null;
        }
    }


    @Override
    public JavadocForBinaryQuery.Result findJavadoc(URL binaryRoot) {
        if (binaryRoot.toString().contains("praxis")) {
            return RESULT;
        } else {
            return null;
        }
    }

}
