/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.audio;

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
public class AudioIconProvider implements ComponentIconProvider {
    
    private final static Image AUDIO_ICON = ImageUtilities.loadImage(
            "net/neilcsmith/praxis/live/audio/resources/audio.png", true);

    @Override
    public Image getIcon(ComponentType type) {
        if ("root:audio".equals(type.toString()) ||
                type.toString().startsWith("audio:")) {
            return AUDIO_ICON;
        }
        return null;
    }
    
}
