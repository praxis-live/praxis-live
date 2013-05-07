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
package net.neilcsmith.praxis.live.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentFactoryProvider;
import net.neilcsmith.praxis.core.ComponentInstantiationException;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.Root;
import net.neilcsmith.praxis.live.components.api.MetaDataRewriter;
import net.neilcsmith.praxis.live.core.CoreFactoryProvider;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ComponentRegistry implements ComponentFactory {

    private final static Logger LOG =
            Logger.getLogger(ComponentRegistry.class.getName());
    private final static ComponentRegistry INSTANCE = new ComponentRegistry();
    private Map<ComponentType, ExtMetaData<? extends Component>> componentCache;
    private Map<ComponentType, ExtMetaData<? extends Root>> rootCache;

    private ComponentRegistry() {
        initData();
    }

    private void initData() {
        componentCache = new LinkedHashMap<ComponentType, ExtMetaData<? extends Component>>();
        rootCache = new LinkedHashMap<ComponentType, ExtMetaData<? extends Root>>();

        Collection<? extends MetaDataRewriter> rewriters =
                Lookup.getDefault().lookupAll(MetaDataRewriter.class);

        for (ComponentFactoryProvider provider : findFactoryProviders()) {
            ComponentFactory factory = provider.getFactory();
            LOG.log(Level.INFO, "Adding components from : {0}", factory.getClass());
            for (ComponentType type : factory.getComponentTypes()) {
                MetaData<? extends Component> data = factory.getMetaData(type);
                if (data == null) {
                    LOG.log(Level.WARNING, "MetaData null for {0}", type);
                    continue;
                }
                for (MetaDataRewriter rewriter : rewriters) {
                    MetaData<? extends Component> d = rewriter.rewrite(type, data);
                    data = d == null ? data : d;
                }
                componentCache.put(type, getExtMetaData(data, factory));
            }
            for (ComponentType type : factory.getRootComponentTypes()) {
                MetaData<? extends Root> data = factory.getRootMetaData(type);
                if (data == null) {
                    LOG.log(Level.WARNING, "MetaData null for {0}", type);
                    continue;
                }
                for (MetaDataRewriter rewriter : rewriters) {
                    MetaData<? extends Root> d = rewriter.rewrite(type, data);
                    data = d == null ? data : d;
                }
                rootCache.put(type, getExtMetaData(data, factory));
            }
        }
    }

    private <T> ExtMetaData<T> getExtMetaData(MetaData<T> data, ComponentFactory factory) {
        return new ExtMetaData<T>(data, factory);
    }

    private List<? extends ComponentFactoryProvider> findFactoryProviders() {
        Collection<? extends ComponentFactoryProvider> lkp =
                Lookup.getDefault().lookupAll(ComponentFactoryProvider.class);
        List<ComponentFactoryProvider> providers =
                new ArrayList<ComponentFactoryProvider>(lkp);
        Collections.reverse(providers);
        return providers;
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
    public MetaData<? extends Component> getMetaData(ComponentType type) {
        return componentCache.get(type);
//        if (factory != null) {
//            return factory.getMetaData(type);
//        } else {
//            throw new IllegalArgumentException();
//        }
    }

    @Override
    public MetaData<? extends Root> getRootMetaData(ComponentType type) {
        return rootCache.get(type);
    }

    @Override
    public Component createComponent(ComponentType type) throws ComponentInstantiationException {
        ExtMetaData<? extends Component> data = componentCache.get(type);
        if (data != null) {
            return data.getFactory().createComponent(type);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Root createRootComponent(ComponentType type) throws ComponentInstantiationException {
        ExtMetaData<? extends Root> data = rootCache.get(type);
        if (data != null) {
            return data.getFactory().createRootComponent(type);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static ComponentRegistry getDefault() {
        return INSTANCE;
    }

    private static class ExtMetaData<T> extends MetaData<T> {

        private final MetaData<T> delegate;
        private final ComponentFactory factory;

        private ExtMetaData(MetaData<T> delegate, ComponentFactory factory) {
            this.delegate = delegate;
            this.factory = factory;
        }

        @Override
        public boolean isTest() {
            return delegate.isTest();
        }

        @Override
        public boolean isDeprecated() {
            return delegate.isDeprecated();
        }

        @Override
        public ComponentType getReplacement() {
            return delegate.getReplacement();
        }

        @Override
        public net.neilcsmith.praxis.core.Lookup getLookup() {
            return delegate.getLookup();
        }

        @Override
        public Class<T> getComponentClass() {
            return delegate.getComponentClass();
        }

        private ComponentFactory getFactory() {
            return factory;
        }
    }

    @ServiceProvider(service = CoreFactoryProvider.class)
    public static class Provider implements CoreFactoryProvider {

        @Override
        public ComponentFactory getFactory() {
            return getDefault();
        }
    }
}
