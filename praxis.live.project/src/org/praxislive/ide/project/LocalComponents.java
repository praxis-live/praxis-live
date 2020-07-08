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
package org.praxislive.ide.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openide.util.Lookup;
import org.praxislive.core.ComponentType;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.core.services.ComponentFactoryProvider;
import org.praxislive.ide.components.api.Components;

/**
 *
 */
class LocalComponents implements Components {

    private static final LocalComponents INSTANCE = new LocalComponents();
    
    private final Map<ComponentType, ComponentFactory.MetaData<?>> componentData;
    private final Map<ComponentType, ComponentFactory.MetaData<?>> rootData;
    private final List<ComponentType> components;
    private final List<ComponentType> roots;

    private LocalComponents() {
        componentData = new LinkedHashMap<>();
        rootData = new LinkedHashMap<>();
        var cmps = new ArrayList<ComponentType>();
        var rts = new ArrayList<ComponentType>();

        Lookup.getDefault().lookupAll(ComponentFactoryProvider.class)
                .stream()
                .map(provider -> provider.getFactory())
                .forEachOrdered(factory -> {
                    factory.componentTypes().forEachOrdered(type -> {
                        cmps.add(type);
                        componentData.put(type, factory.getMetaData(type));
                    }
                    );
                    factory.rootTypes().forEachOrdered(type -> {
                        rts.add(type);
                        rootData.put(type, factory.getRootMetaData(type));
                    }
                    );
                });
        
        components = List.copyOf(cmps);
        roots = List.copyOf(rts);
        
    }

    @Override
    public List<ComponentType> componentTypes() {
        return components;
    }

    @Override
    public List<ComponentType> rootTypes() {
        return roots;
    }

    @Override
    public ComponentFactory.MetaData<?> metaData(ComponentType type) {
        var data = componentData.get(type);
        if (data == null) {
            data = rootData.get(type);
        }
        return data;
    }
    
    static Components getInstance() {
        return INSTANCE;
    }

}
