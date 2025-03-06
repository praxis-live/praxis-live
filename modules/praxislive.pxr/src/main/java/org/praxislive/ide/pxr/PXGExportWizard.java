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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.filechooser.FileSystemView;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;
import org.praxislive.project.SyntaxUtils;

@NbBundle.Messages({
    "PXGExportWizard.defaultLocation=PraxisLIVE Projects/Components",
    "PXGExportWizard.title=Export PXG",
    "PXGExportWizard.nonWritableDirectory=File location must be an existing, writable directory",
    "PXGExportWizard.fileExists=File already exists",
    "PXGExportWizard.invalidPaletteCategory=Palette category is invalid"
})
final class PXGExportWizard {

    static final String KEY_FILE = "file";
    static final String KEY_PALETTE_CATEGORY = "palette-category";
    static final String KEY_LIBRARIES = "libraries";
    static final String KEY_SHARED_CODE = "shared-code";

    private final GraphModel model;
    private final PArray libs;
    private final PMap sharedCode;
    private final String suggestedFileName;
    private final String suggestedPaletteCategory;

    private GraphModel exportModel;
    private File exportFile;
    private String paletteCategory;

    PXGExportWizard(GraphModel model, PArray libs, PMap sharedCode) {
        this.model = model;
        this.libs = libs.stream()
                .filter(lib -> lib.toString().startsWith("pkg:"))
                .collect(PArray.collector());
        this.sharedCode = sharedCode;
        this.suggestedFileName = findSuggestedName(model.root());
        this.suggestedPaletteCategory = findPaletteCategory(model.root());
    }

    Object display() {
        List<WizardDescriptor.Panel<WizardDescriptor>> panels = new ArrayList<>();
        panels.add(new PXGExportWizardPanel1(this));
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
        wiz.setTitle(Bundle.PXGExportWizard_title());
        Object ret = DialogDisplayer.getDefault().notify(wiz);
        if (ret == WizardDescriptor.FINISH_OPTION) {
            exportFile = (File) wiz.getProperty(KEY_FILE);
            paletteCategory = (String) wiz.getProperty(KEY_PALETTE_CATEGORY);
            boolean addLibs = (boolean) wiz.getProperty(KEY_LIBRARIES);
            boolean addSharedCode = (boolean) wiz.getProperty(KEY_SHARED_CODE);
            if (addLibs || addSharedCode) {
                exportModel = model.withTransform(root -> {
                    if (!libs.isEmpty()) {
                        root.command("$LIBRARIES " + SyntaxUtils.valueToToken(libs));
                    }
                    if (!sharedCode.isEmpty()) {
                        root.command("$SHARED_CODE " + SyntaxUtils.valueToToken(sharedCode));
                    }
                });
            } else {
                exportModel = model;
            }
        } else {
            exportFile = null;
            paletteCategory = null;
        }
        return ret;
    }

    File getExportFile() {
        return exportFile;
    }

    GraphModel getExportModel() {
        return exportModel;
    }

    String getPaletteCategory() {
        return paletteCategory;
    }

    String getSuggestedFileName() {
        return suggestedFileName;
    }

    String getSuggestedPaletteCategory() {
        return suggestedPaletteCategory;
    }

    boolean hasLibraries() {
        return !libs.isEmpty();
    }

    boolean hasSharedCode() {
        return !sharedCode.isEmpty();
    }

    boolean mightUseLibraries() {
        // @TODO search for imports in code
        return hasLibraries();
    }

    boolean mightUseSharedCode() {
        // @TODO search for SHARED in code
        return hasSharedCode();
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

    private String findSuggestedName(GraphElement.Root root) {
        if (root.children().size() == 1) {
            return root.children().firstEntry().getKey();
        } else {
            return "";
        }
    }

    private String findPaletteCategory(GraphElement.Root root) {
        String ret = "core:custom";
        for (GraphElement.Component cmp : root.children().sequencedValues()) {
            if (!cmp.children().isEmpty()) {
                // container ??
                return "";
            }
            String type = cmp.type().toString();
            if (type.startsWith("video:gl:")) {
                // short circuit for GL
                return "video:gl:custom";
            } else if (!type.startsWith("core")) {
                String base = type.substring(0, type.indexOf(":"));
                ret = base + ":custom";
            }
        }
        return ret;
    }

}
