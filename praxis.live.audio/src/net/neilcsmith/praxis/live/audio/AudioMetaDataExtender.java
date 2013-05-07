/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.audio;

import net.neilcsmith.praxis.core.ComponentFactory.MetaData;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.api.MetaDataDecorator;
import net.neilcsmith.praxis.live.components.api.MetaDataRewriter;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith
 */
@ServiceProvider(service=MetaDataRewriter.class)
public class AudioMetaDataExtender implements MetaDataRewriter {
    
    private final static String AUDIO_ICON = 
            "net/neilcsmith/praxis/live/audio/resources/audio.png";

    @Override
    public <T> MetaData<T> rewrite(ComponentType type, MetaData<T> data) {
        if ("root:audio".equals(type.toString()) ||
                type.toString().startsWith("audio:")) {
            return new MetaDataDecorator<T>(data, new MetaDataDecorator.Icon(AUDIO_ICON));
        } else {
            return data;
        }
    }
    
}
