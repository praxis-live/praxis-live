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

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.praxislive.core.Value;
import org.praxislive.core.syntax.Token;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.hub.net.HubConfiguration;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.ide.project.spi.ElementHandler;
import org.praxislive.ide.project.spi.FileHandler;
import org.praxislive.ide.project.spi.LineHandler;
import org.praxislive.ide.project.ui.ProjectDialogManager;
import org.praxislive.ide.properties.SyntaxUtils;

import static org.praxislive.ide.project.api.ProjectProperties.PROP_LIBRARIES;
import static org.praxislive.ide.project.DefaultPraxisProject.MAX_JAVA_VERSION;
import static org.praxislive.ide.project.DefaultPraxisProject.MIN_JAVA_VERSION;

@NbBundle.Messages({
    "# {0} - library URI",
    "ERR_addLibraryError=Error adding library {0}"
})
public class ProjectPropertiesImpl implements ProjectProperties {

    private final static String DEFAULT_HUB_CONFIG;

    static {
        String config;
        try (var reader = new BufferedReader(
                new InputStreamReader(
                        ProjectPropertiesImpl.class.getResourceAsStream(
                                "/org/praxislive/ide/project/resources/default-hub.txt"),
                        Charset.forName("UTF-8")))) {
            config = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            config = "";
        }
        DEFAULT_HUB_CONFIG = config;
    }

    private final Map<ExecutionLevel, Map<ExecutionElement, ElementHandler>> elements;
    private final PropertyChangeSupport pcs;
    private final DefaultPraxisProject project;
    private final FileListener listener;
    private final HubLineHandler hubHandler;
    private final CompilerLineHandler compilerHandler;
    private final LibrariesLineHandler librariesHandler;
    private final List<URI> libraries;
    private final List<FileHandler.Provider> fileHandlerProviders;
    private final List<LineHandler.Provider> lineHandlerProviders;

    private int javaRelease = MIN_JAVA_VERSION;

    ProjectPropertiesImpl(DefaultPraxisProject project) {
        this.project = project;
        this.elements = new EnumMap<>(ExecutionLevel.class);
        for (ExecutionLevel level : ExecutionLevel.values()) {
            elements.put(level, new LinkedHashMap<>());
        }
        hubHandler = new HubLineHandler();
        compilerHandler = new CompilerLineHandler();
        librariesHandler = new LibrariesLineHandler();
        libraries = new ArrayList<>();
        fileHandlerProviders = new ArrayList<>(
                Lookup.getDefault().lookupAll(FileHandler.Provider.class));
        lineHandlerProviders = new ArrayList<>();
        lineHandlerProviders.add(this::findInternalHandler);
        lineHandlerProviders.addAll(Lookup.getDefault().lookupAll(LineHandler.Provider.class));
        pcs = new PropertyChangeSupport(this);
        listener = new FileListener();
        project.getProjectDirectory().addRecursiveListener(listener);
    }

    @Override
    public void setElements(ExecutionLevel level, List<ExecutionElement> elements) {
        if (level == ExecutionLevel.CONFIGURE) {
            throw new IllegalArgumentException("Changing configure level not currently supported");
        }
        final Set<ElementHandler> handlers = new HashSet<>();
        if (level == ExecutionLevel.BUILD) {
            handlers.addAll(this.elements.get(ExecutionLevel.CONFIGURE).values());
            handlers.addAll(this.elements.get(ExecutionLevel.RUN).values());
        } else {
            handlers.addAll(this.elements.get(ExecutionLevel.CONFIGURE).values());
            handlers.addAll(this.elements.get(ExecutionLevel.BUILD).values());
        }
        var existing = this.elements.get(level);
        var replacements = new LinkedHashMap<ExecutionElement, ElementHandler>(elements.size());
        for (var el : elements) {
            var handler = existing.get(el);
            if (handler == null) {
                handler = findHandler(level, el);
            }
            if (!handlers.add(handler)) {
                throw new IllegalArgumentException("Duplicate handler");
            }
            replacements.put(el, handler);
        }
        existing.clear();
        existing.putAll(replacements);
        pcs.firePropertyChange(PROP_ELEMENTS, null, null);
    }

