/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
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

package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Value;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.project.api.PraxisProject;

class BoundSharedCodeProperty extends BoundArgumentProperty {
    
    private final PraxisProject project;
    private final FileSystem fileSystem;
    private final FileObject sharedFolder;
    private final Listener fileListener;
    
    private boolean valueIsAdjusting;

    BoundSharedCodeProperty(PraxisProject project, ControlAddress address, ControlInfo info) {
        super(project, address, info);
        this.project = project;
        this.fileSystem = FileUtil.createMemoryFileSystem();
        this.fileListener = new Listener();
        try {
            sharedFolder = fileSystem.getRoot().createFolder("SHARED");
            sharedFolder.addRecursiveListener(fileListener);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        setHidden(true);
        addPropertyChangeListener(this::updateFiles);
    }

    FileObject getSharedCodeFolder() {
        return sharedFolder;
    }
    
    private void updateFiles(PropertyChangeEvent update) {
        
        if (valueIsAdjusting) {
            return;
        }
        
        try {
            valueIsAdjusting = true;

            PMap oldFiles = PMap.from((Value) update.getOldValue()).orElse(PMap.EMPTY);
            PMap newFiles = PMap.from((Value) update.getNewValue()).orElse(PMap.EMPTY);

            List<String> workingList = new ArrayList<>();

            // remove deleted files
            workingList.addAll(oldFiles.keys());
            workingList.removeAll(newFiles.keys());
            workingList.forEach(this::removeFile);

            // add new files
            workingList.clear();
            workingList.addAll(newFiles.keys());
            workingList.removeAll(oldFiles.keys());
            workingList.forEach(f -> addFile(f, newFiles.getString(f, "")));

            // ignore text updates?
            
        } finally {
            valueIsAdjusting = false;
        }
        
    }
    
    private void addFile(String binaryName, String source) {
        try {
            FileObject file = 
                    FileUtil.createData(fileSystem.getRoot(), toFileName(binaryName));
            file.setAttribute("project", project);
            file.setAttribute("controlAddress", getAddress());
            try (OutputStreamWriter writer = new OutputStreamWriter(file.getOutputStream())) {
                writer.append(source);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    private void removeFile(String binaryName) {
        FileObject file = fileSystem.findResource(toFileName(binaryName));
        if (file != null) {
            try {
                file.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    private String toFileName(String binaryName) {
        return binaryName.replace('.', '/') + ".java";
    }
    
    private String toBinaryName(String path) {
        int i = path.lastIndexOf('.');
        if (i > 0) {
            return path.substring(0, i).replace('/', '.');
        } else {
            return path.replace('/', '.');
        }
    }
    
    private class Listener implements FileChangeListener {

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
            // no op
        }

        @Override
        public void fileChanged(FileEvent fe) {
            update();
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            // no op ?
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            if (fe.getFile() == sharedFolder) {
                throw new IllegalStateException("Shared folder deleted!");
            }
            update();
        }

        @Override
        public void fileFolderCreated(FileEvent fe) {
            // no op ?
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            update();
        }
        
        private void update() {
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(this::update);
            }
            if (valueIsAdjusting) {
                return;
            }
            valueIsAdjusting = true;
            try {
                PMap.Builder mapBuilder = PMap.builder();
                sharedFolder.getChildren(true).asIterator()
                        .forEachRemaining(file -> {
                            if ("java".equals(file.getExt())) {
                                try {
                                    mapBuilder.put(toBinaryName(file.getPath()),
                                            file.asText());
                                } catch (IOException ex) {
                                    throw new IllegalStateException(ex);
                                }
                            }
                        });
                setValue(mapBuilder.build());
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                valueIsAdjusting = false;
            }
            
        }
        
    }

}
