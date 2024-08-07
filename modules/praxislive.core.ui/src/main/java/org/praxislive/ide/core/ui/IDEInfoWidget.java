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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import org.netbeans.spi.dashboard.DashboardDisplayer;
import org.netbeans.spi.dashboard.DashboardWidget;
import org.netbeans.spi.dashboard.WidgetElement;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.praxislive.ide.core.api.IDE;

/**
 *
 */
@Messages({
    "LBL_ShowOnStartup=Show Dash on Startup",
    "STATUS_ShowOnStartup=Show dash on startup of the IDE.",
    "# {0} - current version",
    "TXT_CurrentVersion=Version : {0}",
    "# {0} - latest version",
    "TXT_LatestVersion=Latest : {0}",
    "LBL_Download=Download update"
})
public class IDEInfoWidget implements DashboardWidget {

    private final List<WidgetElement> elements;
    private final Action showOnStartupAction;

    public IDEInfoWidget() {
        FileObject configFolder = FileUtil.getConfigFile("Dashboard/Main");
        showOnStartupAction = new ShowOnStartupAction(configFolder);
        String currentVersion = IDE.getVersion();
        String latestVersion = IDE.getLatestAvailableVersion();
        boolean hasUpdate = !Objects.equals(currentVersion, latestVersion);
        List<WidgetElement> elems = new ArrayList<>();
        elems.add(WidgetElement.image("org/praxislive/ide/core/ui/resources/logo128.png"));
        elems.add(WidgetElement.text(
                Bundle.TXT_CurrentVersion(currentVersion) + "\n"
                + Bundle.TXT_LatestVersion(latestVersion)
        ));
        if (hasUpdate) {
            elems.add(WidgetElement.linkButton(Bundle.LBL_Download(), URI.create(Bundle.LINK_Download())));
        }
        elems.add(WidgetElement.component(() -> new JCheckBox(showOnStartupAction)));

        elements = List.copyOf(elems);
    }

    @Override
    public String title(DashboardDisplayer.Panel pnl) {
        return "";
    }

    @Override
    public List<WidgetElement> elements(DashboardDisplayer.Panel pnl) {
        return elements;
    }

    private static class ShowOnStartupAction extends AbstractAction {

        private final FileObject configFile;

        private ShowOnStartupAction(FileObject configFile) {
            super(Bundle.LBL_ShowOnStartup());
            this.configFile = configFile;
            putValue(SHORT_DESCRIPTION, Bundle.STATUS_ShowOnStartup());
            if (configFile != null) {
                putValue(SELECTED_KEY, Boolean.TRUE.equals(configFile.getAttribute("showOnStartup")));
            } else {
                putValue(SELECTED_KEY, false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (configFile != null) {
                try {
                    configFile.setAttribute("showOnStartup",
                            Boolean.TRUE.equals(getValue(SELECTED_KEY)));

                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

        }
    }

}
