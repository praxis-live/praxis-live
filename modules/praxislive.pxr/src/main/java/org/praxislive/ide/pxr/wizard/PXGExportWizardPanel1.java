/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.pxr.wizard;

import java.io.File;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.ChangeSupport;
import org.openide.util.HelpCtx;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ValueFormatException;

class PXGExportWizardPanel1 implements WizardDescriptor.Panel<WizardDescriptor> {

    private final ChangeSupport cs;
    private final PXGExportWizard exporter;
    private PXGExportVisualPanel1 component;
    private boolean valid;
    private WizardDescriptor wiz;

    private File file;
    private String paletteCategory;

    PXGExportWizardPanel1(PXGExportWizard exporter) {
        this.exporter = exporter;
        this.cs = new ChangeSupport(this);
    }

    @Override
    public PXGExportVisualPanel1 getComponent() {
        if (component == null) {
            component = new PXGExportVisualPanel1(this);
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
        wiz.putProperty(PXGExportWizard.KEY_FILE, file);
        wiz.putProperty(PXGExportWizard.KEY_PALETTE_CATEGORY, paletteCategory);

    }

    File getDefaultLocation() {
        return exporter.getDefaultLocation();
    }

    String getSuggestedFileName() {
        return exporter.getSuggestedFileName();
    }

    String getSuggestedPaletteCategory() {
        return exporter.getSuggestedPaletteCategory();
    }

    void validate() {
        if (component == null) {
            return;
        }
        boolean nowValid = false;
        file = null;
        paletteCategory = null;
        File loc = component.getFileLocation();
        String err = null;

        String name = component.getFileName();
        if (name.isEmpty()) {
            // empty name ??
        } else if (loc == null || !loc.isDirectory() || !loc.canWrite()) {
            err = Bundle.PXGExportWizard_nonWritableDirectory();
        } else {
            if (!name.endsWith(".pxg")) {
                name = name + ".pxg";
            }
            File f = new File(loc, name);
            if (f.exists()) {
                err = Bundle.PXGExportWizard_fileExists();
            } else {
                file = f;
                nowValid = true;
            }
        }

        paletteCategory = component.getPaletteCategory().trim();
        if (!paletteCategory.isEmpty()) {
            try {
                ComponentType test = ComponentType.of(paletteCategory + ":test");
            } catch (Exception ex) {
                err = Bundle.PXGExportWizard_invalidPaletteCategory();
                nowValid = false;
            }
        }

        wiz.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, err);

        valid = nowValid;
        cs.fireChange();

    }

}
