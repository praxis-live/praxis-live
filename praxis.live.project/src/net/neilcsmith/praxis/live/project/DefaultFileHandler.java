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
package net.neilcsmith.praxis.live.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.openide.filesystems.FileObject;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class DefaultFileHandler extends FileHandler {

    private static BuildRegistry REGISTRY = new BuildRegistry();
//    private static RequestProcessor RP = new RequestProcessor();
    private ExecutionLevel level;
    private PraxisProject project;
    private FileObject file;

    DefaultFileHandler(PraxisProject project, ExecutionLevel level, FileObject file) {
        this.project = project;
        this.file = file;
        this.level = level;
    }

    @Override
    public void process(Callback callback) throws Exception {
        if (level == ExecutionLevel.BUILD) {
            if (!REGISTRY.addIfAbsent(file)) {
                
            }
        }
        String script = file.asText();
        script = "set _PWD " + project.getProjectDirectory().getURL().toURI() + "\n" + script;
        ProjectHelper.getDefault().executeScript(script, callback);
    }

    private static class BuildRegistry implements PropertyChangeListener {

        private Set<FileObject> files;

        private BuildRegistry() {
            files = new HashSet<FileObject>();
            ProjectHelper.getDefault().addPropertyChangeListener(this);
        }

        private boolean addIfAbsent(FileObject file) {
            return files.add(file);
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (ProjectHelper.PROP_HUB_CONNECTED.equals(pce.getPropertyName())) {
                files.clear();
            }
        }
    }
}
