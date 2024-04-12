/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
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

import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.ProjectUtils;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.praxislive.ide.project.api.PraxisProject;

@NbBundle.Messages({
    "ERR_InvalidName=The launcher name is invalid."
})
class EmbedRuntimeNamePanel implements WizardDescriptor.Panel<WizardDescriptor> {

    // same as project name regex - @TODO review both, in sync
    final static Pattern VALID_NAME = Pattern.compile("[ a-zA-Z0-9_-]+");
    final static String KEY_NAME = "LauncherName";

    private final PraxisProject project;
    private final ChangeSupport cs;
    
    private EmbedRuntimeNameVisual component;
    private boolean valid;
    private WizardDescriptor wizard;
    
    EmbedRuntimeNamePanel(PraxisProject project) {
        this.project = project;
        cs = new ChangeSupport(this);
    }
    
    
    @Override
    public EmbedRuntimeNameVisual getComponent() {
        if (component == null) {
            var name = ProjectUtils.getInformation(project).getName();
            valid = !name.isBlank();
            component = new EmbedRuntimeNameVisual(this, name);
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
        wizard = wiz;
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        wiz.putProperty(KEY_NAME, getComponent().getLauncherName());
    }
    
    void validate() {
        boolean nowValid = isValidName(getComponent().getLauncherName());
        if (nowValid != valid) {
            valid = nowValid;
            cs.fireChange();
            if (wizard != null) {
                wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE,
                        valid ? null : Bundle.ERR_InvalidName());
            }
        }
    }

    private boolean isValidName(String name) {
        return !name.isBlank() && VALID_NAME.matcher(name).matches();
    }
    
}
