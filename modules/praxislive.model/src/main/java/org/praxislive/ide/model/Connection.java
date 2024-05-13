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
package org.praxislive.ide.model;

/**
 * A representation of a port connection.
 */
public final class Connection {

    private final String child1;
    private final String port1;
    private final String child2;
    private final String port2;

    /**
     * Create a connection representation.
     *
     * @param child1 name of first child
     * @param port1 name of port on first child
     * @param child2 name of second child
     * @param port2 name of port on second child
     */
    public Connection(String child1, String port1, String child2, String port2) {
        if (child1 == null || port1 == null || child2 == null || port2 == null) {
            throw new NullPointerException();
        }
        this.child1 = child1;
        this.port1 = port1;
        this.child2 = child2;
        this.port2 = port2;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Connection) {
            Connection c = (Connection) obj;
            return child1.equals(c.child1) && port1.equals(c.port1) && child2.equals(c.child2) && port2.equals(c.port2);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.child1 != null ? this.child1.hashCode() : 0);
        hash = 67 * hash + (this.port1 != null ? this.port1.hashCode() : 0);
        hash = 67 * hash + (this.child2 != null ? this.child2.hashCode() : 0);
        hash = 67 * hash + (this.port2 != null ? this.port2.hashCode() : 0);
        return hash;
    }

    /**
     * Name of first child component.
     *
     * @return first child name
     */
    public final String getChild1() {
        return child1;
    }

    /**
     * Name of second child component.
     *
     * @return second child name
     */
    public final String getChild2() {
        return child2;
    }

    /**
     * Name of port on first child component.
     *
     * @return name of port
     */
    public final String getPort1() {
        return port1;
    }

    /**
     * Name of port on second child component.
     *
     * @return name of port
     */
    public final String getPort2() {
        return port2;
    }

    @Override
    public final String toString() {
        return "~ " + child1 + "!" + port1 + " " + child2 + "!" + port2;
    }

}
