/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.project.api;

import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.praxislive.ide.project.DefaultPraxisProject;

/**
 * Support for querying and configuring various aspects of a PraxisCORE project,
 * including execution elements, libraries and runtime. An implementation of
 * this interface should be obtained from the project lookup.
 */
public interface ProjectProperties {

    /**
     * Name of the property event fired when the list of elements changes.
     */
    public final static String PROP_ELEMENTS = "elements";

    /**
     * Name of the property event fired when the list of libraries changes.
     */
    public final static String PROP_LIBRARIES = "libraries";

    /**
     * Name of the property event fired when the Java release level changes.
     */
    public final static String PROP_JAVA_RELEASE = "java-release";

    /**
     * Set the execution elements for the specified execution level of the
     * project.
     *
     * @param level execution level
     * @param elements execution elements
     * @throws Exception
     */
    public void setElements(ExecutionLevel level, List<ExecutionElement> elements)
            throws Exception;

    /**
     * Get the execution elements for the specified execution level.
     *
     * @param level execution level
     * @return execution elements for level
     */
    public List<ExecutionElement> getElements(ExecutionLevel level);

    /**
     * Get the project.
     *
     * @return project
     */
    public PraxisProject getProject();

    /**
     * Add a property change listener.
     *
     * @param listener property change listener
     *
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remove a property change listener.
     *
     * @param listener property change listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Add a file execution element at the end of existing elements of the
     * execution level.
     *
     * @param level execution level
     * @param file file to register
     * @throws Exception
     */
    public default void addFile(ExecutionLevel level, FileObject file) throws Exception {
        var list = getElements(level);
        list.add(ExecutionElement.forFile(file));
        setElements(level, list);
    }

    /**
     * Remove the execution element(s) matching the specified file in the
     * execution level.
     *
     * @param level execution level
     * @param file file to remove
     * @return true if the file was found and the elements updated
     * @throws Exception
     */
    public default boolean removeFile(ExecutionLevel level, FileObject file) throws Exception {
        var list = getElements(level);
        if (list.removeIf(e -> e instanceof ExecutionElement.File
                && ((ExecutionElement.File) e).file().equals(file))) {
            setElements(level, list);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Add a line execution element at the end of the existing elements of the
     * execution level.
     *
     * @param level execution level
     * @param line line of Pcl script
     * @throws Exception
     */
    public default void addLine(ExecutionLevel level, String line) throws Exception {
        var list = getElements(level);
        list.add(ExecutionElement.forLine(line));
        setElements(level, list);
    }

    /**
     * Remove the line execution element(s) matching the specified script in the
     * execution element.
     *
     * @param level execution level
     * @param line line of Pcl script
     * @return true if the line was found and the list of elements updated
     * @throws Exception
     */
    public default boolean removeLine(ExecutionLevel level, String line) throws Exception {
        var list = getElements(level);
        if (list.removeIf(e -> e instanceof ExecutionElement.Line
                && ((ExecutionElement.Line) e).line().equals(line))) {
            setElements(level, list);
            return true;
        } else {
            return false;
        }
    }

    /**
     * The list of libraries added to the project.
     *
     * @return list of libraries
     */
    public default List<URI> getLibraries() {
        return List.of();
    }

    /**
     * Set the Java release version required by the project.
     *
     * @param release java release version
     * @throws Exception
     */
    public default void setJavaRelease(int release) throws Exception {
        throw new UnsupportedOperationException();
    }

    /**
     * Query the Java release version required by the project.
     *
     * @return java release version
     */
    public default int getJavaRelease() {
        return DefaultPraxisProject.MIN_JAVA_VERSION;
    }

}
