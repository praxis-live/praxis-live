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

package org.praxislive.ide.project.ui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectSensitiveActions;
import org.openide.awt.Actions;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.ide.project.wizard.EmbedRuntime;

/**
 *
 */
@NbBundle.Messages({
    "ACT_EmbedRuntime=Embed CORE Runtime"
})
class PraxisProjectNode extends FilterNode {

    private static final Action SYSTEM_OPEN_ACTION = Actions.forID("Edit",
            "org.netbeans.core.ui.sysopen.SystemOpenAction");
    private static final String ICON_PATH = "org/praxislive/ide/project/resources/pxp16.png";
    
    public PraxisProjectNode(PraxisProject project, Node original) {
        super(original, new PraxisFolderChildren(project, original),
                new ProxyLookup(original.getLookup(), Lookups.singleton(project)));
    }

    @Override
    public Image getIcon(int type) {
        Image image = ImageUtilities.loadImage(ICON_PATH, true);
        if (image == null) {
            return super.getIcon(type);
        } else {
            return image;
        }
    }

    @Override
    public Image getOpenedIcon(int type) {
        Image image = ImageUtilities.loadImage(ICON_PATH, true);
        if (image == null) {
            return super.getOpenedIcon(type);
        } else {
            return image;
        }
    }



    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            CommonProjectActions.newFileAction(),
            null,
            ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_RUN, "Run", null),
            ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_BUILD, "Build", null),
            ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_CLEAN, "Clean", null),
            null,
            new EmbedRuntimeAction(),
            CommonProjectActions.setAsMainProjectAction(),
            SYSTEM_OPEN_ACTION,
            CommonProjectActions.closeProjectAction(),
            null,
            CommonProjectActions.customizeProjectAction()

        };
    }

    private class EmbedRuntimeAction extends AbstractAction {

        private EmbedRuntimeAction() {
            super(Bundle.ACT_EmbedRuntime());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            var project = getLookup().lookup(PraxisProject.class);
            if (project != null) {
                EmbedRuntime.getInstance().process(project);
            }
        }
        
    }
    

    

}
