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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager.Result;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service = ProjectFactory.class, position = 0)
public class PraxisProjectFactory implements ProjectFactory2 {
    
    private final static Icon icon = 
            ImageUtilities.loadImageIcon(
            "net/neilcsmith/praxis/live/project/resources/pxp16.png",
            false);

    @Override
    public boolean isProject(FileObject projectDirectory) {
        FileObject[] files = projectDirectory.getChildren();
        for (FileObject file : files) {
            if (file.hasExt("pxp")) {
                return true;
            }
        }
        return false;
    }
    
    
    @Override
    public Result isProject2(FileObject projectDirectory) {
        if (isProject(projectDirectory)) {
            return new Result(icon);
        } else {
            return null;
        }
    }

    @Override
    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        // find project configuration file
        List<FileObject> possibles = new ArrayList<FileObject>(1);
        FileObject[] files = projectDirectory.getChildren();
        for (FileObject file : files) {
            if (file.hasExt("pxp")) {
                possibles.add(file);
            }
        }
        if (possibles.isEmpty()) {
            return null;
        }
        FileObject projectFile;
        if (possibles.size() == 1) {
            projectFile = possibles.get(0);
        } else {
            return null; //show dialog? better searching?
        }

        return new DefaultPraxisProject(projectDirectory, projectFile, state);
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {
        DefaultPraxisProject p = project.getLookup().lookup(DefaultPraxisProject.class);
        if (p != null) {
            p.save();
        }
    }


}
