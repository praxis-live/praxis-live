/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.graph;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public final class EdgeID<N> {

    private PinID<N> pin1;
    private PinID<N> pin2;

    public EdgeID(PinID<N> pin1, PinID<N> pin2) {
        if (pin1 == null || pin2 == null) {
            throw new NullPointerException();
        }
        this.pin1 = pin1;
        this.pin2 = pin2;
    }

    public PinID<N> getPin1() {
        return pin1;
    }

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
            return (pin1.equals(o.pin1) && pin2.equals(o.pin2)) ||
                    (pin2.equals(o.pin1) && pin1.equals(o.pin2));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pin1.hashCode() ^ pin2.hashCode();
    }



}
