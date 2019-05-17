/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
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
package org.praxislive.ide.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;
import org.praxislive.core.CallArguments;
import org.praxislive.core.services.ServiceUnavailableException;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProjectProperties;
import static org.praxislive.ide.project.api.PraxisProjectProperties.PROP_LIBRARIES;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ProjectPropertiesImpl implements PraxisProjectProperties {

    private final Set<FileObject> buildFiles;
    private final Set<FileObject> runFiles;
    private final PropertyChangeSupport pcs;
    private final DefaultPraxisProject project;
    private final FileListener listener;

    private int javaRelease = 8;
    
    ProjectPropertiesImpl(DefaultPraxisProject project) {
        this.project = project;
        buildFiles = new LinkedHashSet<>();
        runFiles = new LinkedHashSet<>();
        pcs = new PropertyChangeSupport(this);
        listener = new FileListener();
        project.getProjectDirectory().addRecursiveListener(listener);
    }

    @Override
    public synchronized boolean addFile(ExecutionLevel level, FileObject file) {
        Set<FileObject> set;
        if (level == ExecutionLevel.BUILD) {
            set = buildFiles;
        } else if (level == ExecutionLevel.RUN) {
            set = runFiles;
        } else {
            throw new IllegalArgumentException("Unknown build level");
        }
        boolean changed = set.add(file);
        if (changed) {
            pcs.firePropertyChange(PROP_FILES, null, null);
        }
        return changed;
    }

    @Override
    public synchronized boolean removeFile(ExecutionLevel level, FileObject file) {
        Set<FileObject> set;
        if (level == ExecutionLevel.BUILD) {
            set = buildFiles;
        } else if (level == ExecutionLevel.RUN) {
            set = runFiles;
        } else {
            throw new IllegalArgumentException("Unknown build level");
        }
        boolean changed = set.remove(file);
        if (changed) {
            pcs.firePropertyChange(PROP_FILES, null, null);
        }
        return changed;
    }

    @Override
    public synchronized List<FileObject> getFiles(ExecutionLevel level) {
        if (level == ExecutionLevel.BUILD) {
            return new ArrayList<>(buildFiles);
        } else if (level == ExecutionLevel.RUN) {
            return new ArrayList<>(runFiles);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public synchronized void setProjectFiles(ExecutionLevel level, FileObject[] files) {
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
        pcs.firePropertyChange(PROP_FILES, null, null);
    }

    public synchronized void importLibrary(FileObject lib) throws IOException {
        if (FileUtil.isParentOf(project.getProjectDirectory(), lib)) {
            throw new IOException("Library file is already inside project");
        }
        if (!lib.hasExt("jar")) {
            throw new IOException("Library must have a .jar extension");
        }
        // @TODO move off EDT
        FileObject libsFolder = FileUtil.createFolder(project.getProjectDirectory(),
                DefaultPraxisProject.LIBS_PATH);
        FileObject projectLib = FileUtil.copyFile(lib, libsFolder, lib.getName());
        if (project.isActive()) {
            String script = "add-lib " + projectLib.toURI();
            try {
                ProjectHelper.getDefault().executeScript(script, new Callback() {
                    @Override
                    public void onReturn(CallArguments args) {
                    }

                    @Override
                    public void onError(CallArguments args) {
                    }
                });
            } catch (HubUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ServiceUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        project.registerLibs();
        pcs.firePropertyChange(PROP_LIBRARIES, null, null);
    }

    public synchronized void removeLibrary(String name) throws IOException {
        if (project.isActive()) {
            throw new IOException("Cannot delete library from active project");
        }
        // @TODO move off EDT
        FileObject libsFolder = project.getProjectDirectory().getFileObject(DefaultPraxisProject.LIBS_PATH);
        if (libsFolder == null) {
            throw new IOException("No libs folder");
        }
        FileObject lib = libsFolder.getFileObject(name);
        if (lib != null) {
            lib.delete();
        }
        project.registerLibs();
        pcs.firePropertyChange(PROP_LIBRARIES, null, null);
    }

    @Override
    public synchronized List<FileObject> getLibraries() {
        FileObject libsFolder = project.getProjectDirectory().getFileObject(DefaultPraxisProject.LIBS_PATH);
        if (libsFolder == null || !libsFolder.isFolder()) {
            return Collections.EMPTY_LIST;
        } else {
            return Stream.of(libsFolder.getChildren())
                    .filter(f -> f.hasExt("jar"))
                    .collect(Collectors.toList());
        }
    }
    
    public synchronized void setJavaRelease(int release) {
        if (project.isActive()) {
            throw new IllegalStateException("Cannot change source version for active project");
        }
        if (release < 8) {
            throw new IllegalArgumentException();
        }
        if (javaRelease != release) {
            javaRelease = release;
            pcs.firePropertyChange(PROP_JAVA_RELEASE, null, null);
        }
    }
    
    @Override
    public synchronized int getJavaRelease() {
        return javaRelease;
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
            synchronized (ProjectPropertiesImpl.this) {
                FileObject file = fe.getFile();
                boolean changed = false;
                if (buildFiles.remove(file)) {
                    changed = true;
                }
                if (runFiles.remove(file)) {
                    changed = true;
                }
                if (changed) {
                    pcs.firePropertyChange(PROP_FILES, null, null);
                }
            }

        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            synchronized (ProjectPropertiesImpl.this) {
                pcs.firePropertyChange(PROP_FILES, null, null);
            }
        }
    }
}
