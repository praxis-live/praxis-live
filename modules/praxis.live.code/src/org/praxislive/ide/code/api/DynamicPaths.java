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
package org.praxislive.ide.code.api;

import java.util.Objects;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.praxislive.ide.code.PathRegistry;
import org.praxislive.ide.project.api.PraxisProject;

/**
 * Service for registering dynamic paths in the IDE, usually for in-memory
 * source files. This enables queries for classpath, source level, etc. across
 * these files. Each registration method returns a private {@link #Key} that can
 * be used to unregister the sources. Sources should be unregistered when no
 * longer required or a strong reference will be held to them.
 */
public final class DynamicPaths {

    private static final DynamicPaths INSTANCE = new DynamicPaths();

    private DynamicPaths() {

    }

    /**
     * Register a folder for shared code files. The returned key provides access
     * to {@link SharedCodeInfo} that can be used for registering dependent
     * files, or for adding to a Lookup to use for browsing and editing shared
     * sources.
     * <p>
     * The root folder is the classpath root of the file system, eg. the default
     * package location. The shared folder must be the root or a subfolder of
     * the root for a particular package (eg. SHARED).
     * <p>
     * Non-source classpaths for the files, as well as other queries, will
     * delegate to the provided project.
     *
     * @param project the project
     * @param root root folder of the classpath
     * @param sharedFolder folder for all shared files
     * @return key for access to info and unregistering
     */
    public SharedKey registerShared(PraxisProject project, FileObject root, FileObject sharedFolder) {
        Objects.requireNonNull(project);
        Objects.requireNonNull(root);
        Objects.requireNonNull(sharedFolder);
        if (root != sharedFolder && !FileUtil.isParentOf(root, sharedFolder)) {
            throw new IllegalArgumentException("Shared folder must be under root folder");
        }
        PathRegistry.getDefault().register(project, root);
        return new SharedKey(project, root, sharedFolder);
    }

    /**
     * Register a source root without dependency on shared code. See
     * {@link #register(org.praxislive.ide.project.api.PraxisProject, org.praxislive.ide.code.api.SharedCodeInfo, org.openide.filesystems.FileObject)}.
     *
     * @param project the project
     * @param root root folder
     * @return key for unregistering
     */
    public Key register(PraxisProject project, FileObject root) {
        return register(project, root, null);
    }

    /**
     * Register a source root with optional dependency on shared code. The
     * source root should be the classpath root, for example the file system
     * root of a memory file system corresponding to the default package.
     * <p>
     * Non-source classpaths for the root and its files, as well as other
     * queries, will delegate to the provided project.
     *
     * @param project the project
     * @param root root folder
     * @param shared optional shared code, may be null
     * @return
     */
    public Key register(PraxisProject project, FileObject root, SharedCodeInfo shared) {
        PathRegistry.getDefault().register(project, root, shared);
        return new Key(project, root);
    }

    public static DynamicPaths getDefault() {
        return INSTANCE;
    }

    /**
     * A key for unregistering a code path.
     */
    public static class Key {

        private final PraxisProject project;
        private final FileObject root;

        private Key(PraxisProject project, FileObject root) {
            this.project = project;
            this.root = root;
        }

        /**
         * Unregister the root folder related to this key.
         */
        public void unregister() {
            PathRegistry.getDefault().unregister(project, root);
        }

    }

    /**
     * A key for accessing shared code info and unregistering a shared code
     * path.
     */
    public static class SharedKey extends Key {

        private final SharedCodeInfo info;

        private SharedKey(PraxisProject project, FileObject root, FileObject sharedFolder) {
            super(project, root);
            info = new SharedCodeInfo(root, sharedFolder);
        }

        /**
         * Shared code info for the corresponding shared code path.
         *
         * @return shared code info
         */
        public SharedCodeInfo info() {
            return info;
        }

    }

}
