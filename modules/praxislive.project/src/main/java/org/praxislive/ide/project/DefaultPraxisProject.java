/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project;

import java.beans.PropertyChangeEvent;
import org.praxislive.ide.project.ui.PraxisCustomizerProvider;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.lang.model.SourceVersion;
import javax.swing.Icon;
import org.netbeans.api.java.classpath.*;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.ui.PraxisLogicalViewProvider;
import org.praxislive.ide.project.ui.ProjectDialogManager;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
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
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PResource;
import org.praxislive.ide.core.api.AbstractTask;
import org.praxislive.ide.core.api.SerialTasks;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.spi.ElementHandler;
import org.praxislive.ide.project.spi.LineHandler;
import org.praxislive.project.ProjectElement;
import org.praxislive.project.ProjectModel;

/**
 *
 */
@NbBundle.Messages({
    "# {0} - required Java release",
    "PraxisProject.javaVersionError=This project requires Java {0}",
    "ERR_elementContinueBuild=Continue building project?",
    "ERR_elementContinueRun=Continue running project?",
    "# {0} - path or command",
    "ERR_elementExecution=Error executing {0}"
})
public class DefaultPraxisProject implements PraxisProject {

    public final static String LIBS_PATH = "config/libs/";
    final static String LIBS_COMMAND = "libraries {\n  " + LIBS_PATH + "*.jar\n}";

    public static final int MIN_JAVA_VERSION = 11;
    public static final int MAX_JAVA_VERSION;

    static {
        int max = SourceVersion.latest().ordinal();
        MAX_JAVA_VERSION = max < MIN_JAVA_VERSION ? MIN_JAVA_VERSION : max;
    }

    private final static RequestProcessor RP = new RequestProcessor(PraxisProject.class);
    private final static LinkedHashSet<DefaultPraxisProject> REGISTRY
            = new LinkedHashSet<>();

    private final FileObject directory;
    private final FileObject projectFile;
    private final ProjectState state;
    private final HubManager hubManager;
    private final ProjectPropertiesImpl properties;
    private final PropertiesListener propsListener;
    private final Lookup lookup;
    private final Set<ElementHandler> executedHandlers;

    private boolean actionsEnabled;
    private List<URI> libPath;
    private ClassPath libsCP;
    private ClassPath compileCP;
    private TaskExec activeExec;

    DefaultPraxisProject(FileObject directory, FileObject projectFile, ProjectState state)
            throws IOException {
        this.directory = directory;
        this.projectFile = projectFile;
        this.state = state;
        hubManager = new HubManager(this);
        properties = parseProjectFile(projectFile);
        propsListener = new PropertiesListener();
        properties.addPropertyChangeListener(propsListener);
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
                new ClassPathImpl(),
                UILookupMergerSupport.createPrivilegedTemplatesMerger()
        );

