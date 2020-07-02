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
package org.praxislive.ide.project;

import java.beans.PropertyChangeEvent;
import org.praxislive.ide.project.ui.PraxisCustomizerProvider;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.lang.model.SourceVersion;
import javax.swing.Icon;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.ui.PraxisLogicalViewProvider;
import org.praxislive.ide.project.ui.ProjectDialogManager;
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
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.core.Value;
import org.praxislive.ide.core.api.AbstractTask;
import org.praxislive.ide.core.api.SerialTasks;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.spi.ElementHandler;

/**
 *
 */
@NbBundle.Messages({
    "# {0} - required Java release",
    "PraxisProject.javaVersionError=This project requires Java {0}"
})
public class DefaultPraxisProject implements PraxisProject {

    public final static String LIBS_PATH = "config/libs/";
    public final static String LIBS_COMMAND = "add-libs [file-list \"" + LIBS_PATH + "*.jar\"]";

    public static final int MIN_JAVA_VERSION = 11;
    public static final int MAX_JAVA_VERSION;

    static {
        int max = SourceVersion.latest().ordinal();
        MAX_JAVA_VERSION = max < MIN_JAVA_VERSION ? MIN_JAVA_VERSION : max;
    }

    private final static RequestProcessor RP = new RequestProcessor(PraxisProject.class);

    private final FileObject directory;
    private final FileObject projectFile;
    private final ProjectPropertiesImpl properties;
    private final HubManager hubManager;
    private final Lookup lookup;
    private final PropertiesListener propsListener;
    private final ProjectState state;
    private final Set<ElementHandler> executedHandlers;

    private boolean actionsEnabled;
    private ClassPath libsCP;
    private TaskExec activeExec;

    DefaultPraxisProject(FileObject directory, FileObject projectFile, ProjectState state)
            throws IOException {
        this.directory = directory;
        this.projectFile = projectFile;
        this.state = state;
        executedHandlers = new HashSet<>();
        properties = parseProjectFile(projectFile);
        propsListener = new PropertiesListener();
        properties.addPropertyChangeListener(propsListener);
        hubManager = new HubManager(this);
        Lookup base = Lookups.fixed(
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
        );

        base = new ProxyLookup(base, hubManager.getLookup());
        this.lookup = LookupProviderSupport.createCompositeLookup(base, LOOKUP_PATH);
        actionsEnabled = true;
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
        return hubManager.getState() == HubManager.State.Running;
    }

    private void execute(ExecutionLevel level) {

        if (properties.getJavaRelease() > MAX_JAVA_VERSION) {
            NotifyDescriptor nd = new NotifyDescriptor.Message(
                    Bundle.PraxisProject_javaVersionError(properties.getJavaRelease()),
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);
            return;
        }

        List<Task> tasks = new ArrayList<>();

        if (!isActive()) {
            registerLibs();
            executedHandlers.clear();
            tasks.add(hubManager.createStartupTask());
        }

        var elements = properties.elements();
        elements.get(ExecutionLevel.CONFIGURE).forEach(e -> {
            if (!executedHandlers.contains(e.handler())) {
                tasks.add(new ElementTask(ExecutionLevel.CONFIGURE, e));
            }
        });
        if (level == ExecutionLevel.BUILD || level == ExecutionLevel.RUN) {
            elements.get(ExecutionLevel.BUILD).forEach(e -> {
                if (!executedHandlers.contains(e.handler())) {
                    tasks.add(new ElementTask(ExecutionLevel.BUILD, e));
                }
            });
        }
        if (level == ExecutionLevel.RUN) {
            elements.get(ExecutionLevel.RUN).forEach(e -> {
                tasks.add(new ElementTask(ExecutionLevel.RUN, e));
            });
        }

        actionsEnabled = false;
        activeExec = new TaskExec(tasks);
        var execState = activeExec.execute();
        if (execState == Task.State.RUNNING) {
            activeExec.addPropertyChangeListener(e -> {
                actionsEnabled = true;
                activeExec = null;
            });
        } else {
            actionsEnabled = true;
            activeExec = null;
        }

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
            return ImageUtilities.loadImageIcon("org/praxislive/ide/project/resources/pxp16.png", false);
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

            return actionsEnabled;

        }
    }

    private class TaskExec extends SerialTasks {

        private final Map<Task, List<String>> warnings;
        private final ProgressHandle progress;
        private final int count;

        private TaskExec(List<Task> tasks) {
            super(tasks);
            warnings = new LinkedHashMap<>();
            progress = ProgressHandle.createHandle("Executing...", this);
            progress.setInitialDelay(0);
            count = tasks.size();
        }

        @Override
        protected void beforeExecute() {
            progress.start(count);
        }

        @Override
        protected void beforeTask(Task task) {
            task.description().ifPresentOrElse(
                    d -> progress.progress(d, count - remaining()),
                    () -> progress.progress(count - remaining()));
        }

        @Override
        protected void afterTask(Task task) {
            var log = task.log();
            if (!log.isEmpty()) {
                warnings.put(task, List.copyOf(log));
            }
        }

        @Override
        protected void afterExecute() {
            progress.finish();
            if (!warnings.isEmpty()) {
                ProjectDialogManager.getDefault()
                        .showWarningsDialog(DefaultPraxisProject.this, warnings);
            }
        }
        
        

    }

    private class ElementTask extends AbstractTask {

        private final ExecutionLevel level;
        private final ExecutionElement element;
        private final ElementHandler handler;

        private ElementTask(ExecutionLevel level, ExecutionEntry entry) {
            this.level = level;
            this.element = entry.element();
            this.handler = entry.handler();
        }

        @Override
        protected void handleExecute() throws Exception {
            if (level != ExecutionLevel.RUN) {
                executedHandlers.add(handler);
            }
            handler.process(Callback.create(result -> {
                if (result.isError()) {
                    if (continueOnError(element, result.args())) {
                        updateState(State.COMPLETED);
                    } else {
                        updateState(State.ERROR);
                    }
                } else {
                    updateState(State.COMPLETED);
                }
            }));
        }

        @Override
        public Optional<String> description() {
            if (element instanceof ExecutionElement.File) {
                var msg = FileUtil.getRelativePath(getProjectDirectory(),
                        ((ExecutionElement.File) element).file())
                        + " [" + level + "]";
                return Optional.of(msg);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public List<String> log() {
            return handler.warnings();
        }

        private boolean continueOnError(ExecutionElement element, List<Value> args) {
            return ProjectDialogManager.getDefault().continueOnError(
                    DefaultPraxisProject.this, level, element, args);
        }

    }
}
