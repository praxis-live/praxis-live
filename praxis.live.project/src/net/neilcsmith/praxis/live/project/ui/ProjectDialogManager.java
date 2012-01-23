/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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

import java.util.List;
import java.util.Map;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.project.api.ExecutionLevel;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class ProjectDialogManager {
    
    private final static ProjectDialogManager INSTANCE = new ProjectDialogManager();
    
    
    private ProjectDialogManager() {
        
    }
    
    public void showWarningsDialog(PraxisProject project,
            Map<FileObject, List<String>> warnings,
            ExecutionLevel level) {
        WarningsDialogPanel panel = new WarningsDialogPanel(project, warnings, level);
        NotifyDescriptor nd = new NotifyDescriptor.Message(panel, NotifyDescriptor.WARNING_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }
    
    public boolean continueOnError(PraxisProject project, FileObject file, CallArguments args, ExecutionLevel level) {
        StringBuilder sb = new StringBuilder();
        String path = FileUtil.getRelativePath(project.getProjectDirectory(), file);
        if (path == null) {
            path = file.getPath();
        }
        sb.append("Error executing ");
        sb.append(path);
        sb.append(".\n");
        sb.append("Continue ");
        if (level == ExecutionLevel.BUILD) {
            sb.append("building");
        } else {
            sb.append("running");
        }
        sb.append(" project?");
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(sb.toString(),
                "Execution Error",
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.ERROR_MESSAGE);
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }
    
    
    public static ProjectDialogManager getDefault() {
        return INSTANCE;
    }
    
}
