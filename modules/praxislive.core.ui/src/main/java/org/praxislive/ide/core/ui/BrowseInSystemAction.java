/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
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
package org.praxislive.ide.core.ui;

import java.util.Collection;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

@ActionID(
        category = "Edit",
        id = "org.praxislive.ide.core.ui.BrowseInSystemAction")
@ActionRegistration(lazy = false,
        displayName = "#CTL_BrowseInSystemAction")
@ActionReference(path = "Loaders/folder/any/Actions", position = 260, separatorAfter = 275)
@NbBundle.Messages("CTL_BrowseInSystemAction=Browse in System")
public class BrowseInSystemAction extends AbstractAction
        implements ContextAwareAction {

    private final static RequestProcessor RP = new RequestProcessor(BrowseInSystemAction.class);

    @Override
    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ActionImpl(actionContext);
    }
    
    private static final class ActionImpl extends AbstractAction {

        private final File folder;
        
        private ActionImpl(Lookup lookup) {
            super(NbBundle.getMessage(BrowseInSystemAction.class, "CTL_BrowseInSystemAction"));
            putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
            Collection<? extends DataFolder> dataFolders = lookup.lookupAll(DataFolder.class);
            if (dataFolders.size() == 1) {
                folder = FileUtil.toFile(dataFolders.iterator().next().getPrimaryFile());
            } else {
                folder = null;
            }
        }

        @Override
        public boolean isEnabled() {
            return folder != null && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            RP.post(() -> {
                try {
                    Desktop.getDesktop().open(folder);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            });
        }
        
    }

}
