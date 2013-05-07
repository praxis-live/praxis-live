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

package net.neilcsmith.praxis.live.components.api;

import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentType;

/**
 * Service interface that allows the extension of ComponentFactory.MetaData attached
 * to a component type. Implementation should be registered as service providers.
 * @author Neil C Smith
 */
public interface MetaDataRewriter {
    
    /**
     * Creates a ComponentFactory.MetaData instance that extends the passed in 
     * data. The implementation should return the passed in data unchanged if it
     * does not want to extend the data for the passed in type.
     * 
     * @param type ComponentType the MetaData is for
     * @param data Existing MetaData
     * @return Extended MetaData or the passed in data
     */
    public <T> ComponentFactory.MetaData<T> rewrite(ComponentType type, ComponentFactory.MetaData<T> data);
    
}
