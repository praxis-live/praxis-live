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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ProjectPropertiesImpl extends PraxisProjectProperties {

    private FileObject[] buildFiles = new FileObject[0];
    private FileObject[] runFiles = new FileObject[0];
    private PropertyChangeSupport pcs= new PropertyChangeSupport(this);
    private DefaultPraxisProject project;

    ProjectPropertiesImpl(DefaultPraxisProject project) {
        this.project = project;
    }

    @Override
    public FileObject[] getProjectFiles(ExecutionLevel level) {
        if (level == ExecutionLevel.BUILD) {
            return (FileObject[]) buildFiles.clone();
        } else if (level == ExecutionLevel.RUN) {
            return (FileObject[]) runFiles.clone();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void setProjectFiles(ExecutionLevel level, FileObject[] files) {
        files = (FileObject[]) files.clone();
        for (FileObject file : files) {
            checkFile(file);
        }
        if (level == ExecutionLevel.BUILD) {
            buildFiles = files;
        } else if (level == ExecutionLevel.RUN) {
            runFiles = files;
        } else {
            throw new IllegalArgumentException("Unknown build level");
        }
        pcs.firePropertyChange(PROP_FILES_CHANGED, null, null);
    }

    private void checkFile(FileObject file) {
        if (!FileUtil.isParentOf(project.getProjectDirectory(), file)) {
            throw new IllegalArgumentException("All files must be contained in project");
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
