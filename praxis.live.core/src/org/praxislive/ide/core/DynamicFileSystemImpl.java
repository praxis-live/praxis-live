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
package org.praxislive.ide.core;

import java.util.LinkedHashSet;
import org.praxislive.ide.core.api.DynamicFileSystem;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.MultiFileSystem;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

/**
 *
 */
@ServiceProviders ({
    @ServiceProvider(service = FileSystem.class),
    @ServiceProvider(service = DynamicFileSystem.class)
})
public class DynamicFileSystemImpl extends MultiFileSystem
        implements DynamicFileSystem {
    
    private final LinkedHashSet<FileSystem> mounted;
    
    public DynamicFileSystemImpl() {
        setPropagateMasks(true);
        mounted = new LinkedHashSet<>();
    }

    @Override
    public void mount(FileSystem fs) {
        if (fs == null) {
            throw new NullPointerException();
        }
        if (mounted.add(fs)) {
            refresh();
        }
    }

    @Override
    public void unmount(FileSystem fs) {
        if (mounted.remove(fs)) {
            refresh();
        }
    }

    @Override
    public boolean isMounted(FileSystem fs) {
        return mounted.contains(fs);
    }
    
    private void refresh() {
        setDelegates(mounted.toArray(new FileSystem[mounted.size()]));
    }
    
}
