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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.components.api;

import java.awt.Image;
import java.util.List;
import org.praxislive.core.ComponentType;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.ide.components.ComponentSettings;

/**
 *
 */
public interface Components {

   
    public List<ComponentType> componentTypes();
    
    public List<ComponentType> rootTypes();
    
    public ComponentFactory.MetaData metaData(ComponentType type);


    public default Image getIcon(ComponentType type) {
        return Icons.getIcon(type);
    }

    public default boolean showDeprecated() {
        return ComponentSettings.getShowDeprecated();
    }

    public default boolean rewriteDeprecated() {
        return ComponentSettings.getRewriteDeprecated();
    }
}
