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

package net.neilcsmith.praxis.live.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentFactoryProvider;
import net.neilcsmith.praxis.core.ComponentType;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ComponentRegistry {

    private final static Logger LOG = Logger.getLogger(ComponentRegistry.class.getName());

    private final static ComponentRegistry INSTANCE = new ComponentRegistry();

    private Set<ComponentType> components;
    private Set<ComponentType> roots;
//    private SortedSet<ComponentType> components;
//    private SortedSet<ComponentType> roots;

    private ComponentRegistry() {
        initSets();
    }

    private void initSets() {
//        components = new TreeSet<ComponentType>(new ComponentTypeComparator());
//        roots = new TreeSet<ComponentType>(new ComponentTypeComparator());
        components = new HashSet<ComponentType>();
        roots = new HashSet<ComponentType>();
        Collection<? extends ComponentFactoryProvider> providers =
                Lookup.getDefault().lookupAll(ComponentFactoryProvider.class);
        for (ComponentFactoryProvider provider : providers) {
            ComponentFactory factory = provider.getFactory();
            components.addAll(Arrays.asList(factory.getComponentTypes()));
            roots.addAll(Arrays.asList(factory.getRootComponentTypes()));
        }
//        ComponentTypeComparator cmp = new ComponentTypeComparator();

    }


    public Set<ComponentType> getAllComponents() {
        return Collections.unmodifiableSet(components);
//        return Collections.unmodifiableSortedSet(components);
    }

    public Set<ComponentType> getAllRoots() {
        return Collections.unmodifiableSet(roots);
//        return Collections.unmodifiableSortedSet(roots);
    }

    public static ComponentRegistry getDefault() {
        return INSTANCE;
    }

}
