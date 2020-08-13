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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.project.ui;

import java.util.concurrent.Callable;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.OnStop;
import org.openide.util.NbBundle;
import org.praxislive.ide.project.DefaultPraxisProject;

/**
 *
 */
@NbBundle.Messages({
    "TITLE_ClosingActiveProjects=Projects active",
    "MESSAGE_ClosingActiveProjects=There are active projects. Close IDE and lose changes?"
})
@OnStop
public class IDEClosingHandler implements Callable<Boolean> {

    @Override
    public Boolean call() throws Exception {
        var active = DefaultPraxisProject.activeProjects();
        if (!active.isEmpty()) {
            var notification = new NotifyDescriptor.Confirmation(
                    Bundle.MESSAGE_ClosingActiveProjects(),
                    Bundle.TITLE_ClosingActiveProjects(),
                    NotifyDescriptor.OK_CANCEL_OPTION);
            return DialogDisplayer.getDefault().notify(notification)
                    == NotifyDescriptor.OK_OPTION;
        } else {
            return true;
        }
    }

}
