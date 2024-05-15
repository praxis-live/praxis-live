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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.audio;

import java.awt.Image;
import java.util.Optional;
import org.openide.util.ImageUtilities;
import org.praxislive.core.ComponentType;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.ide.components.api.ComponentIconProvider;

/**
 *
 */
@ServiceProvider(service = ComponentIconProvider.class)
public class AudioIconProvider implements ComponentIconProvider {

    private final static Image AUDIO_ICON = ImageUtilities.loadImage(
            "org/praxislive/ide/audio/resources/audio.png");

    @Override
    public Optional<Image> getIcon(ComponentType type) {
        if ("root:audio".equals(type.toString())
                || type.toString().startsWith("audio:")) {
            return Optional.of(AUDIO_ICON);
        }
        return Optional.empty();
    }

}
