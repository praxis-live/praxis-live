/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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

package org.praxislive.ide.project.api;

import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
// @TODO v4 make interface and return lists
public abstract class PraxisProjectProperties {
    
    // @TODO v4 change name and string PROP_FILES - "files"?
    public final static String PROP_FILES_CHANGED = "filesChanged";
    public final static String PROP_LIBRARIES = "libraries";
    
    @Deprecated
    public abstract boolean addProjectFile(ExecutionLevel level, FileObject file);
    
    @Deprecated
    public abstract boolean removeProjectFile(ExecutionLevel level, FileObject file);

    public abstract FileObject[] getProjectFiles(ExecutionLevel level);

    public List<FileObject> getFiles(ExecutionLevel level) {
        return Arrays.asList(getProjectFiles(level));
    }
    
    public List<FileObject> getLibraries() {
        return Collections.EMPTY_LIST;
    }
    
    public abstract void addPropertyChangeListener(PropertyChangeListener listener);

    public abstract void removePropertyChangeListener(PropertyChangeListener listener);

}
