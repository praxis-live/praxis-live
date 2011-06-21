/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.wizard;

import java.awt.Component;
import java.io.File;
import javax.swing.event.ChangeListener;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

class PXRWizardPanel1 implements WizardDescriptor.Panel {

    private PXRVisualPanel1 component;
    private ChangeSupport cs;
    private boolean valid;    
    private WizardDescriptor wizard;

    private String id;
    private File file;
    private ComponentType type;
    private boolean build;
    private boolean autostart;

    PXRWizardPanel1() {
        cs = new ChangeSupport(this);
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
        file = null;
        id = null;
        File loc = component.getFileFolder();
        String err = null;
        if (loc == null) {
            err = "No file location set";
        } else if (!loc.isDirectory() || !loc.exists() || !loc.canWrite()) {
            err = "File location must be an existing, writable directory";
        } else {
            String name = component.getRootID();
            if (!isValidName(name)) {
                err = "Root ID is invalid";
            } else {
                File f = new File(loc, name + ".pxr");
                if (f.exists()) {
                    err = "A file with this name already exists.";
                } else {
                    id = name;
                    file = f;
                    nowValid = true;
                }
            }
        }

        type = component.getType();
        if (type == null) {
            err = "Please choose a Root type";
            nowValid = false;
        }
        
        autostart = component.getAutostart();
        build = component.getBuild();
        if (nowValid != valid) {
            valid = nowValid;
            cs.fireChange();
        }
        if (wizard != null) {
            wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, err);
        }
    }

    private boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.trim().isEmpty()) {
            return false;
        } else {
            return ComponentAddress.isValidID(name);
        }
    }

    FileObject getTargetFolder() {
        if (wizard != null) {
            return Templates.getTargetFolder(wizard);
        }
        return null;
    }


    // You can use a settings object to keep track of state. Normally the
    // settings object will be the WizardDescriptor, so you can use
    // WizardDescriptor.getProperty & putProperty to store information entered
    // by the user.
    @Override
    public void readSettings(Object settings) {
        if (settings instanceof WizardDescriptor) {
            wizard = (WizardDescriptor) settings;
        }
    }

    @Override
    public void storeSettings(Object settings) {
        if (settings == wizard) {
            wizard.putProperty(PXRWizardIterator.PROP_PXR_ID, id);
            wizard.putProperty(PXRWizardIterator.PROP_PXR_FILE, file);
            wizard.putProperty(PXRWizardIterator.PROP_PXR_TYPE, type);
            wizard.putProperty(PXRWizardIterator.PROP_PXR_BUILD, build);
            wizard.putProperty(PXRWizardIterator.PROP_PXR_AUTOSTART, autostart);
        }
    }
}
