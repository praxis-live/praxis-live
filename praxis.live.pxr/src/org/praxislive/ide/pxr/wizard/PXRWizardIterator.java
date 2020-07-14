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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.pxr.PXRDataObject;
import org.praxislive.ide.pxr.PXRFileHandler;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.praxislive.ide.project.api.ProjectProperties;

public final class PXRWizardIterator implements WizardDescriptor.InstantiatingIterator {

    final static Logger LOG = Logger.getLogger(PXRWizardIterator.class.getName());

    public final static String PROP_PXR_ID = "PXR.id";
    public final static String PROP_PXR_FILE = "PXR.file";
    public final static String PROP_PXR_TYPE = "PXR.type";
    private int index;
    private WizardDescriptor wizard;
    private WizardDescriptor.Panel[] panels;
    private ComponentType rootType;

    public PXRWizardIterator() {
        LOG.fine("Creating PXRWizardIterator");
    }

    /**
     * Initialize panels representing individual wizard's steps and sets various
     * properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[]{
                new PXRWizardPanel1(rootType)
            };

            // @TODO - this seems to break Templates.getTargetFolder() ???
//            String[] steps = createSteps();
//            for (int i = 0; i < panels.length; i++) {
//                Component c = panels[i].getComponent();
//                if (steps[i] == null) {
//                    // Default step name to component name of panel. Mainly
//                    // useful for getting the name of the target chooser to
//                    // appear in the list of steps.
//                    steps[i] = c.getName();
//                }
//                if (c instanceof JComponent) { // assume Swing components
//                    JComponent jc = (JComponent) c;
//                    // Sets step number of a component
//                    // TODO if using org.openide.dialogs >= 7.8, can use WizardDescriptor.PROP_*:
//                    jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i));
//                    // Sets steps names for a panel
//                    jc.putClientProperty("WizardPanel_contentData", steps);
//                    // Turn on subtitle creation on each step
//                    jc.putClientProperty("WizardPanel_autoWizardStyle", Boolean.TRUE);
//                    // Show steps on the left side with the image on the background
//                    jc.putClientProperty("WizardPanel_contentDisplayed", Boolean.TRUE);
//                    // Turn on numbering of all steps
//                    jc.putClientProperty("WizardPanel_contentNumbered", Boolean.TRUE);
//                }
//            }
        }
        return panels;
    }

    @Override
    public Set instantiate() throws IOException {
        Object obj = wizard.getProperty(PROP_PXR_ID);
        String id;
        if (obj instanceof String) {
            id = (String) obj;
        } else {
            throw new IOException("No id in wizard");
        }
        obj = wizard.getProperty(PROP_PXR_FILE);
        File file;
        if (obj instanceof File) {
            file = (File) obj;
        } else {
            throw new IOException("No file in wizard");
        }
        if (file.exists()) {
            throw new IOException("File already exists");
        }
        obj = wizard.getProperty(PROP_PXR_TYPE);
        ComponentType type;
        if (obj instanceof ComponentType) {
            type = (ComponentType) obj;
        } else {
            throw new IOException("No type in wizard");
        }
        boolean build = true;
        boolean autostart = true;

        FileObject fileObj = FileUtil.createData(file);
        writeFile(fileObj, id, type, autostart);

        Project project = Templates.getProject(wizard);
        if (project != null) {

//            FileObject autostarter = writeAutostartFile(project, id);

            ProjectProperties props = project.getLookup().lookup(ProjectProperties.class);
            if (props != null) {
                try {
                    props.addFile(ExecutionLevel.BUILD, fileObj);
                    if (autostart) {
                        props.addLine(ExecutionLevel.RUN, "/" + id + ".start");
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }

            }

            if (build) {
                buildFile(project, fileObj);
            }

        } else {
            LOG.warning("No project found for wizard");
        }

        return Collections.singleton(fileObj);
    }

    private void writeFile(FileObject file, String id, ComponentType type, boolean autostart) throws IOException {
        CharSequence code;
        if ("root:video".equals(type.toString())) {
            code = getVideoFileTemplate(id);
        } else if ("root:audio".equals(type.toString())) {
            code = getAudioFileTemplate(id);
        } else {
            code = getDefaultFileTemplate(id, type);
        }
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(file.getOutputStream());
            writer.append(code);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private CharSequence getDefaultFileTemplate(String id, ComponentType type) {
        StringBuilder code = new StringBuilder();
        code.append("@ /").append(id).append(" ").append(type).append(" {\n");
        code.append("}");
        return code;
    }

    private CharSequence getVideoFileTemplate(String id) {
        String template = "@ /video root:video {\n"
                + "  #%pxr.format 4\n"
                + "  #%autostart true\n"
                + "  .width 800\n"
                + "  .height 600\n"
                + "  .fps 60.0\n"
                + "  .renderer OpenGL\n"
                + "  @ ./screen video:output {\n"
                + "    #%graph.x 600\n"
                + "    #%graph.y 200\n"
                + "    .always-on-top true\n"
                + "    .show-cursor true\n"
                + "  }\n"
                + "}";
        return template.replace("/video", "/" + id);
    }
    
    private CharSequence getAudioFileTemplate(String id) {
        String template = "@ /audio root:audio {\n"
                + "  #%pxr.format 4\n"
                + "  #%autostart true\n"
                + "  @ ./output audio:output {\n"
                + "    #%graph.x 600\n"
                + "    #%graph.y 200\n"
                + "  }\n"
                + "}";
        return template.replace("/video", "/" + id);
    }

//    private FileObject writeAutostartFile(Project base, String id) throws IOException {
//        FileObject configDir = base.getProjectDirectory().getFileObject("config");
//        if (configDir == null) {
//            configDir = base.getProjectDirectory().createFolder("config");
//        }
//        String fileName = id + "_autostart";
//        FileObject autostarter = configDir.getFileObject(fileName);
//        if (autostarter == null) {
//            autostarter = configDir.createData(fileName);
//        }
//        String code = "/" + id + ".start";
//
//        Writer writer = null;
//        try {
//            writer = new OutputStreamWriter(autostarter.getOutputStream());
//            writer.append(code);
//        } finally {
//            if (writer != null) {
//                writer.close();
//            }
//        }
//        return autostarter;
//    }

    private void buildFile(Project base, FileObject file) {
        PraxisProject project = base.getLookup().lookup(PraxisProject.class);
        try {
            DataObject dob = DataObject.find(file);
            if (dob instanceof PXRDataObject) {

                new PXRFileHandler(project, (PXRDataObject) dob).process(
                        Callback.create(r -> {}));

            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        FileObject template = Templates.getTemplate(wizard);
        Object typeAttr = template.getAttribute("rootType");
        if (typeAttr != null) {
            try {
                rootType = ComponentType.of(typeAttr.toString());
            } catch (Exception ex) {
                // do nothing
            }
        }
    }

    @Override
    public void uninitialize(WizardDescriptor wizard) {
        panels = null;
    }

    @Override
    public WizardDescriptor.Panel current() {
        return getPanels()[index];
    }

    @Override
    public String name() {
        return index + 1 + ". from " + getPanels().length;
    }

    @Override
    public boolean hasNext() {
        return index < getPanels().length - 1;
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

    // If nothing unusual changes in the middle of the wizard, simply:
    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    // If something changes dynamically (besides moving between panels), e.g.
    // the number of panels changes in response to user input, then uncomment
    // the following and call when needed: fireChangeEvent();
    /*
    private Set<ChangeListener> listeners = new HashSet<ChangeListener>(1); // or can use ChangeSupport in NB 6.0
    public final void addChangeListener(ChangeListener l) {
    synchronized (listeners) {
    listeners.add(l);
    }
    }
    public final void removeChangeListener(ChangeListener l) {
    synchronized (listeners) {
    listeners.remove(l);
    }
    }
    protected final void fireChangeEvent() {
    Iterator<ChangeListener> it;
    synchronized (listeners) {
    it = new HashSet<ChangeListener>(listeners).iterator();
    }
    ChangeEvent ev = new ChangeEvent(this);
    while (it.hasNext()) {
    it.next().stateChanged(ev);
    }
    }
     */
    // You could safely ignore this method. Is is here to keep steps which were
    // there before this wizard was instantiated. It should be better handled
    // by NetBeans Wizard API itself rather than needed to be implemented by a
    // client code.
    private String[] createSteps() {
        String[] beforeSteps = null;
        Object prop = wizard.getProperty("WizardPanel_contentData");
        if (prop != null && prop instanceof String[]) {
            beforeSteps = (String[]) prop;
        }

        if (beforeSteps == null) {
            beforeSteps = new String[0];
        }

        String[] res = new String[(beforeSteps.length - 1) + panels.length];
        for (int i = 0; i < res.length; i++) {
            if (i < (beforeSteps.length - 1)) {
                res[i] = beforeSteps[i];
            } else {
                res[i] = panels[i - beforeSteps.length + 1].getComponent().getName();
            }
        }
        return res;
    }
}
