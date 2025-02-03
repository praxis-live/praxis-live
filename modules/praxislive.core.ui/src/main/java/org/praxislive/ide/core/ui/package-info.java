
@ContainerRegistration(id = "Core",
        categoryName = "#OptionsCategory_Name_Core",
        iconBase = "org/praxislive/ide/core/ui/resources/settings.png",
        keywords = "#OptionsCategory_Keywords_Core",
        keywordsCategory = "Settings",
        position = -100)
@Messages({
    "OptionsCategory_Name_Core=Settings",
    "OptionsCategory_Keywords_Core=settings",
    "TITLE_Application=PraxisLIVE",
    "LINK_Website=https://www.praxislive.org",
    "LINK_Download=https://www.praxislive.org/download/",
    "LINK_Documentation=https://www.praxislive.org/documentation/",
    "LINK_Issues=https://www.praxislive.org/issues/",
    "LINK_Support=https://www.praxislive.org/community/"
})
package org.praxislive.ide.core.ui;

import org.netbeans.spi.options.OptionsPanelController.ContainerRegistration;
import org.openide.util.NbBundle.Messages;
