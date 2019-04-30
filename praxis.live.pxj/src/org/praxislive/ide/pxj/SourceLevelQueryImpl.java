/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
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

import javax.swing.event.ChangeListener;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith - https://www.neilcsmith.net
 */
@ServiceProvider(service = SourceLevelQueryImplementation2.class)
public class SourceLevelQueryImpl implements SourceLevelQueryImplementation2 {

    @Override
    public Result getSourceLevel(FileObject file) {
        Object o = file.getAttribute(PXJDataObject.PXJ_DOB_KEY);
        if (o instanceof PXJDataObject) {
            PXJDataObject dob = (PXJDataObject) o;
            return new ResultImpl(String.valueOf(dob.getJavaRelease()));
        }
        return null;
    }
    
    static class ResultImpl implements Result {

        private final String sourceLevel;
        
        private ResultImpl(String sourceLevel) {
            this.sourceLevel = sourceLevel;
        }
        
        @Override
        public String getSourceLevel() {
            return sourceLevel;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
        }
        
    }
    
}
