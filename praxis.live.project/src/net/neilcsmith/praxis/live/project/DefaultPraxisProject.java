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
import net.neilcsmith.praxis.live.core.api.HubStateException;
import net.neilcsmith.praxis.live.project.ui.PraxisCustomizerProvider;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.core.api.HubManager;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import net.neilcsmith.praxis.live.project.ui.PraxisLogicalViewProvider;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultPraxisProject extends PraxisProject {

    private final static FileObject[] EMPTY_FILES = new FileObject[0];
    
    private final FileObject directory;
    private FileObject projectFile;
    private PraxisProjectProperties properties;
    private final Lookup lookup;
    private HelperListener helperListener;
    private PropertiesListener propsListener;
    private ProjectState state;
    private Set<FileObject> executedBuildFiles = new HashSet<FileObject>();
    private boolean actionsEnabled = true;

    DefaultPraxisProject(FileObject directory, FileObject projectFile, ProjectState state)
            throws IOException {
        this.directory = directory;
        this.projectFile = projectFile;
        this.state = state;
        properties = parseProjectFile(projectFile);
        propsListener = new PropertiesListener();
        properties.addPropertyChangeListener(propsListener);

        Lookup base = Lookups.fixed(new Object[]{
                    this,
                    properties,
                    new Info(),
                    new ActionImpl(),
                    state,
                    new PraxisCustomizerProvider(this),
                    new PraxisLogicalViewProvider(this),
                    new BaseTemplates(),
                    UILookupMergerSupport.createPrivilegedTemplatesMerger()
                });

        this.lookup = LookupProviderSupport.createCompositeLookup(base, LOOKUP_PATH);
        helperListener = new HelperListener();
        ProjectHelper.getDefault().addPropertyChangeListener(
                WeakListeners.propertyChange(helperListener, ProjectHelper.getDefault()));
    }

    @Override
    public FileObject getProjectDirectory() {
        return directory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private ProjectPropertiesImpl parseProjectFile(FileObject projectFile) {
        ProjectPropertiesImpl props = new ProjectPropertiesImpl(this);
        try {
            PXPReader.initializeProjectProperties(directory, projectFile, props);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return props;
    }

    public void save() throws IOException {
        PXPWriter.writeProjectProperties(directory, projectFile, properties);
    }

    private void invokeBuild() {
        actionsEnabled = false;
        executedBuildFiles.clear();
        FileObject[] buildFiles = properties.getProjectFiles(ExecutionLevel.BUILD);
        FileHandlerIterator itr = new FileHandlerIterator(buildFiles, EMPTY_FILES);
        itr.start();
    }

    private void invokeRun() {
        actionsEnabled = false;
        FileObject[] buildFiles = properties.getProjectFiles(ExecutionLevel.BUILD);
        if (!executedBuildFiles.isEmpty()) {
            Set<FileObject> files = new HashSet<FileObject>(Arrays.asList(buildFiles));
            files.removeAll(executedBuildFiles);
            buildFiles = files.toArray(EMPTY_FILES);
        }
        FileObject[] runFiles = properties.getProjectFiles(ExecutionLevel.RUN);
        FileHandlerIterator itr = new FileHandlerIterator(buildFiles, runFiles);
        itr.start();
    }

    private class Info implements ProjectInformation {

        @Override
        public String getName() {
            return directory.getName();
        }

        @Override
        public String getDisplayName() {
            return directory.getName();
        }

        @Override
        public Icon getIcon() {
            return ImageUtilities.loadImageIcon("net/neilcsmith/praxis/live/project/resources/pxp16.png", false);
        }

        @Override
        public Project getProject() {
            return DefaultPraxisProject.this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // no op
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            // no op
        }
    }

    private class BaseTemplates implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                        "Templates/Other/Folder"
                    };
        }
    }

    private class HelperListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProjectHelper.PROP_HUB_CONNECTED.equals(evt.getPropertyName())) {
                if (ProjectHelper.getDefault().isConnected()) {
                    actionsEnabled = true;
                    executedBuildFiles.clear();
                }
            }
        }
    }

    private class PropertiesListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            state.markModified();
        }
    }

    private class ActionImpl implements ActionProvider {

        @Override
        public String[] getSupportedActions() {
            return new String[]{
                        ActionProvider.COMMAND_RUN,
                        ActionProvider.COMMAND_BUILD
                    };
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {

            if (ActionProvider.COMMAND_RUN.equals(command)) {
                invokeRun();
            } else if (ActionProvider.COMMAND_BUILD.equals(command)) {
                invokeBuild();
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {

            return ProjectHelper.getDefault().isConnected() && actionsEnabled;

        }
    }

    class FileHandlerIterator implements Cancellable {

        private FileObject[] buildFiles;
        private FileObject[] runFiles;
        private ProgressHandle progress = null;
        private int index = -1;
        private FileHandler.Provider[] handlers = new FileHandler.Provider[0];
        private Map<FileObject, List<String>> errors;

        private FileHandlerIterator(FileObject[] buildFiles, FileObject[] runFiles) {
            this.buildFiles = buildFiles;
            this.runFiles = runFiles;
            handlers = Lookup.getDefault().lookupAll(FileHandler.Provider.class).toArray(handlers);
        }

        public void start() {
            int totalFiles = buildFiles.length + runFiles.length;
            if (totalFiles == 0) {
                return;
            }
            progress = ProgressHandleFactory.createHandle("Executing...", this);
            progress.start(totalFiles);
            next();
        }

        @Override
        public boolean cancel() {
            return false;
        }

        private void next() {
            index++;
            if (index >= (buildFiles.length + runFiles.length)) {
                done();
                return;
            }
            FileObject file;
            ExecutionLevel level;
            if (index < buildFiles.length) {
                file = buildFiles[index];
                executedBuildFiles.add(file);
                level = ExecutionLevel.BUILD;
            } else {
                file = runFiles[index - buildFiles.length];
                level = ExecutionLevel.RUN;
            }
            FileHandler handler = findHandler(level, file);
            String msg = FileUtil.getRelativePath(getProjectDirectory(), file) + " [" + level + "]";
            progress.progress(msg, index);
            try {
                handler.process(new CallbackImpl(handler, file));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                error(handler, file);
                next();
            }
        }

        private void error(FileHandler handler, FileObject file) {
            if (errors == null) {
                errors = new LinkedHashMap<FileObject, List<String>>();
            }
            errors.put(file, handler.getErrors());
        }

        private void done() {
            actionsEnabled = true;
            progress.finish();
        }

        private FileHandler findHandler(ExecutionLevel level, FileObject file) {
            FileHandler handler = null;
            for (FileHandler.Provider provider : handlers) {
                try {
                    handler = provider.getHandler(DefaultPraxisProject.this, level, file);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                if (handler != null) {
                    break;
                }
            }
            if (handler == null) {
                handler = new DefaultFileHandler(DefaultPraxisProject.this, level, file);
            }
            return handler;
        }

        private class CallbackImpl implements Callback {
            
            private FileHandler handler;
            private FileObject file;

            private CallbackImpl(FileHandler handler, FileObject file) {
                this.handler = handler;
                this.file = file;
            }
            
            @Override
            public void onReturn(CallArguments args) {
                next();
            }

            @Override
            public void onError(CallArguments args) {
                error(handler, file);
                next();
            }
        }
    }
}
