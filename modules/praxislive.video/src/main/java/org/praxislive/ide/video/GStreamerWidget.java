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
package org.praxislive.ide.video;

import java.util.List;
import org.netbeans.spi.dashboard.DashboardDisplayer;
import org.netbeans.spi.dashboard.DashboardWidget;
import org.netbeans.spi.dashboard.WidgetElement;
import org.openide.util.NbBundle.Messages;

/**
 *
 */
@Messages({
    "TITLE_GStreamer=GStreamer Library",
    "TXT_GStreamer=The GStreamer library is required for video playback and capture."
})
public class GStreamerWidget implements DashboardWidget {

    @Override
    public String title(DashboardDisplayer.Panel pnl) {
        return Bundle.TITLE_GStreamer();
    }

    @Override
    public List<WidgetElement> elements(DashboardDisplayer.Panel pnl) {
        return List.of(
                WidgetElement.text(Bundle.TXT_GStreamer())
        );
    }

}
