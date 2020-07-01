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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.praxislive.core.services.ServiceUnavailableException;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.ide.project.spi.ElementHandler;
import org.praxislive.ide.project.spi.FileHandler;
import org.praxislive.ide.project.spi.LineHandler;

import static org.praxislive.ide.project.api.ProjectProperties.PROP_LIBRARIES;
import static org.praxislive.ide.project.DefaultPraxisProject.LIBS_COMMAND;
import static org.praxislive.ide.project.DefaultPraxisProject.MAX_JAVA_VERSION;
import static org.praxislive.ide.project.DefaultPraxisProject.MIN_JAVA_VERSION;

/**
 *
 */
public class ProjectPropertiesImpl implements ProjectProperties {

    private final Map<ExecutionLevel, Map<ExecutionElement, ElementHandler>> elements;
    private final PropertyChangeSupport pcs;
    private final DefaultPraxisProject project;
    private final FileListener listener;
    private final CompilerLineHandler compilerHandler;
    private final LibrariesLineHandler librariesHandler;
    private final List<FileHandler.Provider> fileHandlerProviders;
    private final List<LineHandler.Provider> lineHandlerProviders;

    private int javaRelease = MIN_JAVA_VERSION;

    ProjectPropertiesImpl(DefaultPraxisProject project) {
        this.project = project;
        this.elements = new EnumMap<>(ExecutionLevel.class);
        for (ExecutionLevel level : ExecutionLevel.values()) {
            elements.put(level, new LinkedHashMap<>());
        }
        compilerHandler = new CompilerLineHandler();
        librariesHandler = new LibrariesLineHandler();
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
                .filter(e -> librariesHandler.isSupportedCommand(e.tokens().get(0).getText()))
                .findFirst()
                .ifPresentOrElse(e -> {
                    librariesHandler.configure(e);
                    map.put(e, librariesHandler);
                }, () -> {
                    map.put(librariesHandler.defaultElement(), librariesHandler);
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

    public void importLibrary(FileObject lib) throws IOException {
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
                project.getLookup().lookup(ProjectHelper.class)
                        .executeScript(script, Callback.create(r -> {
                        }));
            } catch (HubUnavailableException | ServiceUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        project.registerLibs();
        pcs.firePropertyChange(PROP_LIBRARIES, null, null);
    }

    public void removeLibrary(String name) throws IOException {
        if (project.isActive()) {
            throw new IOException("Cannot delete library from active project");
        }
        // @TODO move off EDT
        FileObject libsFolder = project.getProjectDirectory()
                .getFileObject(DefaultPraxisProject.LIBS_PATH);
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
        if (release < MIN_JAVA_VERSION || release > MAX_JAVA_VERSION) {
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

    private class CompilerLineHandler implements LineHandler {

        private final Set<String> commands;

        private CompilerLineHandler() {
            commands = Set.of("java-compiler-release");
        }

        @Override
        public void process(Callback callback) throws Exception {
            String script = "java-compiler-release " + getJavaRelease();
            project.getLookup().lookup(ProjectHelper.class).executeScript(script, callback);
        }

        @Override
        public String rewrite(String line) {
            return "java-compiler-release " + getJavaRelease();
        }

        private boolean isSupportedCommand(String command) {
            return commands.contains(command);
        }

        private void configure(ExecutionElement.Line element) {
            try {
                setJavaRelease(Integer.parseInt(element.tokens().get(1).getText()));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private ExecutionElement.Line defaultElement() {
            return ExecutionElement.forLine("java-compiler-release " + MIN_JAVA_VERSION);
        }
    }

    private class LibrariesLineHandler implements LineHandler {

        private final Set<String> commands;

        private LibrariesLineHandler() {
            commands = Set.of("add-libs");
        }

        @Override
        public void process(Callback callback) throws Exception {
            String script = "set _PWD " + project.getProjectDirectory().toURI()
                    + "\n" + LIBS_COMMAND;
            project.getLookup().lookup(ProjectHelper.class).executeScript(script, callback);
        }

        @Override
        public String rewrite(String line) {
            return LIBS_COMMAND;
        }

        private boolean isSupportedCommand(String command) {
            return commands.contains(command);
        }

        private void configure(ExecutionElement.Line element) {
            // no op
        }

        private ExecutionElement.Line defaultElement() {
            return ExecutionElement.forLine(LIBS_COMMAND);
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
