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
package net.neilcsmith.praxis.live.project;

import java.beans.PropertyChangeEvent;
import net.neilcsmith.praxis.live.core.api.HubStateException;
import net.neilcsmith.praxis.live.project.ui.PraxisCustomizerProvider;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import javax.swing.Icon;
import net.neilcsmith.praxis.live.core.api.HubManager;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import net.neilcsmith.praxis.live.project.ui.PraxisLogicalViewProvider;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultPraxisProject extends PraxisProject {

    private final FileObject directory;
    private FileObject projectFile;
    private PraxisProjectProperties properties;
    private final Lookup lookup;
    private boolean built;
    private boolean run;
    private HelperListener helperListener;

    DefaultPraxisProject(FileObject directory, FileObject projectFile, ProjectState state)
            throws IOException {
        this.directory = directory;
        this.projectFile = projectFile;
        properties = parseProjectFile(projectFile);

        Lookup base = Lookups.fixed(new Object[]{
                    this,
                    properties,
                    new Info(),
                    new ActionImpl(),
                    state,
                    new PraxisCustomizerProvider(this),
                    new PraxisLogicalViewProvider(this),
                    new BaseTemplates(),
                    UILookupMergerSupport.createPrivilegedTemplatesMerger()
                });
//        this.lookup = base;
        this.lookup = LookupProviderSupport.createCompositeLookup(base, LOOKUP_PATH);
        helperListener = new HelperListener();
        ProjectHelper.getDefault().addPropertyChangeListener(
                WeakListeners.propertyChange(helperListener, ProjectHelper.getDefault()));
    }

    @Override
    public FileObject getProjectDirectory() {
        return directory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private ProjectPropertiesImpl parseProjectFile(FileObject projectFile) {
        ProjectPropertiesImpl props = new ProjectPropertiesImpl(this);
        try {
            PXPReader.initializeProjectProperties(directory, projectFile, props);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return props;
    }

    public void save() throws IOException {
        PXPWriter.writeProjectProperties(directory, projectFile, properties);
    }

    private class Info implements ProjectInformation {

        @Override
        public String getName() {
            return directory.getName();
        }

        @Override
        public String getDisplayName() {
            return directory.getName();
        }

        @Override
        public Icon getIcon() {
            return ImageUtilities.loadImageIcon("net/neilcsmith/praxis/live/project/resources/pxp16.png", false);
        }

        @Override
        public Project getProject() {
            return DefaultPraxisProject.this;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            // no op
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            // no op
        }
    }

    private class BaseTemplates implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                        "Templates/Other/Folder"
                    };
        }
    }

    private class HelperListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProjectHelper.PROP_HUB_CONNECTED.equals(evt.getPropertyName())) {
                if (!ProjectHelper.getDefault().isConnected()) {
                    built = run = false;
                }
            }
        }

    }

    private class ActionImpl implements ActionProvider {

        @Override
        public String[] getSupportedActions() {
            return new String[]{
                        ActionProvider.COMMAND_RUN,
                        ActionProvider.COMMAND_BUILD,
                        ActionProvider.COMMAND_CLEAN
                    };
        }

        @Override
        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
            
            if (ActionProvider.COMMAND_RUN.equals(command)) {
                invokeRun();
            } else if (ActionProvider.COMMAND_BUILD.equals(command)) {
                invokeBuild();
            } else if (ActionProvider.COMMAND_CLEAN.equals(command)) {
                invokeClean();
            }
        }

        private void invokeClean() {
            try {
                HubManager.getDefault().restart();
            } catch (HubStateException ex) {
                // @TODO what here?
            }
            built = run = false;
        }
        
        private void invokeBuild() {
            PraxisProject project = DefaultPraxisProject.this;
            if (built) {
                throw new IllegalArgumentException("Project has already been built");
            }
            FileHandlerIterator itr = FileHandlerIterator.createBuildIterator(project);       
            built = true;
            itr.start();
        }

        private void invokeRun() {
            PraxisProject project = DefaultPraxisProject.this;
            if (run) {
                throw new IllegalArgumentException("Project has already been run");
            }
            FileHandlerIterator itr;
            if (!built) {
                itr = FileHandlerIterator.createBuildAndRunIterator(project);
            } else {
                itr = FileHandlerIterator.createRunIterator(project);
            }
            built = run = true;
            itr.start();
        }

        @Override
        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
            if (!ProjectHelper.getDefault().isConnected()) {
                return false;
            }
            if (ActionProvider.COMMAND_CLEAN.equals(command)) {
                return true;
            }
            if (!built && ActionProvider.COMMAND_BUILD.equals(command)) {
                return true;
            }
            if (!run && ActionProvider.COMMAND_RUN.equals(command)) {
                return true;
            }
            return false;
        }
    }
}
