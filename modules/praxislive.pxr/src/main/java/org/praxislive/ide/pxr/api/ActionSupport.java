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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.api;

import org.praxislive.ide.pxr.spi.RootEditor;
import java.util.List;
import java.util.Set;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.ActionBridge;
import org.openide.filesystems.FileObject;

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

    @Deprecated
    public void copyToClipboard(ContainerProxy container, Set<String> children) {
        ActionBridge.getDefault().copyToClipboard(container, children);
    }
    
    public Task createCopyTask(ContainerProxy container, Set<String> children) {
        return createCopyTask(container, children, null, null);
    }
    
    public Task createCopyTask(ContainerProxy container,
            Set<String> children,
            Runnable preWriteTask, Runnable postWriteTask) {
        return ActionBridge.getDefault().createCopyTask(container, children, preWriteTask, postWriteTask);
    }
    
    public Task createExportTask(ContainerProxy container, Set<String> children) {
        return createExportTask(container, children, null, null);
    }
    
    public Task createExportTask(ContainerProxy container,
            Set<String> children,
            Runnable preWriteTask, Runnable postWriteTask) {
        return ActionBridge.getDefault().createExportTask(container, children, preWriteTask, postWriteTask);
    }
    
    
    public boolean pasteFromClipboard(ContainerProxy container, Callback callback) {
        return ActionBridge.getDefault().pasteFromClipboard(container, callback);
    }
    
    public boolean importSubgraph(ContainerProxy container, FileObject file, Callback callback) {
        return ActionBridge.getDefault().importSubgraph(container, file, null, callback);
    }
    
    public boolean importSubgraph(ContainerProxy container, FileObject file, List<String> warnings, Callback callback) {
        return ActionBridge.getDefault().importSubgraph(container, file, warnings, callback);
    }

}