        base = new ProxyLookup(base, hubManager.getLookup());
        this.lookup = LookupProviderSupport.createCompositeLookup(base, LOOKUP_PATH);
        executedHandlers = new HashSet<>();
        actionsEnabled = true;
        libPath = List.of();
        compileCP = CoreClassPathRegistry.getInstance().getCompileClasspath();
    }

    private ProjectPropertiesImpl parseProjectFile(FileObject projectFile) {
        ProjectPropertiesImpl props = new ProjectPropertiesImpl(this);
        try {
            ProjectModel model = ProjectModel.parse(directory.toURI(), projectFile.asText());
            List<ExecutionElement> config = model.setupElements().stream()
                    .map(this::fromModelElement)
                    .filter(e -> e != null)
                    .toList();
            List<ExecutionElement> build = model.buildElements().stream()
                    .map(this::fromModelElement)
                    .filter(e -> e != null)
                    .toList();
            List<ExecutionElement> run = model.runElements().stream()
                    .map(this::fromModelElement)
                    .filter(e -> e != null)
                    .toList();
            props.initElements(Map.of(
                    ExecutionLevel.CONFIGURE, config,
                    ExecutionLevel.BUILD, build,
                    ExecutionLevel.RUN, run));
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
        ProjectModel.Builder builder = ProjectModel.builder();
        builder.context(directory.toURI());
        Map<ExecutionLevel, List<ExecutionEntry>> elements = properties.elements();
        elements.get(ExecutionLevel.CONFIGURE)
                .forEach(e -> builder.setupElement(toModelElement(e)));
        elements.get(ExecutionLevel.BUILD)
                .forEach(e -> builder.buildElement(toModelElement(e)));
        elements.get(ExecutionLevel.RUN)
                .forEach(e -> builder.runElement(toModelElement(e)));
        String script = builder.build().writeToString();
        Files.writeString(FileUtil.toPath(projectFile), script);
    }

    public boolean isActive() {
        return hubManager.getState() == HubManager.State.Running;
    }

    public static List<DefaultPraxisProject> activeProjects() {
        // extra check required?
        REGISTRY.removeIf(p -> !p.isActive());
        return new ArrayList<>(REGISTRY);
    }

    private void execute(ExecutionLevel level) {

        if (properties.getJavaRelease() > MAX_JAVA_VERSION) {
            ProjectDialogManager.get(this).reportError(
                    Bundle.PraxisProject_javaVersionError(properties.getJavaRelease()));
            return;
        }

        List<Task> tasks = new ArrayList<>();

        if (!isActive()) {
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
                if (isActive()) {
                    REGISTRY.add(this);
                }
            });
        } else {
            actionsEnabled = true;
            activeExec = null;
            if (isActive()) {
                REGISTRY.add(this);
            }
        }

    }

    private void clean() {
        if (activeExec != null) {
            activeExec.cancel();
        }
        List<Task> tasks = List.of(hubManager.createShutdownTask());
        activeExec = new TaskExec(tasks);
        actionsEnabled = false;
        var execState = activeExec.execute();
        if (execState == Task.State.RUNNING) {
            activeExec.addPropertyChangeListener(e -> {
                actionsEnabled = true;
                activeExec = null;
                REGISTRY.removeIf(p -> !p.isActive());
            });
        } else {
            actionsEnabled = true;
            activeExec = null;
            REGISTRY.removeIf(p -> !p.isActive());
        }

    }

    void updateLibs(PArray libs) {
        clearLibs();
        libPath = List.copyOf(buildLibList(libs));
        libsCP = buildLibsClasspath(libPath);
        if (libsCP != null) {
            compileCP = ClassPathSupport.createProxyClassPath(libsCP,
                    CoreClassPathRegistry.getInstance().getCompileClasspath());
            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, new ClassPath[]{libsCP});
        }
    }

    private void clearLibs() {
        if (libsCP != null) {
            GlobalPathRegistry.getDefault().unregister(ClassPath.COMPILE, new ClassPath[]{libsCP});
        }
        libPath = List.of();
        libsCP = null;
        compileCP = CoreClassPathRegistry.getInstance().getCompileClasspath();
    }

    private List<URI> buildLibList(PArray path) {
        return path.stream()
                .flatMap(v -> PResource.from(v).stream())
                .map(PResource::value)
                .filter(uri -> "file".equals(uri.getScheme()))
                .collect(Collectors.toList());
    }

    private ClassPath buildLibsClasspath(List<URI> path) {
        try {
            return ClassPathSupport.createClassPath(
                    path.stream()
                            .map(File::new)
                            .map(FileUtil::urlForArchiveOrDir)
                            .toArray(URL[]::new)
            );
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private ExecutionElement fromModelElement(ProjectElement element) {
        try {
            if (element instanceof ProjectElement.File fileElement) {
                return ExecutionElement.forFile(FileUtil.toFileObject(Path.of(fileElement.file())));
            } else if (element instanceof ProjectElement.Line lineElement) {
                return ExecutionElement.forLine(lineElement.line());
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private ProjectElement toModelElement(ExecutionEntry entry) {
        ExecutionElement element = entry.element();
        ElementHandler handler = entry.handler();
        if (element instanceof ExecutionElement.File fileElement) {
            return ProjectElement.file(fileElement.file().toURI());
        } else if (element instanceof ExecutionElement.Line lineElement) {
            String line = lineElement.line();
            if (handler instanceof LineHandler lineHandler) {
                line = lineHandler.rewrite(line);
            }
            return ProjectElement.line(line);
        }
        throw new IllegalArgumentException();
    }

    private class ClassPathImpl implements ClassPathProvider {

        @Override
        public ClassPath findClassPath(FileObject file, String type) {
            switch (type) {
                case ClassPath.BOOT:
                case JavaClassPathConstants.MODULE_BOOT_PATH:
                    return CoreClassPathRegistry.getInstance().getBootClasspath();
                case ClassPath.COMPILE:
                    return compileCP;
                default:
                    return null;
            }
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
                ActionProvider.COMMAND_BUILD,
                ActionProvider.COMMAND_CLEAN
            };
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {

            if (ActionProvider.COMMAND_RUN.equals(command)) {
                execute(ExecutionLevel.RUN);
            } else if (ActionProvider.COMMAND_BUILD.equals(command)) {
                execute(ExecutionLevel.BUILD);
            } else if (ActionProvider.COMMAND_CLEAN.equals(command)) {
                clean();
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {

            if (ActionProvider.COMMAND_CLEAN.equals(command)) {
                return isActive();
            } else {
                return actionsEnabled;
            }

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
                ProjectDialogManager.get(DefaultPraxisProject.this)
                        .reportWarnings(warnings);
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
                    if (continueOnError(result.args())) {
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

        private boolean continueOnError(List<Value> args) {
            String pathOrCmd;
            if (element instanceof ExecutionElement.File) {
                var file = ((ExecutionElement.File) element).file();
                var path = FileUtil.getRelativePath(getProjectDirectory(), file);
                if (path == null) {
                    path = file.getPath();
                }
                pathOrCmd = path;
            } else if (element instanceof ExecutionElement.Line) {
                var cmd = ((ExecutionElement.Line) element).line();
                if (handler instanceof LineHandler) {
                    cmd = ((LineHandler) handler).rewrite(cmd)
                            .lines().limit(5).collect(Collectors.joining("\n"));
                }
                pathOrCmd = cmd;
            } else {
                pathOrCmd = "???"; // should never get here!
            }
            String extra = null;
            if (!args.isEmpty()) {
                extra = args.get(0).toString().lines().limit(5).collect(Collectors.joining("\n"));
            }
            String message = Bundle.ERR_elementExecution(pathOrCmd);
            if (extra != null) {
                message += "\n\n";
                message += extra;
            }
            String title = level == ExecutionLevel.RUN
                    ? Bundle.ERR_elementContinueRun()
                    : Bundle.ERR_elementContinueBuild();
            return ProjectDialogManager.get(DefaultPraxisProject.this)
                    .confirmOnError(title, message);
        }

    }
}
