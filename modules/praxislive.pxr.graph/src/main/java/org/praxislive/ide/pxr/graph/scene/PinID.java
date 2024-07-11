/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.graph.scene;

/**
 * ID of a pin on a node.
 *
 * @param <N> node type
 */
public class PinID<N> {

    private N parent;
    private String name;

    /**
     * Construct a PinID for a pin with the given name on the provided node.
     *
     * @param parent parent node
     * @param name pin name
     */
    public PinID(N parent, String name) {
        if (parent == null || name == null) {
            throw new NullPointerException();
        }
        this.parent = parent;
        this.name = name;
    }

    /**
     * Get the parent node.
     *
     * @return parent node
     */
    public N getParent() {
        return parent;
    }

    /**
     * Get pin name.
     *
     * @return pin name
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PinID) {
            PinID o = (PinID) obj;
            if (parent.equals(o.parent) && name.equals(o.name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + this.parent.hashCode();
        hash = 17 * hash + this.name.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "Pin : " + parent.toString() + ">" + name;
    }

}
