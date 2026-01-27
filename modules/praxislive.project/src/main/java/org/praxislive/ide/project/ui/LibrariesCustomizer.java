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
package org.praxislive.ide.project.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.praxislive.ide.project.DefaultPraxisProject;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.praxislive.core.types.PArray;

/**
 *
 */
@NbBundle.Messages({
    "TTL_import=Import library",
    "LBL_import=Import",
    "TTL_libraryURI=Add library",
    "LBL_libraryURI=Enter a library URI, path or Package URL",
    "HLP_libraryURI=PURL - pkg:maven/[group]/[artifact]@[version]"
})
class LibrariesCustomizer extends javax.swing.JPanel {

    private final ProjectCustomizer.Category category;
    private final DefaultPraxisProject project;
    private final Map<URI, FileObject> filesToImport;

    private boolean changed;

    LibrariesCustomizer(ProjectCustomizer.Category category, DefaultPraxisProject project) {
        this.category = Objects.requireNonNull(category);
        this.project = Objects.requireNonNull(project);
        filesToImport = new LinkedHashMap<>();
        initComponents();
        refresh();
        libsTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                textChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                textChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                textChanged();
            }
        });
    }

    final void refresh() {
        filesToImport.clear();
        libsTextArea.setEditable(!project.isActive());
        libsTextArea.setText(libsToText(project.getProperties().getLibraries()));
        changed = false;
    }

    final void updateProject() {
        if (changed) {
            try {
                List<URI> libs = textToLibs(libsTextArea.getText());
                copyFiles(libs);
                project.getProperties().setLibraries(libs);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void copyFiles(List<URI> libs) throws IOException {
        try {
            List<FileObject> files = filesToImport.entrySet().stream()
                    .filter(e -> libs.contains(e.getKey()))
                    .map(e -> e.getValue())
                    .toList();
            if (files.isEmpty()) {
                return;
            }
            FileObject libFolder = FileUtil.createFolder(project.getProjectDirectory(), "libs");
            for (FileObject libFile : files) {
                FileUtil.copyFile(libFile, libFolder, libFile.getName());
            }
        } finally {
            filesToImport.clear();
        }
    }

    private String libsToText(List<URI> libs) {
        URI parent = project.getProjectDirectory().toURI();
        return libs.stream()
                .map(parent::relativize)
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    private List<URI> textToLibs(String text) throws Exception {
        return PArray.parse(text)
                .asListOf(String.class)
                .stream()
                .map(URI::create)
                .toList();
    }

    private void textChanged() {
        try {
            textToLibs(libsTextArea.getText());
            category.setValid(true);
            changed = true;
        } catch (Exception ex) {
            category.setValid(false);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        importButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        scrollPane = new javax.swing.JScrollPane();
        libsTextArea = new javax.swing.JTextArea();
        resetButton = new javax.swing.JButton();

        importButton.setText(org.openide.util.NbBundle.getMessage(LibrariesCustomizer.class, "LibrariesCustomizer.importButton.text")); // NOI18N
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
            }
        });

        addButton.setText(org.openide.util.NbBundle.getMessage(LibrariesCustomizer.class, "LibrariesCustomizer.addButton.text")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        libsTextArea.setColumns(20);
        libsTextArea.setRows(5);
        scrollPane.setViewportView(libsTextArea);

        resetButton.setText(org.openide.util.NbBundle.getMessage(LibrariesCustomizer.class, "LibrariesCustomizer.resetButton.text")); // NOI18N
        resetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(importButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(resetButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(importButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resetButton)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed
        importFiles();
    }//GEN-LAST:event_importButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        addURI();
    }//GEN-LAST:event_addButtonActionPerformed

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonActionPerformed
        refresh();
    }//GEN-LAST:event_resetButtonActionPerformed

    private void importFiles() {
        FileChooserBuilder fcb = new FileChooserBuilder(LibrariesCustomizer.class)
                .setFilesOnly(true)
                .setTitle(Bundle.TTL_import())
                .setApproveText(Bundle.LBL_import())
                .forceUseOfDefaultWorkingDirectory(true)
                .setAcceptAllFileFilterUsed(true)
                .setFileFilter(new FileNameExtensionFilter("JAR files", "jar"));
        File[] files = fcb.showMultiOpenDialog();
        if (files != null) {
            for (File file : files) {
                FileObject fo = FileUtil.toFileObject(file);
                if (FileUtil.isParentOf(project.getProjectDirectory(), fo)) {
                    if (fo.hasExt("jar")) {
                        URI parent = project.getProjectDirectory().toURI();
                        checkAndAddURI(parent.relativize(fo.toURI()));
                    }
                } else {
                    if (fo.hasExt("jar")) {
                        try {
                            URI uri = new URI(null, null, "libs/" + file.getName(), null);
                            if (checkAndAddURI(uri)) {
                                filesToImport.put(uri, fo);
                            }
                        } catch (URISyntaxException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }
        }
    }

    private void addURI() {
        var input = new NotifyDescriptor.InputLine(Bundle.LBL_libraryURI(), Bundle.TTL_libraryURI());
        input.createNotificationLineSupport().setInformationMessage(Bundle.HLP_libraryURI());
        Object ret = DialogDisplayer.getDefault().notify(input);
        if (ret == NotifyDescriptor.OK_OPTION) {
            try {
                checkAndAddURI(new URI(input.getInputText().trim()));
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private boolean checkAndAddURI(URI lib) {
        try {
            List<URI> existing = textToLibs(libsTextArea.getText());
            if (existing.contains(lib)) {
                return false;
            } else {
                libsTextArea.append((existing.isEmpty() ? "" : "\n") + lib);
                return true;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton importButton;
    private javax.swing.JTextArea libsTextArea;
    private javax.swing.JButton resetButton;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

}
