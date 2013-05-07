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
import java.util.List;
import java.util.TreeMap;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory.MetaData;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.Root;
import org.openide.nodes.Children;
import org.openide.nodes.Node;


/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class CategoryChildren extends Children.Keys<String> {
    
    private String[] filters;
    private List<String> forceTestFilters;
    private TreeMap<String, TreeMap<ComponentType, MetaData<? extends Component>>> core;
    private TreeMap<String, TreeMap<ComponentType, MetaData<? extends Component>>>  others;
    private boolean includeTest = ComponentSettings.getShowTestComponents();
    

    public CategoryChildren() {
        this(null);
    }

    public CategoryChildren(String[] filters) {
        this.filters = filters;
        buildForceTest(filters);
        buildMap();
        List<String> keys = new ArrayList<String>();
        keys.addAll(core.keySet());
        keys.addAll(others.keySet());
        setKeys(keys);
    }
    
    private void buildForceTest(String[] filters) {
        forceTestFilters = new ArrayList<String>(1);
        for (String filter : filters) {
            try {
                ComponentType type = ComponentType.create("root:" + filter);
                MetaData<? extends Root> data = ComponentRegistry.getDefault().getRootMetaData(type);
                if (data != null && data.isTest()) {
                    forceTestFilters.add(filter);
                }
            } catch (Exception ex) {
                continue;
            }
            
            
        }
    }
    
    private void buildMap() {
        core = new TreeMap<String, TreeMap<ComponentType, MetaData<? extends Component>>>();
        others = new TreeMap<String, TreeMap<ComponentType, MetaData<? extends Component>>>();
        ComponentRegistry reg = ComponentRegistry.getDefault();
        ComponentType[] types = reg.getComponentTypes();
        for (ComponentType type : types) {
            String str = type.toString();
            MetaData<? extends Component> data = reg.getMetaData(type);
            if (!include(str, data)) {
                continue;
            }
            str = str.substring(0, str.lastIndexOf(':'));
            boolean cr = str.startsWith("core");
            TreeMap<ComponentType, MetaData<? extends Component>> children = cr ? core.get(str) : others.get(str);
            if (children == null) {
                children = new TreeMap<ComponentType, MetaData<? extends Component>> (TypeComparator.INSTANCE);
                if (cr) {
                    core.put(str, children);
                } else {
                    others.put(str, children);
                }
            }
            children.put(type, data);
        }
    }

    private boolean include(String type, MetaData<? extends Component> data) {
        if (data != null && data.isTest()) {
            if (data.isDeprecated()) {
                return false;
            }
            if (!includeTest) {
                boolean forced = false;
                for (String forceFilter : forceTestFilters) {
                    if (type.startsWith(forceFilter)) {
                        forced = true;
                        break;
                    }
                }
                if (!forced) {
                    return false;
                }
            }
        }

        if (filters == null) {
            return true;
        } else {
            for (String start : filters) {
                if (type.startsWith(start)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    

    @Override
    protected Node[] createNodes(String key) {
        if (key.startsWith("core")) {
            return new Node[]{new CategoryNode(key, core.get(key))};
        } else {
            return new Node[]{new CategoryNode(key, others.get(key))};
        }
        
    }

}