    @Override
    public List<ExecutionElement> getElements(ExecutionLevel level) {
        return new ArrayList<>(elements.get(level).keySet());
    }

    @Override
    public PraxisProject getProject() {
        return project;
    }

    void initElements(Map<ExecutionLevel, List<ExecutionElement>> elementMap) {
        initConfigure(elementMap.get(ExecutionLevel.CONFIGURE));
        final Set<ElementHandler> handlers = new HashSet<>();
        handlers.addAll(elements.get(ExecutionLevel.CONFIGURE).values());
        initLevel(handlers, ExecutionLevel.BUILD, elementMap.get(ExecutionLevel.BUILD));
        initLevel(handlers, ExecutionLevel.RUN, elementMap.get(ExecutionLevel.RUN));
    }

    Map<ExecutionLevel, List<ExecutionEntry>> elements() {
        EnumMap<ExecutionLevel, List<ExecutionEntry>> map = new EnumMap<>(ExecutionLevel.class);
        map.put(ExecutionLevel.CONFIGURE, elements.get(ExecutionLevel.CONFIGURE)
                .entrySet()
                .stream()
                .map(e -> new ExecutionEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList())
        );
        map.put(ExecutionLevel.BUILD, elements.get(ExecutionLevel.BUILD)
                .entrySet()
                .stream()
                .map(e -> new ExecutionEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList())
        );
        map.put(ExecutionLevel.RUN, elements.get(ExecutionLevel.RUN)
                .entrySet()
                .stream()
                .map(e -> new ExecutionEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList())
        );
        return map;
    }

    private void initConfigure(List<ExecutionElement> elementList) {
        var map = elements.get(ExecutionLevel.CONFIGURE);
        map.clear();
        elementList.stream()
                .filter(ExecutionElement.Line.class::isInstance)
                .map(ExecutionElement.Line.class::cast)
                .filter(e -> hubHandler.isSupportedCommand(e.tokens().get(0).getText()))
                .findFirst()
                .ifPresentOrElse(e -> {
                    hubHandler.configure(e);
                    map.put(e, hubHandler);
                }, () -> {
                    map.put(hubHandler.defaultElement(), hubHandler);
                });
        elementList.stream()
                .filter(ExecutionElement.Line.class::isInstance)
                .map(ExecutionElement.Line.class::cast)
                .filter(e -> compilerHandler.isSupportedCommand(e.tokens().get(0).getText()))
                .findFirst()
                .ifPresentOrElse(e -> {
                    compilerHandler.configure(e);
                    map.put(e, compilerHandler);
                }, () -> {
                    map.put(compilerHandler.defaultElement(), compilerHandler);
                });
        elementList.stream()
                .filter(ExecutionElement.Line.class::isInstance)
                .map(ExecutionElement.Line.class::cast)
                .filter(e -> librariesHandler.isSupportedCommand(e.tokens().get(0).getText()))
                .findFirst()
                .ifPresentOrElse(e -> {
                    librariesHandler.configure(e);
                    map.put(e, librariesHandler);
                }, () -> {
                    map.put(librariesHandler.defaultElement(), librariesHandler);
                });

    }

