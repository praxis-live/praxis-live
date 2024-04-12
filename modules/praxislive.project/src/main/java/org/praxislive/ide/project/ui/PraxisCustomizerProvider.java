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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project.ui;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.praxislive.ide.project.DefaultPraxisProject;
import org.praxislive.ide.project.ProjectPropertiesImpl;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.util.NbBundle;

/**
 *
 */
@NbBundle.Messages({
    "LBL_buildLevelElements=Build Level Elements",
    "LBL_runLevelElements=Run Level Elements",
    "LBL_hubConfiguration=Hub Configuration",
    "LBL_libraries=Libraries",
    "LBL_compiler=Compiler"
})
public class PraxisCustomizerProvider implements CustomizerProvider,
        ProjectCustomizer.CategoryComponentProvider {

    private final Category build;
    private final Category run;
    private final Category hub;
    private final Category libraries;
    private final Category java;
    private final DefaultPraxisProject project;

    private ElementsCustomizer buildFiles;
    private ElementsCustomizer runFiles;
    private HubCustomizer hubCustomizer;
    private LibrariesCustomizer librariesCustomizer;
    private JavaCustomizer javaCustomizer;

    public PraxisCustomizerProvider(DefaultPraxisProject project) {
        this.project = project;
        build = Category.create(
                "build",
                Bundle.LBL_buildLevelElements(),
                null);
        run = Category.create(
                "run",
                Bundle.LBL_runLevelElements(),
                null);
        hub = Category.create(
                "hub",
                Bundle.LBL_hubConfiguration(),
                null);
        libraries = Category.create(
                "libraries",
                Bundle.LBL_libraries(),
                null);
        java = Category.create(
                "java",
                Bundle.LBL_compiler(),
                null);
    }

    @Override
    public void showCustomizer() {
        Category[] categories = new Category[]{build, run, hub, libraries, java};
        if (buildFiles != null) {
            buildFiles.refreshList();
        }
        if (runFiles != null) {
            runFiles.refreshList();
        }
        if (librariesCustomizer != null) {
            librariesCustomizer.refresh();
        }
        if (javaCustomizer != null) {
            javaCustomizer.refresh();
        }
        if (hubCustomizer != null) {
            hubCustomizer.refresh();
        }
        Dialog dialog = ProjectCustomizer.createCustomizerDialog(categories, this,
                null, new OKButtonListener(), null);
        dialog.setTitle(ProjectUtils.getInformation(project).getDisplayName());
        dialog.setVisible(true);
    }

    @Override
    public JComponent create(Category category) {
        if (build.equals(category)) {
            if (buildFiles == null) {
                buildFiles = new ElementsCustomizer(project, ExecutionLevel.BUILD);
            } else {
                buildFiles.refreshList();
            }
            return buildFiles;
        } else if (run.equals(category)) {
            if (runFiles == null) {
                runFiles = new ElementsCustomizer(project, ExecutionLevel.RUN);
            } else {
                runFiles.refreshList();
            }
            return runFiles;
        } else if (libraries.equals(category)) {
            if (librariesCustomizer == null) {
                librariesCustomizer = new LibrariesCustomizer(project);
            } else {
                librariesCustomizer.refresh();
            }
            return librariesCustomizer;
        } else if (java.equals(category)) {
            if (javaCustomizer == null) {
                javaCustomizer = new JavaCustomizer(project);
            } else {
                javaCustomizer.refresh();
            }
            return javaCustomizer;
        } else if (hub.equals(category)) {
            if (hubCustomizer == null) {
                hubCustomizer = new HubCustomizer(project);
            } else {
                hubCustomizer.refresh();
            }
            return hubCustomizer;
        } else {
            return new JPanel();
        }
    }

    private class OKButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            ProjectPropertiesImpl props = project.getLookup().lookup(ProjectPropertiesImpl.class);
            if (props == null) {
                return;
            }
            if (buildFiles != null) {
                props.setElements(ExecutionLevel.BUILD, buildFiles.getElements());
            }
            if (runFiles != null) {
                props.setElements(ExecutionLevel.RUN, runFiles.getElements());
            }
            if (librariesCustomizer != null) {
                librariesCustomizer.updateProject();
            }
            if (javaCustomizer != null) {
                javaCustomizer.updateProject();
            }
            if (hubCustomizer != null) {
                hubCustomizer.updateProject();
            }
//            ProjectState state = project.getLookup().lookup(ProjectState.class);
//            if (state != null) {
//                state.markModified();
//            }
        }

    }

}
