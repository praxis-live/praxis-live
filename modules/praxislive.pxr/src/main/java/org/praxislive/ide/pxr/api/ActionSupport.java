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
package org.praxislive.ide.pxr.api;

import org.praxislive.ide.pxr.spi.RootEditor;
import java.util.Set;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.ActionBridge;
import org.openide.filesystems.FileObject;
import org.praxislive.ide.pxr.spi.ModelTransform;


/**
 *
 * Support class for Actions in PXR editors.
 *
 */
public class ActionSupport {

    private final RootEditor editor;

    public ActionSupport(RootEditor editor) {
        if (editor == null) {
            throw new NullPointerException();
        }
        this.editor = editor;
    }

    public Task createCopyTask(ContainerProxy container, Set<String> children) {
        return ActionBridge.getDefault().createCopyTask(container,
                children,
                editor.getLookup().lookup(ModelTransform.Copy.class));
    }

    @SuppressWarnings("deprecation")
    public Task createExportTask(ContainerProxy container, Set<String> children) {
        return ActionBridge.getDefault().createExportTask(
                container,
                children,
                editor.getLookup().lookup(ModelTransform.Export.class));
    }


    public Task createImportTask(ContainerProxy container, FileObject file) {
        return ActionBridge.getDefault().createImportTask(container,
                file,
                editor.getLookup().lookup(ModelTransform.Import.class));
    }

    public Task createPasteTask(ContainerProxy container) {
        return ActionBridge.getDefault().createPasteTask(container,
                editor.getLookup().lookup(ModelTransform.Paste.class));
    }


}
