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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.code.api;

import org.openide.filesystems.FileObject;

/**
 * Provide access to the Shared Code files for a component hierarchy. Instances
 * should be retrieved from the container lookup (eg. RootProxy).
 * <p>
 * To create a SharedCodeInfo, register a folder and classpath root with
 * {@link DynamicPaths#registerShared(org.praxislive.ide.project.api.PraxisProject, org.openide.filesystems.FileObject, org.openide.filesystems.FileObject)}
 * and obtain the info from {@link DynamicPaths.SharedKey#info}.
 */
public final class SharedCodeInfo {
    
    private final FileObject root;
    private final FileObject folder;
    
    SharedCodeInfo(FileObject root, FileObject folder) {
        this.root = root;
        this.folder = folder;
    }
    

    /**
     * The folder which contains all shared code files. Likely to be on a memory
     * file system.
     *
     * @return shared code folder
     */
    public FileObject getFolder() {
        return folder;
    }
    
    
    
}
