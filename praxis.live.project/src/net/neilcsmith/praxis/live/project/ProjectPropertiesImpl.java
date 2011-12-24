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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ProjectPropertiesImpl extends PraxisProjectProperties {
    
    private final static FileObject[] FILEOBJECT_ARRAY = new FileObject[0];

    private Set<FileObject> buildFiles;
    private Set<FileObject> runFiles;
    private PropertyChangeSupport pcs;
    private DefaultPraxisProject project;
    private FileListener listener;

    ProjectPropertiesImpl(DefaultPraxisProject project) {
        this.project = project;
        buildFiles = new LinkedHashSet<FileObject>();
        runFiles = new LinkedHashSet<FileObject>();
        pcs = new PropertyChangeSupport(this);
        listener = new FileListener();
        project.getProjectDirectory().addRecursiveListener(listener);
    }

    @Override
    public FileObject[] getProjectFiles(ExecutionLevel level) {
        if (level == ExecutionLevel.BUILD) {
            return buildFiles.toArray(FILEOBJECT_ARRAY);
        } else if (level == ExecutionLevel.RUN) {
            return runFiles.toArray(FILEOBJECT_ARRAY);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public void setProjectFiles(ExecutionLevel level, FileObject[] files) {
        for (FileObject file : files) {
            checkFile(file);
        }
        if (level == ExecutionLevel.BUILD) {
            buildFiles.clear();
            buildFiles.addAll(Arrays.asList(files));
        } else if (level == ExecutionLevel.RUN) {
            runFiles.clear();
            runFiles.addAll(Arrays.asList(files));
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
    
    private class FileListener extends FileChangeAdapter {

        @Override
        public void fileDeleted(FileEvent fe) {
            FileObject file = fe.getFile();
            boolean changed = false;
            if (buildFiles.remove(file)) {
                changed = true;
            }
            if (runFiles.remove(file)) {
                changed = true;
            }
            if (changed) {
                pcs.firePropertyChange(PROP_FILES_CHANGED, null, null);
            }
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            pcs.firePropertyChange(PROP_FILES_CHANGED, null, null);
        }
       
        
    }
    
    
    
}
