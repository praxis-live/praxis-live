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
package net.neilcsmith.praxis.live.pxr.api;

import java.util.Set;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.ActionBridge;
import org.openide.filesystems.FileObject;

/**
 *
 * Support class for Actions in PXR editors.
 *
 * @author Neil C Smith
 */
public class ActionSupport {

    private final RootEditor editor;
    
    public ActionSupport(RootEditor editor) {
        if (editor == null) {
            throw new NullPointerException();
        }
        this.editor = editor;
    }

    public void copyToClipboard(ContainerProxy container, Set<String> children) {
        ActionBridge.copyToClipboard(container, children);
    }
    
    public boolean pasteFromClipboard(ContainerProxy container, Callback callback) {
        return ActionBridge.pasteFromClipboard(container, callback);
    }
    
    public boolean importSubgraph(ContainerProxy container, FileObject file, Callback callback) {
        return ActionBridge.importSubgraph(container, file, callback);
    }

}
