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

import java.awt.EventQueue;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.project.GraphModel;
import org.praxislive.project.ParseException;


@NbBundle.Messages({
    "TITLE_buildProject=Build project?",
    "# {0} - project name",
    "MSG_buildProject=Build project {0}?"
})
final class PXRWizardIterator implements TemplateWizard.Iterator {

    final static Logger LOG = Logger.getLogger(PXRWizardIterator.class.getName());

    public final static String PROP_PXR_ID = "PXR.id";
    public final static String PROP_PXR_FILE = "PXR.file";
    public final static String PROP_PXR_TYPE = "PXR.type";
    private int index;
    private WizardDescriptor.Panel[] panels;

    @Override
    public void initialize(TemplateWizard wizard) {
        FileObject projectDir = Templates.getProject(wizard).getProjectDirectory();
        FileObject templateFile = Templates.getTemplate(wizard);
        panels = new WizardDescriptor.Panel[]{
            new PXRWizardPanel1(projectDir, templateFile)
        };
    }

    @Override
    public Set<DataObject> instantiate(TemplateWizard wizard) throws IOException {
        try {
            String id = wizard.getProperty(PROP_PXR_ID).toString();
            Project project = Templates.getProject(wizard);
            FileObject projectDir = project.getProjectDirectory();
            FileObject templateFile = Templates.getTemplate(wizard);

            FileObject file = createFile(projectDir, templateFile, id);
            boolean build = true;
            boolean autostart = true;

            Runnable task = () -> {
                ProjectProperties props = project.getLookup().lookup(ProjectProperties.class);
                if (props != null) {
                    try {
                        props.addFile(ExecutionLevel.BUILD, file);
                        if (autostart) {
                            props.addLine(ExecutionLevel.RUN, "/" + id + ".start");
                        }
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }

                }

                if (build) {
                    buildFile(project, file);
                }
            };

            if (EventQueue.isDispatchThread()) {
                task.run();
            } else {

                try {
                    // need to invoke and wait or dialog is closed and project
                    // built automatically when wizard closes.
                    EventQueue.invokeAndWait(task);
                } catch (InterruptedException | InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return Collections.singleton(DataObject.find(file));
        } catch (ParseException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void uninitialize(TemplateWizard wizard) {
        panels = null;
    }

    @Override
    public WizardDescriptor.Panel current() {
        return panels[index];
    }

    @Override
    public String name() {
        return index + 1 + ". from " + panels.length;
    }

    @Override
    public boolean hasNext() {
        return index < panels.length - 1;
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    @Override
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    private FileObject createFile(FileObject projectDir, FileObject templateFile, String id) throws IOException, ParseException {
        String script = templateFile.asText();
        GraphModel model = GraphModel.parse(script);
        model = model.withRename(id);
        script = model.writeToString();
        Path file = FileUtil.toPath(projectDir).resolve(id + ".pxr");
        Files.writeString(file, script, StandardOpenOption.CREATE_NEW);
        return FileUtil.toFileObject(file);
    }

    private void buildFile(Project base, FileObject file) {
        try {
            var project = base.getLookup().lookup(PraxisProject.class);
            var helper = project.getLookup().lookup(PXRHelper.class);
            if (helper != null && helper.isConnected()) {
                var dob = DataObject.find(file);
                if (dob instanceof PXRDataObject) {

                    new PXRFileHandler(project, (PXRDataObject) dob).process(
                            Callback.create(r -> {
                            }));

                }
            } else {
                var name = ProjectUtils.getInformation(project).getDisplayName();
                var ret = DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Confirmation(Bundle.MSG_buildProject(name),
                                Bundle.TITLE_buildProject(),
                                NotifyDescriptor.YES_NO_OPTION));
                if (ret == NotifyDescriptor.YES_OPTION) {
                    var actions = project.getLookup().lookup(ActionProvider.class);
                    actions.invokeAction(ActionProvider.COMMAND_BUILD, Lookup.EMPTY);
                }
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @ProjectServiceProvider(service = PrivilegedTemplates.class, projectType = "org-praxislive-ide-project")
    public static class PrivilegedTemplatesImpl implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                "Templates/Roots/audio.pxr",
                "Templates/Roots/video.pxr",
                "Templates/Roots/gui.pxr",
                "Templates/Roots/data.pxr",
                "Templates/Roots/root.pxr"
            };
        }

    }

}
