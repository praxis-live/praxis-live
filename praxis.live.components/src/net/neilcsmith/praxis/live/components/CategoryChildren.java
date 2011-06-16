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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.neilcsmith.praxis.core.ComponentType;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class CategoryChildren extends Children.Keys<String> {
    
    private String[] filters;
//    private java.util.Map<String, List<ComponentType>> map;
    private TreeMap<String, TreeSet<ComponentType>> core;
    private TreeMap<String, TreeSet<ComponentType>> others;
    

    public CategoryChildren() {
        this(null);
    }

    public CategoryChildren(String[] filters) {
        this.filters = filters;
        buildMap();
        List<String> keys = new ArrayList<String>();
        keys.addAll(core.keySet());
        keys.addAll(others.keySet());
        setKeys(keys);
    }
    
    private void buildMap() {
//        map = new LinkedHashMap<String, List<ComponentType>>();
//        Collection<ComponentType> types = ComponentRegistry.getDefault().getAllComponents();
//        for (ComponentType type : types) {
//            String str = type.toString();
//            if (!include(str)) {
//                continue;
//            }
//            str = str.substring(0, str.lastIndexOf(':'));
//            List<ComponentType> list = map.get(str);
//            if (list == null) {
//                list = new ArrayList<ComponentType>();
//                map.put(str, list);
//            }
//            list.add(type);
//        }
        core = new TreeMap<String, TreeSet<ComponentType>>();
        others = new TreeMap<String, TreeSet<ComponentType>>();
        Set<ComponentType> types = ComponentRegistry.getDefault().getAllComponents();
        for (ComponentType type : types) {
            String str = type.toString();
            if (!include(str)) {
                continue;
            }
            str = str.substring(0, str.lastIndexOf(':'));
            TreeMap<String, TreeSet<ComponentType>> map;
            if (str.startsWith("core")) {
                map = core;
            } else {
                map = others;
            }
            TreeSet<ComponentType> children = map.get(str);
            if (children == null) {
                children = new TreeSet<ComponentType>(TypeComparator.INSTANCE);
                map.put(str, children);
            }
            children.add(type);
        }
    }

    private boolean include(String type) {
//        if (type.contains(":test:")) {
//            return false;
//        }
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
        TreeMap<String, TreeSet<ComponentType>> map;
        if (key.startsWith("core")) {
            map = core;
        } else {
            map = others;
        }
        return new Node[]{new CategoryNode(key, map.get(key))};
    }

}
