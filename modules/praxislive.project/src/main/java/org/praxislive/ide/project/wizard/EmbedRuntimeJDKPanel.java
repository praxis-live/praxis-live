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

import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

class EmbedRuntimeJDKPanel implements WizardDescriptor.Panel<WizardDescriptor> {

    final static String KEY_JDK = "EmbedJDK";

    private EmbedRuntimeJDKVisual component;
    
    @Override
    public EmbedRuntimeJDKVisual getComponent() {
        if (component == null) {
            component = new EmbedRuntimeJDKVisual();
        }
        return component;
    }

    @Override public HelpCtx getHelp() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override public boolean isValid() {
        return true;
    }

    @Override public void addChangeListener(ChangeListener l) {
    }

    @Override public void removeChangeListener(ChangeListener l) {
    }

    @Override public void readSettings(WizardDescriptor wiz) {
        
    }

    @Override public void storeSettings(WizardDescriptor wiz) {
        wiz.putProperty(KEY_JDK, getComponent().includeJDK());
    }

}
