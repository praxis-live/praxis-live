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
package net.neilcsmith.praxis.live.pxr.wizard;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import net.neilcsmith.praxis.live.pxr.PXRDataObject;
import net.neilcsmith.praxis.live.pxr.PXRFileHandler;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public final class PXRWizardIterator implements WizardDescriptor.InstantiatingIterator {

    public final static String PROP_PXR_ID = "PXR.id";
    public final static String PROP_PXR_FILE = "PXR.file";
    public final static String PROP_PXR_TYPE = "PXR.type";
    public final static String PROP_PXR_BUILD = "PXR.build";
    public final static String PROP_PXR_AUTOSTART = "PXR.autostart";
    private int index;
    private WizardDescriptor wizard;
    private WizardDescriptor.Panel[] panels;
    private ComponentType rootType;

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel[] getPanels() {
        if (panels == null) {
            panels = new WizardDescriptor.Panel[] {
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
        boolean build = false;
        obj = wizard.getProperty(PROP_PXR_BUILD);
        if (obj instanceof Boolean) {
            build = ((Boolean) obj).booleanValue();
        }
        boolean autostart = false;
        obj = wizard.getProperty(PROP_PXR_AUTOSTART);
        if (obj instanceof Boolean) {
            autostart = ((Boolean) obj).booleanValue();
        }

        FileObject fileObj = FileUtil.createData(file);
        writeFile(fileObj, id, type, autostart);

        Project project = Templates.getProject(wizard);
        if (project != null) {


            FileObject autostarter = null;
            if (autostart) {
                autostarter = writeAutostartFile(project, id);
            }

            PraxisProjectProperties props = project.getLookup().lookup(PraxisProjectProperties.class);
            if (props != null) {
                List<FileObject> files =
                        new ArrayList<FileObject>(Arrays.asList(props.getProjectFiles(ExecutionLevel.BUILD)));
                files.add(fileObj);
                props.setProjectFiles(ExecutionLevel.BUILD, files.toArray(new FileObject[files.size()]));
                if (autostarter != null) {
                    files.clear();
                    files.addAll(Arrays.asList(props.getProjectFiles(ExecutionLevel.RUN)));
                    files.add(autostarter);
                    props.setProjectFiles(ExecutionLevel.RUN, files.toArray(new FileObject[files.size()]));
                }

            }
            
            if (build) {
                buildFile(project, fileObj);
            }

        }



        return Collections.singleton(fileObj);
    }

    private void writeFile(FileObject file, String id, ComponentType type, boolean autostart) throws IOException {
        StringBuilder code = new StringBuilder();
        code.append("@ /").append(id).append(" ").append(type).append(" {\n");
        code.append("  ").append("#%autostart ").append(autostart).append("\n");
        code.append("}");

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

    private FileObject writeAutostartFile(Project base, String id) throws IOException {
        FileObject configDir = base.getProjectDirectory().getFileObject("config");
        if (configDir == null) {
            configDir = base.getProjectDirectory().createFolder("config");
        }
        String fileName = id + "_autostart";
        FileObject autostarter = configDir.getFileObject(fileName);
        if (autostarter == null) {
            autostarter = configDir.createData(fileName);
        }
        String code = "/" + id + ".start";

        Writer writer = null;
        try {
            writer = new OutputStreamWriter(autostarter.getOutputStream());
            writer.append(code);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return autostarter;
    }

    private void buildFile(Project base, FileObject file) {
        PraxisProject project = base.getLookup().lookup(PraxisProject.class);
        try {
            DataObject dob = DataObject.find(file);
            if (dob instanceof PXRDataObject) {

                new PXRFileHandler(project, (PXRDataObject) dob).process(new Callback() {

                    @Override
                    public void onReturn(CallArguments args) {
                    }

                    @Override
                    public void onError(CallArguments args) {
                    }
                });

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
                rootType = ComponentType.create(typeAttr.toString());
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
