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
package net.neilcsmith.praxis.live.project.ui;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.neilcsmith.praxis.live.project.DefaultPraxisProject;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProjectProperties;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PraxisCustomizerProvider implements CustomizerProvider,
        ProjectCustomizer.CategoryComponentProvider {

    private Category build;
    private Category run;
    private DefaultPraxisProject project;
    
    private FilesCustomizer buildFiles;
    private FilesCustomizer runFiles;

    public PraxisCustomizerProvider(DefaultPraxisProject project) {
        this.project = project;
        build = Category.create(
                "build",
                "Build Level Files",
                null);
        run = Category.create(
                "run",
                "Run Level Files",
                null);
    }


    @Override
    public void showCustomizer() {
        Category[] categories = new Category[] {build, run};
        if (buildFiles != null) {
            buildFiles.refreshList();
        }
        if (runFiles != null) {
            runFiles.refreshList();
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
                buildFiles = new FilesCustomizer(project, ExecutionLevel.BUILD);
            } else {
                buildFiles.refreshList();
            }
            return buildFiles;
        } else if (run.equals(category)) {
            if (runFiles == null) {
                runFiles = new FilesCustomizer(project, ExecutionLevel.RUN);
            } else {
                runFiles.refreshList();
            }
            return runFiles;
        } else {
            return new JPanel();
        }
    }

    private class OKButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            PraxisProjectProperties props = project.getLookup().lookup(PraxisProjectProperties.class);
            if (props == null) {
               return;
            }
            if (buildFiles != null) {
                props.setProjectFiles(ExecutionLevel.BUILD, buildFiles.getFiles());
            }
            if (runFiles != null) {
                props.setProjectFiles(ExecutionLevel.RUN, runFiles.getFiles());
            }
//            ProjectState state = project.getLookup().lookup(ProjectState.class);
//            if (state != null) {
//                state.markModified();
//            }
        }

    }


    

}
