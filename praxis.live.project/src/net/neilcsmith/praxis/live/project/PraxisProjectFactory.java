/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.project;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service = ProjectFactory.class, position = 0)
public class PraxisProjectFactory implements ProjectFactory {

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
    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        return new PraxisProject(projectDirectory, state);
    }

    @Override
    public void saveProject(Project project) throws IOException, ClassCastException {
        // no op
    }

}
