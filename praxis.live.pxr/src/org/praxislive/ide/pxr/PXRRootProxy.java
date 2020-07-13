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
package org.praxislive.ide.pxr;

import java.io.File;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.model.RootProxy;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.praxislive.ide.core.api.Disposable;

/**
 *
 */
public class PXRRootProxy extends PXRContainerProxy implements RootProxy, Disposable {

    private final ComponentAddress address;
    private final PXRDataObject source;
    private final PraxisProject project;
    private final PXRHelper helper;

    PXRRootProxy(PraxisProject project, PXRHelper helper, PXRDataObject source, String id,
            ComponentType type, ComponentInfo info) {
        super(null, type, info);
        this.address = ComponentAddress.of("/" + id);
        this.source = source;
        this.project = project;
        this.helper = helper;
    }

    @Override
    public ComponentAddress getAddress() {
        return address;
    }

    public FileObject getSourceFile() {
        return source.getPrimaryFile();
    }
    
    @Override
    public void dispose() {
        super.dispose();
        var reg = project.getLookup().lookup(PXRRootRegistry.class);
        assert reg != null;
        if (reg != null) {
            reg.remove(this);
        }
    }

    @Override
    PXRRootProxy getRoot() {
        return this;
    }

    PXRDataObject getSource() {
        return source;
    }

    PraxisProject getProject() {
        return project;
    }

    PXRHelper getHelper() {
        return helper;
    }
    
    
    File getWorkingDirectory() {
        if (project == null) {
            return FileUtil.toFile(source.getPrimaryFile().getParent());
        } else {
            return FileUtil.toFile(project.getProjectDirectory());
        }
    }

}
