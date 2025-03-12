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

import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

class SaveAsTemplateWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor> {

    private final ChangeSupport cs;
    private final SaveAsTemplateWizard wizard;
    private SaveAsTemplateVisualPanel1 component;
    private boolean valid;
    private WizardDescriptor wiz;

    private FileObject destination;
    private String filename;
    private boolean includeLibs;

    SaveAsTemplateWizardPanel1(SaveAsTemplateWizard exporter) {
        this.wizard = exporter;
        this.cs = new ChangeSupport(this);
    }

    @Override
    public SaveAsTemplateVisualPanel1 getComponent() {
        if (component == null) {
            component = new SaveAsTemplateVisualPanel1(this);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {
        this.wiz = wiz;
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        wiz.putProperty(SaveAsTemplateWizard.KEY_DESTINATION, destination);
        wiz.putProperty(SaveAsTemplateWizard.KEY_FILE_NAME, filename);
        wiz.putProperty(SaveAsTemplateWizard.KEY_LIBRARIES, includeLibs);
    }

    SaveAsTemplateWizard getWizard() {
        return wizard;
    }

    void validate() {
        if (component == null) {
            return;
        }
        destination = null;
        filename = null;
        includeLibs = false;
        boolean nowValid = false;

        FileObject dest = component.getDestinationFolder();
        String err = null;

        String name = component.getFileName();
        if (name.isEmpty()) {
            // empty name ??
        } else if (dest == null || !dest.isFolder() || !dest.canWrite()) {
            err = Bundle.SaveAsTemplateWizard_nonWritableDirectory();
        } else {
            if (!name.endsWith(".pxx")) {
                name = name + ".pxx";
            }
            if (dest.getFileObject(name) == null) {
                destination = dest;
                filename = name;
                includeLibs = component.includeLibraries();
                nowValid = true;
            } else {
                err = Bundle.SaveAsTemplateWizard_fileExists();
            }
        }

        wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, err);

        valid = nowValid;
        cs.fireChange();

    }

}
