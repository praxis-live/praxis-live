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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.WizardDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.project.SyntaxUtils;

@NbBundle.Messages({
    "CTL_SaveAsTemplateAction=Save as Template..."
})
@ActionID(
        category = "PXR", id = "org.praxislive.ide.pxr.SaveAsTemplateAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveAsTemplateAction"
)
public final class SaveAsTemplateAction implements ActionListener {

    private static final RequestProcessor RP = new RequestProcessor();

    private final PXRDataObject rootDOB;

    public SaveAsTemplateAction(PXRDataObject context) {
        this.rootDOB = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        Project project = FileOwnerQuery.getOwner(rootDOB.getPrimaryFile());
        ProjectProperties props = project == null ? null : project.getLookup().lookup(ProjectProperties.class);
        List<URI> libs = props == null ? List.of() : props.getLibraries();
        SaveAsTemplateWizard wizard = new SaveAsTemplateWizard(rootDOB.getPrimaryFile().getName(), libs);
        if (wizard.display() == WizardDescriptor.FINISH_OPTION) {
            FileObject destination = wizard.getDestination();
            String filename = wizard.getFileName();
            PArray exportLibs = wizard.getExportLibraries();
            PXRRootProxy active = PXRRootRegistry.findRootForFile(rootDOB.getPrimaryFile());
            if (active != null) {
                Task.run(SaveTask.createSaveTask(Set.of(rootDOB)))
                        .thenRunAsync(() -> copyTemplate(destination, filename, exportLibs), RP);
            } else {
                RP.execute(() -> copyTemplate(destination, filename, exportLibs));
            }
        }
    }

    private void copyTemplate(FileObject destination, String filename, PArray libs) {
        try {
            String templateContents = rootDOB.getPrimaryFile().asText();
            if (!libs.isEmpty()) {
                templateContents = "libraries " + SyntaxUtils.valueToToken(libs)
                        + "\n\n" + templateContents;
            }
            FileObject template = FileUtil.createData(destination, filename);
            try (OutputStreamWriter writer = new OutputStreamWriter(template.getOutputStream())) {
                writer.append(templateContents);
            }
            template.setAttribute("template", true);
            template.setAttribute("displayName", template.getName());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
