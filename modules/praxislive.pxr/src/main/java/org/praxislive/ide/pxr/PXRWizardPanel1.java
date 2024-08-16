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
package org.praxislive.ide.pxr;

import java.awt.Component;
import java.nio.file.Files;
import java.util.Objects;
import javax.swing.event.ChangeListener;
import org.praxislive.core.ComponentAddress;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle.Messages;

@Messages({
    "ERR_InvalidRootID=Root ID is invalid",
    "ERR_RootFileExists=A root file with this name already exists"
})
class PXRWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor> {

    private final ChangeSupport cs;
    private final FileObject projectDir;
    private final FileObject templateFile;

    private PXRVisualPanel1 component;
    private boolean valid;
    private WizardDescriptor wizard;

    private String id;

    PXRWizardPanel1(FileObject projectDir, FileObject templateFile) {
        this.projectDir = Objects.requireNonNull(projectDir);
        this.templateFile = Objects.requireNonNull(templateFile);
        this.cs = new ChangeSupport(this);
    }

    @Override
    public Component getComponent() {
        if (component == null) {
            component = new PXRVisualPanel1(this);
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public final void addChangeListener(ChangeListener l) {
        cs.addChangeListener(l);
    }

    @Override
    public final void removeChangeListener(ChangeListener l) {
        cs.removeChangeListener(l);
    }

    void validate() {
        boolean nowValid = false;
        id = null;
        String err = null;
        String name = component.getRootID();
        if (!isValidName(name)) {
            err = Bundle.ERR_InvalidRootID();
        } else {
            if (Files.exists(FileUtil.toPath(projectDir).resolve(name + ".pxr"))) {
                err = Bundle.ERR_RootFileExists();
            } else {
                id = name;
                nowValid = true;
            }
        }

        if (nowValid != valid) {
            valid = nowValid;
            cs.fireChange();
        }
        if (wizard != null) {
            wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, err);
        }
    }

    private boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.strip().isEmpty()) {
            return false;
        } else {
            return ComponentAddress.isValidID(name);
        }
    }

    FileObject getTemplateFile() {
        return templateFile;
    }
    
    FileObject getProjectDir() {
        return projectDir;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
        wizard = settings;
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        if (settings == wizard) {
            wizard.putProperty(PXRWizardIterator.PROP_PXR_ID, id);
        }
    }
}
