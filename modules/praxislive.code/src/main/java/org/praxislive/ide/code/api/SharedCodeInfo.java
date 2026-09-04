/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
package org.praxislive.ide.code.api;

import java.util.Objects;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Provide access to the Shared Code files for a component hierarchy. Instances
 * should be retrieved from the container lookup (eg. RootProxy).
 * <p>
 * A SharedCodeInfo can be created directly, or by registering a classpath root
 * with
 * {@link DynamicPaths#registerShared(org.praxislive.ide.project.api.PraxisProject, org.openide.filesystems.FileObject, org.openide.filesystems.FileObject)}
 * and obtaining the info from {@link DynamicPaths.SharedKey#info}.
 */
public final class SharedCodeInfo {

    private final Project project;
    private final FileObject root;
    private final FileObject folder;

    /**
     * Create a SharedCodeInfo for the given project and shared code root.
     *
     * @param project project
     * @param root shared code root
     */
    public SharedCodeInfo(Project project, FileObject root) {
        this(project, root, null);
    }

    SharedCodeInfo(Project project, FileObject root, FileObject folder) {
        this.project = Objects.requireNonNull(project);
        this.root = Objects.requireNonNull(root);
        this.folder = folder;
    }

    /**
     * The project the shared code is from.
     *
     * @return project
     */
    public Project project() {
        return project;
    }

    /**
     * The root folder for shared code files. This is the root folder of the
     * package tree.
     *
     * @return shared code root
     */
    public FileObject root() {
        return root;
    }

    /**
     * Query whether the shared code files are within the project file tree.
     * Shared code files opened in a memory file system will return
     * {@code false} from this method.
     *
     * @return true if shared code files within project tree
     */
    public boolean isInProjectFiles() {
        return FileUtil.isParentOf(project.getProjectDirectory(), root);
    }

    /**
     * The folder which contains all shared code files. The same or a subfolder
     * of root.
     *
     * @return shared code folder
     */
    @Deprecated
    public FileObject getFolder() {
        return folder == null ? root : folder;
    }

}
