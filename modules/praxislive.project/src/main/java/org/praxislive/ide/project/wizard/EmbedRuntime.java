/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.project.wizard;

import java.awt.Component;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import org.netbeans.api.java.platform.JavaPlatform;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.core.embedder.CORE;
import org.praxislive.ide.project.api.PraxisProject;

import static java.nio.file.FileVisitResult.CONTINUE;
import java.util.Set;

@NbBundle.Messages({
    "TTL_ImportRuntime=Import Runtime",
    "ERR_RuntimeFoldersExist=Some runtime folders exist. Make sure there is no bin, mods or jdk folder in the project."
})
public final class EmbedRuntime {

    private static final EmbedRuntime INSTANCE = new EmbedRuntime();
    private static final RequestProcessor RP = new RequestProcessor(EmbedRuntime.class);

    private static final String BIN = "bin";
    private static final String ETC = "etc";
    private static final String MODS = "mods";
    private static final String JDK = "jdk";

    private EmbedRuntime() {
    }

    public void process(PraxisProject project) {
        var namePanel = new EmbedRuntimeNamePanel(project);
        var jdkPanel = new EmbedRuntimeJDKPanel();
        var panels = List.of(namePanel, jdkPanel);

        var steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            steps[i] = c.getName();
            if (c instanceof JComponent jc) {
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        var wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<>(panels));
        // {0} will be replaced by WizardDesriptor.Panel.getComponent().getName()
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle(Bundle.TTL_ImportRuntime());
        if (DialogDisplayer.getDefault().notify(wiz) == WizardDescriptor.FINISH_OPTION) {
            String launcherName = wiz.getProperty(EmbedRuntimeNamePanel.KEY_NAME).toString();
            boolean includeJDK = wiz.getProperty(EmbedRuntimeJDKPanel.KEY_JDK) instanceof Boolean b ? b : false;
            handleImport(project, launcherName, includeJDK);
        }
    }

    private void handleImport(PraxisProject project, String launcherName, boolean includeJDK) {
        RP.execute(() -> handleImportAsync(project.getProjectDirectory(), launcherName, includeJDK));
    }

    private void handleImportAsync(FileObject folder, String launcherName, boolean includeJDK) {
        if (folder.getFileObject(BIN) != null
                || folder.getFileObject(ETC) != null
                || folder.getFileObject(MODS) != null
                || folder.getFileObject(JDK) != null) {
            EventQueue.invokeLater(this::errorExists);
            return;
        }
        try {
            Path runtimePath = CORE.installDir();
            Path projectPath = FileUtil.toFile(folder).toPath();
            Path binPath = Files.createDirectory(projectPath.resolve(BIN));
            Path etcPath = Files.createDirectory(projectPath.resolve(ETC));
            Path modsPath = Files.createDirectory(projectPath.resolve(MODS));
            copyFiles(runtimePath.resolve(BIN), binPath);
            copyFiles(runtimePath.resolve(ETC), etcPath);
            copyFiles(runtimePath.resolve(MODS), modsPath);
            renameLaunchers(binPath, launcherName);
            if (includeJDK) {
                List<FileObject> jdkFolders = JavaPlatform.getDefault().getInstallFolders()
                        .stream()
                        .collect(Collectors.toList());
                if (jdkFolders.size() != 1) {
                    throw new IllegalStateException("Java platform has unsupported number of install folders");
                }
                Path jdkSrc = FileUtil.toFile(jdkFolders.get(0)).toPath();
                Path jdkDst = Files.createDirectory(projectPath.resolve(JDK));
                copyFiles(jdkSrc, jdkDst);
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void copyFiles(Path src, Path dst) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Path targetDir = dst.resolve(src.relativize(dir));
                try {
                    Files.copy(dir, targetDir);
                    ensureWritable(targetDir);
                } catch (FileAlreadyExistsException e) {
                    if (!Files.isDirectory(targetDir)) {
                        throw e;
                    }
                }
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Path targetFile = dst.resolve(src.relativize(file));
                Files.copy(file, targetFile,
                        StandardCopyOption.COPY_ATTRIBUTES);
                ensureWritable(targetFile);
                return CONTINUE;
            }
        });
    }

    private void renameLaunchers(Path binPath, String launcherName) throws IOException {
        Files.move(binPath.resolve("praxis"),
                binPath.resolve(launcherName));
        Files.move(binPath.resolve("praxis.cmd"),
                binPath.resolve(launcherName + ".cmd"));
    }

    private void errorExists() {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(Bundle.ERR_RuntimeFoldersExist(),
                        NotifyDescriptor.ERROR_MESSAGE));
    }

    private void ensureWritable(Path path) {
        if (Files.isWritable(path)) {
            return;
        }
        try {
            PosixFileAttributeView posix = Files.getFileAttributeView(path, PosixFileAttributeView.class);
            if (posix != null) {
                Set<PosixFilePermission> perms = posix.readAttributes().permissions();
                perms.add(PosixFilePermission.OWNER_WRITE);
                posix.setPermissions(perms);
                return;
            }
            DosFileAttributeView dos = Files.getFileAttributeView(path, DosFileAttributeView.class);
            if (dos != null) {
                dos.setReadOnly(false);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static EmbedRuntime getInstance() {
        return INSTANCE;
    }

}
