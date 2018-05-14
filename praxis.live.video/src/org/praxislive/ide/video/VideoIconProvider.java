/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.praxislive.ide.video;

import java.awt.Image;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.ComponentIconProvider;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
@ServiceProvider(service=ComponentIconProvider.class)
public class VideoIconProvider implements ComponentIconProvider {
    
    private final static Image VIDEO_ICON = ImageUtilities.loadImage(
            "org/praxislive/ide/video/resources/video.png", true);

    @Override
    public Image getIcon(ComponentType type) {
        if ("root:video".equals(type.toString()) ||
                type.toString().startsWith("video:")) {
            return VIDEO_ICON;
        }
        return null;
    }
    
}
