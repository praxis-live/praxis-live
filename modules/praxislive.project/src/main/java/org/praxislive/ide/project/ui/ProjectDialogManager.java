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

import java.util.List;
import java.util.Map;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileUtil;
import org.praxislive.core.Value;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ExecutionElement;

/**
 *
 */
public class ProjectDialogManager {

    private final static ProjectDialogManager INSTANCE = new ProjectDialogManager();

    private ProjectDialogManager() {

    }
    
    public void reportError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message,
                        NotifyDescriptor.ERROR_MESSAGE));
    }
    
    public void reportWarnings(Map<Task, List<String>> warnings) {
        WarningsDialogPanel panel = new WarningsDialogPanel(warnings);
        NotifyDescriptor nd = new NotifyDescriptor.Message(panel, NotifyDescriptor.WARNING_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }
    
    @Deprecated
    public void showWarningsDialog(PraxisProject project,
            Map<Task, List<String>> warnings) {
        reportWarnings(warnings);
    }

    public boolean confirm(String title, String message) {
        return confirm(title, message, NotifyDescriptor.PLAIN_MESSAGE);
    }
    
    public boolean confirmOnError(String title, String message) {
        return confirm(title, message, NotifyDescriptor.ERROR_MESSAGE);
    }
    
    private boolean confirm(String title, String message, int type) {
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(message, title,
                NotifyDescriptor.YES_NO_OPTION, type);
        return DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.YES_OPTION;
    }
    
    @Deprecated
    public boolean continueOnError(PraxisProject project, ExecutionLevel level, ExecutionElement element, List<Value> args) {
        StringBuilder sb = new StringBuilder();
        if (element instanceof ExecutionElement.File) {
            var file = ((ExecutionElement.File) element).file();
            var path = FileUtil.getRelativePath(project.getProjectDirectory(), file);
            if (path == null) {
                path = file.getPath();
            }
            sb.append("Error executing ");
            sb.append(path);
            sb.append(".\n");
        } else if (element instanceof ExecutionElement.Line) {
            var cmd = ((ExecutionElement.Line) element).line();
            sb.append("Error executing ");
            sb.append(cmd);
            sb.append(".\n");
        }

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
    
    public static ProjectDialogManager get(PraxisProject project) {
        return getDefault();
    }

}
