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
package org.praxislive.ide.code;

import java.net.MalformedURLException;
import java.net.URI;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.FileOwnerQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 */
@ServiceProvider(service = FileOwnerQueryImplementation.class)
public class ProjectQueryImpl implements FileOwnerQueryImplementation {
    
    @Override
    public Project getOwner(URI uri) {
        try {
            var file = URLMapper.findFileObject(uri.toURL());
            if (file != null) {
                return getOwner(file);
            }
        } catch (MalformedURLException ex) {
        }
        return null;
    }

    @Override
    public Project getOwner(FileObject file) {
        var info = PathRegistry.getDefault().findInfo(file);
        return info == null ? null : info.project();
    }

}
