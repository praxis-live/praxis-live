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
 * ID of a connection (edge) between two pin IDs on nodes.
 *
 * @param <N> node type
 */
public final class EdgeID<N> {

    private PinID<N> pin1;
    private PinID<N> pin2;

    /**
     * Construct an EdgeID representing a connection between the two provided
     * PinID.
     *
     * @param pin1 first pin
     * @param pin2 second pin
     */
    public EdgeID(PinID<N> pin1, PinID<N> pin2) {
        if (pin1 == null || pin2 == null) {
            throw new NullPointerException();
        }
        this.pin1 = pin1;
        this.pin2 = pin2;
    }

    /**
     * Get the ID of the first pin.
     *
     * @return first pin
     */
    public PinID<N> getPin1() {
        return pin1;
    }

    /**
     * Get the ID of the second pin.
     *
     * @return second pin
     */
    public PinID<N> getPin2() {
        return pin2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EdgeID) {
            EdgeID o = (EdgeID) obj;
            return (pin1.equals(o.pin1) && pin2.equals(o.pin2))
                    || (pin2.equals(o.pin1) && pin1.equals(o.pin2));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pin1.hashCode() ^ pin2.hashCode();
    }

    @Override
    public String toString() {
        return pin1.toString() + " --> " + pin2.toString();
    }

}
