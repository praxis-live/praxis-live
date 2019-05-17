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

package org.praxislive.ide.project.api;

import java.beans.PropertyChangeListener;
import java.util.List;
import javax.lang.model.SourceVersion;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://www.neilcsmith.net)
 */
public interface PraxisProjectProperties {
    
    public final static String PROP_FILES = "files";
    public final static String PROP_LIBRARIES = "libraries";
    public final static String PROP_JAVA_RELEASE = "java-release";
    
    public boolean addFile(ExecutionLevel level, FileObject file);
    
    public boolean removeFile(ExecutionLevel level, FileObject file);

    public List<FileObject> getFiles(ExecutionLevel level);
    
    public List<FileObject> getLibraries();
    
    public default int getJavaRelease() {
        return 8;
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);

}
