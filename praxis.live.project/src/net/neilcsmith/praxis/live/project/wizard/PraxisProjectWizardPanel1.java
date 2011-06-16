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
package net.neilcsmith.praxis.live.project.wizard;

import java.awt.Component;
import java.io.File;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;

class PraxisProjectWizardPanel1 implements WizardDescriptor.Panel {

    public final static Pattern VALID_NAME = Pattern.compile("[ a-zA-Z0-9_-]+");

    private PraxisProjectVisualPanel1 component;
    private ChangeSupport cs;
    private boolean valid;
    private File project;
    private WizardDescriptor wizard;

    PraxisProjectWizardPanel1() {
        cs = new ChangeSupport(this);
    }


    @Override
    public Component getComponent() {
        if (component == null) {
            component = new PraxisProjectVisualPanel1(this);
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
        project = null;
        File loc = component.getProjectLocation();
        String err = null;
        if (loc == null) {
            err = "No project location set";
        } else if (!loc.isDirectory() || !loc.exists() || !loc.canWrite()) {
            err = "Project location must be an existing, writable directory";
        } else {
            String name = component.getProjectName();
            if (!isValidName(name)) {
                err = "Project name is invalid";
            } else {
                File f = new File(loc, name);
                if (f.exists()) {
                    err = "A file or directory with this name already exists.";
                } else {
                    project = f;
                    nowValid = true;
                }
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
        if (name == null || name.isEmpty() || name.trim().isEmpty()) {
            return false;
        } else {
            return VALID_NAME.matcher(name).matches();
        }
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
            wizard.putProperty("ProjectDirectory", project);
        }
    }
}
