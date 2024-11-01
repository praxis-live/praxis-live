
@TemplateRegistrations({
    @TemplateRegistration(folder = "Roots",
            position = 100,
            content = "resources/audio.pxr",
            displayName = "#TPL_Audio",
            category = "PXR"),
    @TemplateRegistration(folder = "Roots",
            position = 200,
            content = "resources/video.pxr",
            displayName = "#TPL_Video",
            category = "PXR"),
    @TemplateRegistration(folder = "Roots",
            position = 300,
            content = "resources/gui.pxr",
            displayName = "#TPL_GUI",
            category = "PXR"),
    @TemplateRegistration(folder = "Roots",
            position = 400,
            content = "resources/data.pxr",
            displayName = "#TPL_Generic",
            category = "PXR"),
    @TemplateRegistration(folder = "Roots",
            position = 500,
            content = "resources/root.pxr",
            displayName = "#TPL_Custom",
            category = "PXR")
})
@NbBundle.Messages({
    "TPL_Audio=Audio patch",
    "TPL_Video=Video patch",
    "TPL_GUI=Control panel (GUI)",
    "TPL_Generic=Generic data patch",
    "TPL_Custom=Custom root"
})
package org.praxislive.ide.pxr;

import org.netbeans.api.templates.TemplateRegistration;
import org.netbeans.api.templates.TemplateRegistrations;
import org.openide.util.NbBundle;
