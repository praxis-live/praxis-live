/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.support.GenericSources;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.ide.code.api.DynamicPaths;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.project.spi.ui.ProjectNodeDecorator;

@ProjectServiceProvider(projectType = PraxisProject.TYPE,
        service = {Sources.class, ProjectOpenedHook.class, ProjectNodeDecorator.class})
public class SharedCodeSources extends ProjectOpenedHook implements Sources, ProjectNodeDecorator {

    private final PraxisProject project;
    private final ChangeSupport cs;
    private final Listener listener;
    private final Map<FileObject, Info> sharedSources;

    private record Info(SourceGroup sourceGroup, DynamicPaths.SharedKey sharedKey) {}

    public SharedCodeSources(Lookup lookup) {
        this.project = Objects.requireNonNull(lookup.lookup(PraxisProject.class));
        this.cs = new ChangeSupport(this);
        this.listener = new Listener();
        this.sharedSources = new HashMap<>();
    }

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        if (JavaProjectConstants.SOURCES_TYPE_JAVA.equals(type)) {
            synchronized (type) {
                return sharedSources.values().stream()
                        .map(Info::sourceGroup)
                        .toArray(SourceGroup[]::new);
            }
        } else {
            return new SourceGroup[0];
        }
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        cs.addChangeListener(cl);
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        cs.removeChangeListener(cl);
    }

    @Override
    public Optional<Node> decorate(Node node) {
        FileObject fob = node.getLookup().lookup(FileObject.class);
        if (fob != null) {
            Info info;
            synchronized (this) {
                info = sharedSources.get(fob);
            }
            if (info != null) {
                return Optional.of(new SourceGroupNode(node));
            }
        }
        return Optional.empty();
    }

    @Override
    protected synchronized void projectOpened() {
        project.getProjectDirectory().addFileChangeListener(listener);
        FileObject codeFolder = project.getProjectDirectory().getFileObject("code");
        if (codeFolder != null && codeFolder.isFolder()) {
            codeFolder.addFileChangeListener(listener);
        }
        refresh();
    }

    @Override
    protected synchronized void projectClosed() {
        project.getProjectDirectory().removeFileChangeListener(listener);
        FileObject codeFolder = project.getProjectDirectory().getFileObject("code");
        if (codeFolder != null && codeFolder.isFolder()) {
            codeFolder.removeFileChangeListener(listener);
        }
        sharedSources.entrySet().forEach(e -> e.getValue().sharedKey().unregister());
        sharedSources.clear();
    }

    private synchronized void refresh() {
        Set<FileObject> sourceFolders = new HashSet<>();
        FileObject codeFolder = project.getProjectDirectory().getFileObject("code");
        if (codeFolder != null && codeFolder.isFolder()) {
            for (FileObject child : codeFolder.getChildren()) {
                if (child.isFolder()) {
                    sourceFolders.add(child);
                }
            }
        }
        if (sourceFolders.equals(sharedSources.keySet())) {
            return;
        }
        sharedSources.entrySet().removeIf(e -> {
            if (!sourceFolders.contains(e.getKey())) {
                e.getValue().sharedKey().unregister();
                return true;
            } else {
                return false;
            }
        });
        sourceFolders.forEach(folder -> {
            sharedSources.computeIfAbsent(folder, f -> {
                SourceGroup sg = GenericSources.group(project, f, f.getName(),
                        f.getName(), null, null);
                DynamicPaths.SharedKey sk = DynamicPaths.getDefault()
                        .registerShared(project, f);
                return new Info(sg, sk);
            });
        });
        EventQueue.invokeLater(cs::fireChange);
    }

    private class Listener extends FileChangeAdapter {

        @Override
        public void fileFolderCreated(FileEvent fe) {
            FileObject folder = fe.getFile();
            if (isCodeFolder(folder)) {
                folder.addFileChangeListener(this);
                refresh();
            } else if (isCodeFolder(folder.getParent())) {
                refresh();
            }
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            FileObject file = fe.getFile();
            if (isCodeFolder(file) || isCodeFolder(file.getParent())) {
                refresh();
            }
        }

        @Override
        public void fileRenamed(FileRenameEvent fre) {
            FileObject file = fre.getFile();
            if (isCodeFolder(file)) {
                file.addFileChangeListener(this);
                refresh();
            } else if (Objects.equals(project.getProjectDirectory(), file.getParent())
                    && fre.getName().equals("code")) {
                file.removeFileChangeListener(this);
                refresh();
            } else if (isCodeFolder(file.getParent())) {
                refresh();
            }
        }

        private boolean isCodeFolder(FileObject file) {
            return file != null && file.isFolder()
                    && Objects.equals("code", file.getName())
                    && Objects.equals(project.getProjectDirectory(), file.getParent());
        }

    }

    private static class SourceGroupNode extends FilterNode {

        private SourceGroupNode(Node original) {
            super(original, new Children(original));
        }

        private static class Children extends FilterNode.Children {

            private Children(Node node) {
                super(node);
            }

            @Override
            protected Node copyNode(Node node) {
                if ("SHARED".equals(node.getName())) {
                    return new SharedPackageNode(node);
                } else {
                    return new UnknownPackageNode(node);
                }
            }

        }

    }

    private static class SharedPackageNode extends FilterNode {

        private SharedPackageNode(Node original) {
            super(original, new Children(original), new ProxyLookup(
                    Lookups.singleton(new PrivilegedCodeTemplates()),
                    Lookups.exclude(original.getLookup(), PrivilegedTemplates.class)
            ));
        }

        private static class Children extends FilterNode.Children {

            private Children(Node node) {
                super(node);
            }

            @Override
            protected Node copyNode(Node node) {
                return super.copyNode(node);
            }

        }

    }

    private static class UnknownPackageNode extends FilterNode {

        private UnknownPackageNode(Node original) {
            super(original);
        }

        @Override
        public String getHtmlDisplayName() {
            return "<s>" + getDisplayName() + "</s>";
        }

    }

    private static class PrivilegedCodeTemplates implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                "Templates/Code/Class.java",
                "Templates/Code/Interface.java",
                "Templates/Code/Enum.java",
                "Templates/Code/Record.java",};
        }

    }

}
