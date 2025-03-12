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
package org.praxislive.ide.pxr;

import java.awt.Component;
import java.io.File;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PResource;

@NbBundle.Messages({
    "SaveAsTemplateWizard.title=Save as template",
    "SaveAsTemplateWizard.nonWritableDirectory=File location must be an existing, writable directory",
    "SaveAsTemplateWizard.fileExists=File already exists",})
final class SaveAsTemplateWizard {

    static final String TEMPLATES_PATH = "Templates/Roots";
    static final String KEY_DESTINATION = "folder";
    static final String KEY_FILE_NAME = "filename";
    static final String KEY_LIBRARIES = "libraries";

    private final PArray libs;
    private final String suggestedFileName;

    private FileObject destination;
    private String fileName;
    private boolean includeLibraries;

    SaveAsTemplateWizard(String suggestedFileName, List<URI> libs) {
        this.suggestedFileName = suggestedFileName;
        this.libs = libs.stream()
                .filter(lib -> lib.toString().startsWith("pkg:"))
                .map(PResource::of)
                .collect(PArray.collector());
    }

    Object display() {
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<>();
        panels.add(new SaveAsTemplateWizardPanel1(this));
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent jc) {
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<>(panels));
        wiz.setTitleFormat(new MessageFormat("{0}"));
        Object ret = DialogDisplayer.getDefault().notify(wiz);
        if (ret == WizardDescriptor.FINISH_OPTION) {
            destination = (FileObject) wiz.getProperty(KEY_DESTINATION);
            fileName = (String) wiz.getProperty(KEY_FILE_NAME);
            includeLibraries = (boolean) wiz.getProperty(KEY_LIBRARIES);

        } else {
            destination = null;
            fileName = null;
            includeLibraries = false;
        }
        return ret;
    }

    FileObject getDestination() {
        return destination;
    }

    String getFileName() {
        return fileName;
    }

    PArray getExportLibraries() {
        return includeLibraries ? libs : PArray.EMPTY;
    }

    String getSuggestedFileName() {
        return suggestedFileName;
    }

    boolean hasLibraries() {
        return !libs.isEmpty();
    }

    boolean mightUseLibraries() {
        // @TODO search for imports in code
        return hasLibraries();
    }

    FileObject getDefaultDestination() {
        try {
            return FileUtil.createFolder(FileUtil.getConfigRoot(), TEMPLATES_PATH);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return FileUtil.toFileObject(new File(System.getProperty("user.home")));
        }
    }
}
