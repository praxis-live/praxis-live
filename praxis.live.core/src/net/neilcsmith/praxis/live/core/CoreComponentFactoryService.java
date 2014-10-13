/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2014 Neil C Smith.
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
package net.neilcsmith.praxis.live.core;

import java.util.EnumSet;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.Root;
import net.neilcsmith.praxis.core.interfaces.ComponentFactoryService;
import net.neilcsmith.praxis.core.interfaces.RootFactoryService;
import net.neilcsmith.praxis.core.types.PReference;
import net.neilcsmith.praxis.impl.AbstractRoot;
import net.neilcsmith.praxis.impl.SimpleControl;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class CoreComponentFactoryService extends AbstractRoot {
    
    private final ComponentFactory factory;

    public CoreComponentFactoryService() {
        super(EnumSet.noneOf(Caps.class));
        factory = Utils.findCoreFactory();
        registerControl(ComponentFactoryService.NEW_INSTANCE, new NewInstanceControl());
        registerControl(RootFactoryService.NEW_ROOT_INSTANCE, new NewRootInstanceControl());
        registerInterface(ComponentFactoryService.class);
        registerInterface(RootFactoryService.class);
    }
    
    
    private class NewInstanceControl extends SimpleControl {

        private NewInstanceControl() {
            super(ComponentFactoryService.NEW_INSTANCE_INFO);
        }

        @Override
        protected CallArguments process(long time, CallArguments args, boolean quiet) throws Exception {
            Component c = factory.createComponent(ComponentType.coerce(args.get(0)));
            return CallArguments.create(PReference.wrap(c));
        }
    }

    private class NewRootInstanceControl extends SimpleControl {

        private NewRootInstanceControl() {
            super(RootFactoryService.NEW_ROOT_INSTANCE_INFO);
        }

        @Override
        protected CallArguments process(long time, CallArguments args, boolean quiet) throws Exception {
            Root r = factory.createRootComponent(ComponentType.coerce(args.get(0)));
            return CallArguments.create(PReference.wrap(r));
        }
    }
    
}
