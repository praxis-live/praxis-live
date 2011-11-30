/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.video;

import java.awt.Image;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.api.ComponentIconProvider;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
@ServiceProvider(service=ComponentIconProvider.class)
public class VideoIconProvider implements ComponentIconProvider {
    
    private final static Image VIDEO_ICON = ImageUtilities.loadImage(
            "net/neilcsmith/praxis/live/video/resources/video.png", true);

    @Override
    public Image getIcon(ComponentType type) {
        if ("root:video".equals(type.toString()) ||
                type.toString().startsWith("video:")) {
            return VIDEO_ICON;
        }
        return null;
    }
    
}
