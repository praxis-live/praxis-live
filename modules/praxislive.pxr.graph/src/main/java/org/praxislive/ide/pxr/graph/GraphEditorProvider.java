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
package org.praxislive.ide.pxr.graph;

import java.util.Optional;
import org.openide.filesystems.FileObject;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
@ServiceProvider(service = RootEditor.Provider.class)
public class GraphEditorProvider implements RootEditor.Provider {

    @Override
    public Optional<RootEditor> createEditor(RootProxy root, RootEditor.Context context) {
        String type = root.getType().toString();
        Optional<FileObject> file = context.file();
        Optional<PraxisProject> project = context.project();
        if (type.startsWith("root:") && file.isPresent() && project.isPresent()) {
            return Optional.of(new GraphEditor(project.orElseThrow(),
                    file.orElseThrow(), root, type.substring(5)));
        }
        return Optional.empty();
    }

}
