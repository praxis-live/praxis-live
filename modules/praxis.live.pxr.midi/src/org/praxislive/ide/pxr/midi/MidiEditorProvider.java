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
package org.praxislive.ide.pxr.midi;

import java.util.Optional;
import org.openide.filesystems.FileObject;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.praxislive.ide.model.RootProxy;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
@ServiceProvider(service=RootEditor.Provider.class, position = 20)
public class MidiEditorProvider implements RootEditor.Provider {

    @Override
    public Optional<RootEditor> createEditor(PraxisProject project, FileObject file, RootProxy model) {
         ComponentType type = model.getType();
        if (type.toString().equals("root:midi")) {
            return Optional.of(new MidiEditor(model));
        }
        return Optional.empty();
    }
    
}
