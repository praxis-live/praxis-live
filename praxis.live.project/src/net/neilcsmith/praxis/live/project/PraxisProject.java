/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.project;

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PraxisProject implements Project {

    private final FileObject directory;
    private final Lookup lookup;

    PraxisProject(FileObject directory, ProjectState state) {
        this.directory = directory;
        this.lookup = Lookups.fixed(new Object[]{
                    this,
                    new Info(),
                    state
                });


    }

    @Override
    public FileObject getProjectDirectory() {
        return directory;


    }

    @Override
    public Lookup getLookup() {
        return lookup;
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
            return PraxisProject.this;
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
}