    private void initLevel(Set<ElementHandler> handlers,
            ExecutionLevel level,
            List<ExecutionElement> elementList) {
        var map = this.elements.get(level);
        map.clear();
        for (var element : elementList) {
            try {
                var handler = findHandler(level, element);
                if (!handlers.add(handler)) {
                    throw new IllegalStateException("Duplicate handler");
                }
                map.put(element, handler);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private ElementHandler findHandler(ExecutionLevel level, ExecutionElement element)
            throws IllegalArgumentException {
        if (element instanceof ExecutionElement.File) {
            var fileElement = (ExecutionElement.File) element;
            checkFile(fileElement.file());
            return fileHandlerProviders.stream()
                    .flatMap(p -> p.createHandler(project, level, fileElement).stream())
                    .findFirst()
                    .orElseGet(() -> new DefaultFileHandler(project, level, fileElement));
        } else if (element instanceof ExecutionElement.Line) {
            var lineElement = (ExecutionElement.Line) element;
            return lineHandlerProviders.stream()
                    .flatMap(p -> p.createHandler(project, level, lineElement).stream())
                    .findFirst()
                    .orElseGet(() -> new DefaultLineHandler(project, level, lineElement));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private Optional<LineHandler> findInternalHandler(
            PraxisProject project,
            ExecutionLevel level,
            ExecutionElement.Line lineElement) {
        var tokens = lineElement.tokens();
        var command = tokens.get(0).getText();
        if (compilerHandler.isSupportedCommand(command)) {
            return Optional.of(compilerHandler);
        } else if (librariesHandler.isSupportedCommand(command)) {
            return Optional.of(librariesHandler);
        } else {
            return Optional.empty();
        }
    }

    public void setLibraries(List<URI> libs) {
        var working = new ArrayList<>(libs);
        var active = project.isActive();
        if (!active) {
            libraries.clear();
        }
        working.removeAll(libraries);
        if (working.isEmpty()) {
            return;
        }
        libraries.addAll(working);
        if (active) {
            try {
                var helper = project.getLookup().lookup(ProjectHelper.class);
                if (helper != null) {
                    for (var lib : working) {
                        var script = "set _PWD " + project.getProjectDirectory().toURI()
                                + "\n" + librariesHandler.librariesScript(List.of(lib));
                        helper.executeScript(script, Callback.create(r -> {
                            if (r.isError()) {
                                libraries.remove(lib);
                                var msg = Bundle.ERR_addLibraryError(lib);
                                if (!r.args().isEmpty()) {
                                    msg += ("\n" + r.args().get(0));
                                }
                                ProjectDialogManager.get(project)
                                        .reportError(msg);
                            }
                        }));
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        pcs.firePropertyChange(PROP_LIBRARIES, null, null);
    }

    @Override
    public List<URI> getLibraries() {
        return List.copyOf(libraries);
    }

    @Override
    public void setJavaRelease(int release) throws Exception {
        if (javaRelease == release) {
            return;
        }
        if (project.isActive()) {
            throw new IllegalStateException("Cannot change source version for active project");
        }
        if (release < MIN_JAVA_VERSION || release > MAX_JAVA_VERSION) {
            throw new IllegalArgumentException();
        }
        javaRelease = release;
        pcs.firePropertyChange(PROP_JAVA_RELEASE, null, null);
    }

    @Override
    public int getJavaRelease() {
        return javaRelease;
    }

    public void setHubConfiguration(String config) throws Exception {
        if (hubHandler.hubConfiguration.equals(config)) {
            return;
        }
        if (project.isActive()) {
            throw new IllegalStateException("Cannot change hub for active project");
        }
        hubHandler.configure(config);
    }

    public String getHubConfiguration() {
        return hubHandler.hubConfiguration;
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

    private class HubLineHandler implements LineHandler {

        private final String DEFAULT_HUB = "hub {\n"
                + DEFAULT_HUB_CONFIG
                + "\n}";

        private final Set<String> commands;

        private String hubConfiguration;

        private HubLineHandler() {
            commands = Set.of("hub-configure", "hub");
            hubConfiguration = DEFAULT_HUB_CONFIG;
        }

        @Override
        public void process(Callback callback) throws Exception {
            project.getLookup().lookup(ProjectHelper.class)
                    .executeScript("hub {\n" + hubConfiguration + "\n}",
                            callback);
        }

        @Override
        public String rewrite(String line) {
            return "hub {\n" + hubConfiguration + "\n}";
        }

        private boolean isSupportedCommand(String command) {
            return commands.contains(command);
        }

        private void configure(ExecutionElement.Line element) {
            try {
                var config = element.tokens().get(1).getText();
                configure(config);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private void configure(String config) throws Exception {
            config = config.lines()
                    .filter(Predicate.not(String::isBlank))
                    .collect(Collectors.joining("\n"));
            var checkConfig = HubConfiguration.fromMap(PMap.parse(config));
            this.hubConfiguration = config;
        }

        private ExecutionElement.Line defaultElement() {
            return ExecutionElement.forLine(DEFAULT_HUB);
        }
    }

    private class CompilerLineHandler implements LineHandler {

        private final Set<String> commands;

        private CompilerLineHandler() {
            commands = Set.of("java-compiler-release", "compiler");
        }

        @Override
        public void process(Callback callback) throws Exception {
            String script = compilerScript(getJavaRelease());
            project.getLookup().lookup(ProjectHelper.class).executeScript(script, callback);
        }

        @Override
        public String rewrite(String line) {
            return compilerScript(getJavaRelease());
        }

        private boolean isSupportedCommand(String command) {
            return commands.contains(command);
        }

        private void configure(ExecutionElement.Line element) {
            try {
                int release;
                if ("java-compiler-release".equals(element.tokens().get(0).getText())) {
                    release = (Integer.parseInt(element.tokens().get(1).getText()));
                } else {
                    var params = PMap.parse(element.tokens().get(1).getText());
                    release = params.getInt("release", MIN_JAVA_VERSION);
                }
                if (release < MIN_JAVA_VERSION) {
                    release = MIN_JAVA_VERSION;
                }
                javaRelease = release;
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private String compilerScript(int release) {
            return "compiler {\n  release " + release + "\n}";
        }

        private ExecutionElement.Line defaultElement() {
            return ExecutionElement.forLine(compilerScript(MIN_JAVA_VERSION));
        }
    }

    private class LibrariesLineHandler implements LineHandler {

        private final Set<String> commands;

        private LibrariesLineHandler() {
            commands = Set.of("add-libs", "libraries");
        }

        @Override
        public void process(Callback callback) throws Exception {
            String script = "set _PWD " + project.getProjectDirectory().toURI()
                    + "\n" + librariesScript(libraries);
            project.getLookup().lookup(ProjectHelper.class).executeScript(script, callback);
        }

        @Override
        public String rewrite(String line) {
            return librariesScript(libraries);
        }

        private boolean isSupportedCommand(String command) {
            return commands.contains(command);
        }

        private void configure(ExecutionElement.Line element) {
            libraries.clear();
            try {
                var token = element.tokens().get(1);
                if (token.getType() == Token.Type.SUBCOMMAND) {
                    // legacy line
                    libraries.add(new URI("config/libs/*.jar"));
                } else {
                    PArray arr = PArray.parse(element.tokens().get(1).getText());
                    //                URI parent = project.getProjectDirectory().toURI();
                    for (Value v : arr) {
                        URI lib = new URI(v.toString());
                        //                    lib = lib.resolve(parent);
                        libraries.add(lib);
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private ExecutionElement.Line defaultElement() {
            return ExecutionElement.forLine("libraries {}");
        }

        private String librariesScript(List<URI> libs) {
            if (libs.isEmpty()) {
                return "libraries {}";
            }
//            URI parent = project.getProjectDirectory().toURI();
            return libs.stream()
                    //                    .map(u -> u.relativize(parent))
                    .map(u -> SyntaxUtils.escape(u.toString()))
                    .collect(Collectors.joining("\n  ", "libraries {\n  ", "\n}"));
        }

    }

    private class FileListener extends FileChangeAdapter {

        @Override
        public void fileDeleted(FileEvent fe) {
            synchronized (ProjectPropertiesImpl.this) {
                EventQueue.invokeLater(() -> checkFileDeleted(fe.getFile()));
            }
        }

        private void checkFileDeleted(FileObject file) {
            boolean changed = false;
            for (var category : elements.entrySet()) {
                changed = category.getValue().keySet().removeIf(
                        el -> el instanceof ExecutionElement.File
                        && ((ExecutionElement.File) el).file().equals(file)
                );
            }
            if (changed) {
                pcs.firePropertyChange(PROP_ELEMENTS, null, null);
            }
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
//            synchronized (ProjectPropertiesImpl.this) {
//                pcs.firePropertyChange(PROP_ELEMENTS, null, null);
//            }
        }
    }
}
