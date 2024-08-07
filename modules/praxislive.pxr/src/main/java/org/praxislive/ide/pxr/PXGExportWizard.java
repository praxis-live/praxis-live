/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.filechooser.FileSystemView;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;


@NbBundle.Messages({
    "PXGExportWizard.defaultLocation=PraxisLIVE Projects/Components",
    "PXGExportWizard.title=Export PXG",
    "PXGExportWizard.nonWritableDirectory=File location must be an existing, writable directory",
    "PXGExportWizard.fileExists=File already exists",
    "PXGExportWizard.invalidPaletteCategory=Palette category is invalid"
})
public final class PXGExportWizard {
  
    final static String KEY_FILE = "file";
    final static String KEY_PALETTE_CATEGORY = "palette-category";

    private String suggestedFileName;
    private String suggestedPaletteCategory;
    
    private File exportFile;
    private String paletteCategory;
    
    public PXGExportWizard() {
        this.suggestedFileName = "";
        this.suggestedPaletteCategory = "";
    }

    public void setSuggestedFileName(String suggestedFileName) {
        this.suggestedFileName = Objects.requireNonNull(suggestedFileName);
    }

    public String getSuggestedFileName() {
        return suggestedFileName;
    }

    public void setSuggestedPaletteCategory(String paletteCategory) {
        this.suggestedPaletteCategory = Objects.requireNonNull(paletteCategory);
    }
    
    public String getSuggestedPaletteCategory() {
        return suggestedPaletteCategory;
    }
    
    
    public Object display() {
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<>();
        panels.add(new PXGExportWizardPanel1(this));
        String[] steps = new String[panels.size()];
        for (int i = 0; i < panels.size(); i++) {
            Component c = panels.get(i).getComponent();
            // Default step name to component name of panel.
            steps[i] = c.getName();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent) c;
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, i);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, true);
                jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, true);
            }
        }
        WizardDescriptor wiz = new WizardDescriptor(new WizardDescriptor.ArrayIterator<WizardDescriptor>(panels));
        wiz.setTitleFormat(new MessageFormat("{0}"));
        wiz.setTitle(Bundle.PXGExportWizard_title());
        Object ret = DialogDisplayer.getDefault().notify(wiz);
        if (ret == WizardDescriptor.FINISH_OPTION) {
            exportFile = (File) wiz.getProperty(KEY_FILE);
            paletteCategory = (String) wiz.getProperty(KEY_PALETTE_CATEGORY);
        } else {
            exportFile = null;
            paletteCategory = null;
        }
        return ret;
    }
    
    public File getExportFile() {
        return exportFile;
    }
    
    public String getPaletteCategory() {
        return paletteCategory;
    }
    

    File getDefaultLocation() {
        File location = FileSystemView.getFileSystemView().getDefaultDirectory();
        if (location != null) {
            try {
                String[] path = Bundle.PXGExportWizard_defaultLocation().split("/");
                for (String dir : path) {
                    File d = new File(location, dir);
                    if (d.exists()) {
                        if (d.isDirectory() && d.canWrite()) {
                            location = d;
                        } else {
                            break;
                        }
                    } else {
                        if (d.mkdir()) {
                            location = d;
                        } else {
                            break;
                        }
                    }
                }
                return location;
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return FileUtil.normalizeFile(new File(System.getProperty("user.home")));
    }
    
}
