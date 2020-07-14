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

package org.praxislive.ide.project.api;

import java.beans.PropertyChangeListener;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 *
 */
public interface ProjectProperties {
    
    public final static String PROP_ELEMENTS = "elements";
    public final static String PROP_LIBRARIES = "libraries";
    public final static String PROP_JAVA_RELEASE = "java-release";
    
    
    public void setElements(ExecutionLevel level, List<ExecutionElement> elements);
    
    public List<ExecutionElement> getElements(ExecutionLevel level);
    
    public PraxisProject getProject();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);

    public void removePropertyChangeListener(PropertyChangeListener listener);
    
    public default void addFile(ExecutionLevel level, FileObject file) {
        var list = getElements(level);
        list.add(ExecutionElement.forFile(file));
        setElements(level, list);
    }
    
    public default boolean removeFile(ExecutionLevel level, FileObject file) {
        var list = getElements(level);
        if (list.removeIf(e -> e instanceof ExecutionElement.File &&
                ((ExecutionElement.File) e).file().equals(file))) {
            setElements(level, list);
            return true;
        } else {
            return false;
        }
    }
    
    public default void addLine(ExecutionLevel level, String line) {
        var list = getElements(level);
        list.add(ExecutionElement.forLine(line));
        setElements(level, list);
    }
    
    public default boolean removeLine(ExecutionLevel level, String line) {
        var list = getElements(level);
        if (list.removeIf(e -> e instanceof ExecutionElement.Line &&
                ((ExecutionElement.Line) e).line().equals(line))) {
            setElements(level, list);
            return true;
        } else {
            return false;
        }
    }
    
    public default List<FileObject> getLibraries() {
        return List.of();
    }
    
    public default int getJavaRelease() {
        return 11;
    }
    
    

}
