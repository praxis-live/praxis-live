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

package net.neilcsmith.praxis.live.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentFactoryProvider;
import net.neilcsmith.praxis.core.ComponentInstantiationException;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.ComponentTypeNotFoundException;
import net.neilcsmith.praxis.core.Root;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class NbLookupComponentFactory implements ComponentFactory {
    
    private final static Logger logger = 
            Logger.getLogger(NbLookupComponentFactory.class.getName());

    private Map<ComponentType, ComponentFactory> componentCache;
    private Map<ComponentType, ComponentFactory> rootCache;

    private NbLookupComponentFactory(Map<ComponentType, ComponentFactory> componentCache,
            Map<ComponentType, ComponentFactory> rootCache) {
        this.componentCache = componentCache;
        this.rootCache = rootCache;
    }

    @Override
    public ComponentType[] getComponentTypes() {
        Set<ComponentType> keys = componentCache.keySet();
        return keys.toArray(new ComponentType[keys.size()]);
    }

    @Override
    public ComponentType[] getRootComponentTypes() {
        Set<ComponentType> keys = rootCache.keySet();
        return keys.toArray(new ComponentType[keys.size()]);
    }

    @Override
    public Component createComponent(ComponentType type) throws ComponentTypeNotFoundException, ComponentInstantiationException {
        ComponentFactory factory = componentCache.get(type);
        if (factory != null) {
            return factory.createComponent(type);
        } else {
            throw new ComponentTypeNotFoundException();
        }
    }

    @Override
    public Root createRootComponent(ComponentType type) throws ComponentTypeNotFoundException, ComponentInstantiationException {
        ComponentFactory factory = rootCache.get(type);
        if (factory != null) {
            return factory.createRootComponent(type);
        } else {
            throw new ComponentTypeNotFoundException();
        }
    }

    @Override
    public ComponentType getTypeForClass(Class<? extends Component> clazz) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    public static NbLookupComponentFactory getInstance() {
        Map<ComponentType, ComponentFactory> componentCache =
                new LinkedHashMap<ComponentType, ComponentFactory>();
        Map<ComponentType, ComponentFactory> rootCache =
                new LinkedHashMap<ComponentType, ComponentFactory>();

        Collection<? extends ComponentFactoryProvider> lkp =
                Lookup.getDefault().lookupAll(ComponentFactoryProvider.class);
        List<ComponentFactoryProvider> providers =
                new ArrayList<ComponentFactoryProvider>(lkp);
        Collections.reverse(providers);
        for (ComponentFactoryProvider provider : providers) {
            ComponentFactory factory = provider.getFactory();
            logger.info("Adding components from : " + factory.getClass());
            for (ComponentType type : factory.getComponentTypes()) {
                componentCache.put(type, factory);
            }
            for (ComponentType type : factory.getRootComponentTypes()) {
                rootCache.put(type, factory);
            }
        }
        return new NbLookupComponentFactory(componentCache, rootCache);
    }


}
