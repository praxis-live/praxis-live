/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.io.File;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRRootProxy extends PXRContainerProxy implements RootProxy {

    private ComponentAddress address;
    private PXRDataObject source;
    private PraxisProject project;
//    private String id;

    PXRRootProxy(PXRDataObject source, String id, ComponentType type, ComponentInfo info) {
        this(null, source, id, type, info);
    }

    PXRRootProxy(PraxisProject project, PXRDataObject source, String id,
            ComponentType type, ComponentInfo info) {
        super(null, type, info);
//        this.id = id;
        this.address = ComponentAddress.create("/" + id);
        this.source = source;
        this.project = project;
    }

    @Override
    public ComponentAddress getAddress() {
        return address;
    }

    @Override
    public FileObject getSourceFile() {
        return source.getPrimaryFile();
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

    File getWorkingDirectory() {
        if (project == null) {
            return FileUtil.toFile(source.getPrimaryFile().getParent());
        } else {
            return FileUtil.toFile(project.getProjectDirectory());
        }
    }

}
