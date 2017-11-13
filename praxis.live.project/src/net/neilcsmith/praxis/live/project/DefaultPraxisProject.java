/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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
import net.neilcsmith.praxis.live.project.ui.PraxisCustomizerProvider;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.swing.Icon;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.FileHandler;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import net.neilcsmith.praxis.live.project.ui.PraxisLogicalViewProvider;
import net.neilcsmith.praxis.live.project.ui.ProjectDialogManager;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultPraxisProject extends PraxisProject {

    public final static String LIBS_PATH = "config/libs";
    public final static String LIBS_COMMAND = "add-libs [file-list \"" + LIBS_PATH + "\" \"*.jar\"]";

    private final static RequestProcessor RP = new RequestProcessor(PraxisProject.class);

    private final FileObject directory;
    private final FileObject projectFile;
    private final PraxisProjectProperties properties;
    private final Lookup lookup;
    private final HelperListener helperListener;
    private final PropertiesListener propsListener;
    private final ProjectState state;
    private final Set<FileObject> executedBuildFiles = new HashSet<>();
    private boolean actionsEnabled = true;
    private boolean active;
    private ClassPath libsCP;

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
            new ProjectOpenedHookImpl(),
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

    private ProjectPropertiesImpl parseProjectFile(FileObject projectFile) {
        ProjectPropertiesImpl props = new ProjectPropertiesImpl(this);
        try {
            PXPReader.initializeProjectProperties(directory, projectFile, props);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return props;
    }

    @Override
    public FileObject getProjectDirectory() {
        return directory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public void save() throws IOException {
        PXPWriter.writeProjectProperties(directory, projectFile, properties);
    }

    public boolean isActive() {
        return active;
    }

    private void execute(ExecutionLevel level) {
        if (!active) {
            registerLibs();
        }
        List<FileObject> buildFiles = new ArrayList<>();
        FileObject libsFolder = directory.getFileObject(LIBS_PATH);
        if (libsFolder != null) {
            buildFiles.add(libsFolder);
        }
        buildFiles.addAll(Arrays.asList(properties.getProjectFiles(ExecutionLevel.BUILD)));
        buildFiles.removeAll(executedBuildFiles);

        List<FileObject> runFiles = level == ExecutionLevel.RUN
                ? Arrays.asList(properties.getProjectFiles(ExecutionLevel.RUN))
                : Collections.EMPTY_LIST;

        FileHandlerIterator itr = new FileHandlerIterator(buildFiles, runFiles);
        active = true;
        actionsEnabled = false;
        itr.start();
    }

    void registerLibs() {
        clearLibs();
        libsCP = buildLibsClasspath();
        if (libsCP != null) {
            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, new ClassPath[]{libsCP});
        }
    }
    
    private void clearLibs() {
        if (libsCP != null) {
            GlobalPathRegistry.getDefault().unregister(ClassPath.COMPILE, new ClassPath[]{libsCP});
        }
        libsCP = null;
    }

    private ClassPath buildLibsClasspath() {
        FileObject libsFolder = directory.getFileObject(LIBS_PATH);
        if (libsFolder != null) {
            return ClassPathSupport.createClassPath(
                    Stream.of(libsFolder.getChildren())
                            .filter(f -> f.isData() && f.hasExt("jar"))
                            .map(f -> FileUtil.urlForArchiveOrDir(FileUtil.toFile(f)))
                            .toArray(URL[]::new)
            );
        } else {
            return null;
        }
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
    
    private class ProjectOpenedHookImpl extends ProjectOpenedHook {

        @Override
        protected void projectOpened() {
            registerLibs();
        }

        @Override
        protected void projectClosed() {
            clearLibs();
        }
        
    }

    private class BaseTemplates implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                "Templates/Other/Folder",
                "Templates/Other/org-netbeans-modules-project-ui-NewFileIterator-folderIterator"
            };
        }
    }

    private class HelperListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProjectHelper.PROP_HUB_CONNECTED.equals(evt.getPropertyName())) {
                if (ProjectHelper.getDefault().isConnected()) {
                    actionsEnabled = true;
                    active = false;
                    executedBuildFiles.clear();
                }
            }
        }
    }

    private class PropertiesListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            state.markModified();
            RP.schedule(new Runnable() {

                @Override
                public void run() {
                    try {
                        ProjectManager.getDefault().saveProject(DefaultPraxisProject.this);
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }, 500, TimeUnit.MILLISECONDS);
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
                execute(ExecutionLevel.RUN);
            } else if (ActionProvider.COMMAND_BUILD.equals(command)) {
                execute(ExecutionLevel.BUILD);
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {

            return ProjectHelper.getDefault().isConnected() && actionsEnabled;

        }
    }

    private class LibrariesFileHandler extends FileHandler {

        @Override
        public void process(Callback callback) throws Exception {
            String script = "set _PWD " + directory.toURI() + "\n" + LIBS_COMMAND;
            ProjectHelper.getDefault().executeScript(script, callback);
        }

    }

    private class FileHandlerIterator implements Cancellable {

        private List<FileObject> buildFiles;
        private List<FileObject> runFiles;
        private ProgressHandle progress = null;
        private int index = -1;
        private FileHandler.Provider[] handlers = new FileHandler.Provider[0];
        private Map<FileObject, List<String>> warnings;
        private ExecutionLevel level;

        private FileHandlerIterator(List<FileObject> buildFiles, List<FileObject> runFiles) {
            this.buildFiles = buildFiles;
            this.runFiles = runFiles;
            handlers = Lookup.getDefault().lookupAll(FileHandler.Provider.class).toArray(handlers);
        }

        public void start() {
            int totalFiles = buildFiles.size() + runFiles.size();
            if (totalFiles == 0) {
                return;
            }
            progress = ProgressHandle.createHandle("Executing...", this);
            progress.setInitialDelay(0);
            progress.start(totalFiles);
            next();
        }

        @Override
        public boolean cancel() {
            return false;
        }

        private void next() {
            index++;
            if (index >= (buildFiles.size() + runFiles.size())) {
                done();
                return;
            }
            FileObject file;
//            ExecutionLevel level;
            if (index < buildFiles.size()) {
                file = buildFiles.get(index);
                executedBuildFiles.add(file);
                level = ExecutionLevel.BUILD;
            } else {
                file = runFiles.get(index - buildFiles.size());
                level = ExecutionLevel.RUN;
            }
            FileHandler handler = findHandler(level, file);
            String msg = FileUtil.getRelativePath(getProjectDirectory(), file) + " [" + level + "]";
            progress.progress(msg, index);
            try {
                handler.process(new CallbackImpl(handler, file));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                if (continueOnError(handler, file, null)) {
                    next();
                } else {
                    done();
                }
            }
        }

        private boolean continueOnError(FileHandler handler, FileObject file, CallArguments args) {
            return ProjectDialogManager.getDefault().continueOnError(
                    DefaultPraxisProject.this, file, args, level);
        }

        private void logWarnings(FileHandler handler, FileObject file) {
            List<String> wl = handler.getWarnings();
            if (wl == null || wl.isEmpty()) {
                return;
            }
            if (warnings == null) {
                warnings = new LinkedHashMap<FileObject, List<String>>();
            }
            warnings.put(file, wl);
        }

        private void done() {
            progress.finish();
            if (warnings != null) {
                ProjectDialogManager.getDefault().showWarningsDialog(DefaultPraxisProject.this, warnings, level);
            }
            actionsEnabled = true;
        }

        private FileHandler findHandler(ExecutionLevel level, FileObject file) {

            if (file.isFolder()
                    && file.equals(directory.getFileObject(LIBS_PATH))) {
                return new LibrariesFileHandler();
            }

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
                logWarnings(handler, file);
                next();
            }

            @Override
            public void onError(CallArguments args) {
                if (continueOnError(handler, file, args)) {
                    next();
                } else {
                    done();
                }
            }
        }
    }
}
