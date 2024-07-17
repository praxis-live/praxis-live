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
package org.praxislive.ide.core.ui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.spi.dashboard.DashboardDisplayer;
import org.netbeans.spi.dashboard.DashboardWidget;
import org.netbeans.spi.dashboard.WidgetElement;
import org.openide.awt.Actions;
import org.openide.util.NbBundle.Messages;

/**
 *
 */
@Messages({
    "TITLE_Projects=Projects"
})
public class ProjectWidget implements DashboardWidget {

    private final List<WidgetElement> elements;

    public ProjectWidget() {
        List<WidgetElement> el = new ArrayList<>(2);
        Action newProjectOriginal = Actions.forID("Project", "org.netbeans.modules.project.ui.NewProject");
        if (newProjectOriginal != null) {
            el.add(WidgetElement.actionLink(new ProjectDelegateAction(newProjectOriginal)));
        }
        Action openProjectOriginal = Actions.forID("Project", "org.netbeans.modules.project.ui.OpenProject");
        if (openProjectOriginal != null) {
            el.add(WidgetElement.actionLink(new ProjectDelegateAction(openProjectOriginal)));
        }
        elements = List.copyOf(el);
    }

    @Override
    public String title(DashboardDisplayer.Panel pnl) {
        return Bundle.TITLE_Projects();
    }

    @Override
    public List<WidgetElement> elements(DashboardDisplayer.Panel pnl) {
        return elements;
    }

    private static class ProjectDelegateAction extends AbstractAction {

        private final Action delegate;

        private ProjectDelegateAction(Action delegate) {
            super(Actions.cutAmpersand(String.valueOf(delegate.getValue(NAME)).replace("...", "")));
            this.delegate = delegate;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            delegate.actionPerformed(e);
        }

    }

}
