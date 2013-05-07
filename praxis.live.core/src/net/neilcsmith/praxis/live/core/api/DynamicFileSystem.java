/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.core.api;

import org.openide.filesystems.FileSystem;
import org.openide.filesystems.MultiFileSystem;
import org.openide.util.Lookup;

/**
 * Provides ability to insert and remove file systems dynamically from the 
 * configuration filesystem by providing a dynamic proxy.
 * 
 * @author Neil C Smith
 */

public abstract class DynamicFileSystem extends MultiFileSystem {
    
    /**
     * Insert filesystem into the dynamic configuration filesystem
     * @param fs
     */
    public abstract void mount(FileSystem fs);
    
    /**
     * Remove filesystem from the dynamic configuration filesystem
     * @param fs
     */
    public abstract void unmount(FileSystem fs);
    
    
    /**
     * Check whether a filesystem is already mounted in this dynamic filesystem.
     * Filesystems inserted into the configuration filesystem in other ways will
     * not register as mounted.
     * @param fs
     * @return
     */
    public abstract boolean isMounted(FileSystem fs);
    
    /**
     * Get access to the default implementation that has already been included
     * into the system configuration filesystem.
     * @return
     */
    public static DynamicFileSystem getDefault() {
        return Lookup.getDefault().lookup(DynamicFileSystem.class);
    }
    
}
