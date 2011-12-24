/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.gui;

import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.util.AbstractHelperComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class GuiHelper extends AbstractHelperComponent {
    
    private static GuiHelper INSTANCE = new GuiHelper();

    private GuiHelper() {
    }
    
    
    public static GuiHelper getDefault() {
        return INSTANCE;
    }

    @ServiceProvider(service = ExtensionProvider.class)
    public static class Provider implements ExtensionProvider {

        @Override
        public Component getExtensionComponent() {
            return getDefault();
        }
    }
    
}
