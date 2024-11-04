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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.core.api.Task;

@NbBundle.Messages({
    "CTL_SaveAsTemplateAction=Save as Template...",
    "TTL_TemplateName=Enter template name",
    "LBL_TemplateName=Template name",
    "ERR_TemplateExists=A template with that name already exists"
})
@ActionID(
        category = "PXR", id = "org.praxislive.ide.pxr.SaveAsTemplateAction"
)
@ActionRegistration(
        displayName = "#CTL_SaveAsTemplateAction"
)
public final class SaveAsTemplateAction implements ActionListener {

    private static final String TEMPLATES_PATH = "Templates/Roots";
    private static final RequestProcessor RP = new RequestProcessor();

    private final PXRDataObject rootDOB;

    public SaveAsTemplateAction(PXRDataObject context) {
        this.rootDOB = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                Bundle.LBL_TemplateName(),
                Bundle.TTL_TemplateName()
        );
        dlg.setInputText(rootDOB.getPrimaryFile().getName());
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            String templateName = dlg.getInputText().strip();
            PXRRootProxy active = PXRRootRegistry.findRootForFile(rootDOB.getPrimaryFile());
            if (active != null) {
                Task.run(SaveTask.createSaveTask(Set.of(rootDOB)))
                        .thenRunAsync(() -> copyTemplate(templateName), RP);

            } else {
                RP.execute(() -> copyTemplate(templateName));
            }
        }
    }

    private void copyTemplate(String name) {
        try {
            FileObject templateFolder = FileUtil.createFolder(
                    FileUtil.getConfigRoot(), TEMPLATES_PATH);
            if (templateFolder.getFileObject(name + ".pxr") != null) {
                DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(
                                Bundle.ERR_TemplateExists(),
                                NotifyDescriptor.ERROR_MESSAGE));
                return;
            }
            FileObject template = 
                    FileUtil.copyFile(rootDOB.getPrimaryFile(), templateFolder, name);
            template.setAttribute("template", true);
            template.setAttribute("displayName", name);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
