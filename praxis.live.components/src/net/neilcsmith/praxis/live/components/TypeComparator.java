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

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.ComponentType;

public class TypeComparator implements Comparator<ComponentType> {

//    private final static Logger LOG = Logger.getLogger(ComponentTypeComparator.class.getName());

    public final static TypeComparator INSTANCE = new TypeComparator();

    private TypeComparator() {}

    @Override
    public int compare(ComponentType type1, ComponentType type2) {

        return type1.toString().compareTo(type2.toString());

//        if (type1.equals(type2)) {
//            return 0;
//        }
//
//        String s1 = type1.toString();
//        String s2 = type2.toString();
//        String cat1 = s1.substring(0, s1.lastIndexOf(':'));
//        String cat2 = s2.substring(0, s2.lastIndexOf(':'));
//        int ret = cat1.compareTo(cat2);
//        if (ret == 0) {
//            ret = s1.compareTo(s2);
//        }

//        if (LOG.isLoggable(Level.FINE)) {
//            LOG.fine("Comparing " + type1 + " & " + type2 + " : " + ret);
//        }

//        return ret;

    }
}
