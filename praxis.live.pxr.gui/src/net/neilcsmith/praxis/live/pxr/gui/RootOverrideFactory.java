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
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentFactoryProvider;
import net.neilcsmith.praxis.core.ComponentInstantiationException;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.ComponentTypeNotFoundException;
import net.neilcsmith.praxis.core.Root;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class RootOverrideFactory implements ComponentFactory {

    private final static ComponentType root = ComponentType.create("root:gui");
    private final static RootOverrideFactory factory = new RootOverrideFactory();

    private RootOverrideFactory() {}

    @Override
    public ComponentType[] getComponentTypes() {
        return new ComponentType[0];
    }

    @Override
    public ComponentType[] getRootComponentTypes() {
        return new ComponentType[]{root};
    }

    @Override
    public Component createComponent(ComponentType type) throws ComponentTypeNotFoundException, ComponentInstantiationException {
        throw new ComponentTypeNotFoundException("This factory cannot create any components");
    }

    @Override
    public Root createRootComponent(ComponentType type) throws ComponentTypeNotFoundException, ComponentInstantiationException {
        if (root.equals(type)) {
            return new DockableGuiRoot();
        }
        throw new ComponentTypeNotFoundException("Root type " + type + " not found in this factory");
    }

    @Override
    public ComponentType getTypeForClass(Class<? extends Component> clazz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @ServiceProvider(service=ComponentFactoryProvider.class, position=0)
    public static class Provider implements ComponentFactoryProvider {

        @Override
        public ComponentFactory getFactory() {
            return factory;
        }

    }

}
