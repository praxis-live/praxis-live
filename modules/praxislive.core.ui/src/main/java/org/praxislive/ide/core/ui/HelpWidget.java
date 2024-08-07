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
import java.net.URI;
import java.util.List;
import javax.swing.AbstractAction;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.spi.dashboard.DashboardDisplayer;
import org.netbeans.spi.dashboard.DashboardWidget;
import org.netbeans.spi.dashboard.WidgetElement;
import org.openide.util.NbBundle.Messages;

/**
 *
 */
@Messages({
    "TITLE_Help=Learn & Discover",
    "LBL_ShowShortcuts=Keyboard Shortcuts",
    "STATUS_ShowShortcuts=Show and edit keyboard shortcuts.",
    "LBL_OnlineLinks=Useful Links",
    "LBL_WebsiteLink=PraxisLIVE website",
    "LBL_DocsLink=Online documentation",
    "LBL_CommunityLink=Community Help"
})
public class HelpWidget implements DashboardWidget {

    @Override
    public String title(DashboardDisplayer.Panel pnl) {
        return Bundle.TITLE_Help();
    }

    @Override
    public List<WidgetElement> elements(DashboardDisplayer.Panel pnl) {
        return List.of(
                WidgetElement.actionLink(new ShowShortcutsAction()),
                WidgetElement.separator(),
                WidgetElement.subheading(Bundle.LBL_OnlineLinks()),
                WidgetElement.link(Bundle.LBL_WebsiteLink(), URI.create(Bundle.LINK_Website())),
                WidgetElement.link(Bundle.LBL_DocsLink(), URI.create(Bundle.LINK_Documentation())),
                WidgetElement.link(Bundle.LBL_CommunityLink(), URI.create(Bundle.LINK_Support()))
        );
    }

    private static class ShowShortcutsAction extends AbstractAction {

        private ShowShortcutsAction() {
            super(Bundle.LBL_ShowShortcuts());
            putValue(SHORT_DESCRIPTION, Bundle.STATUS_ShowShortcuts());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OptionsDisplayer.getDefault().open(OptionsDisplayer.KEYMAPS);
        }

    }
}
