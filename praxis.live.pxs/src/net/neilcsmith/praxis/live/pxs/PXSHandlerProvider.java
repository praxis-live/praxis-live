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

package net.neilcsmith.praxis.live.pxs;

import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service=FileHandler.Provider.class)
public class PXSHandlerProvider implements FileHandler.Provider {

    @Override
    public FileHandler getHandler(PraxisProject project, ExecutionLevel level, FileObject file) {
        return null;
    }


    private class Handler extends FileHandler {

        @Override
        public void process(Callback callback) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

}
