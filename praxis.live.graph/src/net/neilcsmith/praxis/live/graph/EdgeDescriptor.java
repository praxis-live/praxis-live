/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.graph;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public final class EdgeDescriptor<N> {

    private PinDescriptor<N> pin1;
    private PinDescriptor<N> pin2;

    public EdgeDescriptor(PinDescriptor<N> pin1, PinDescriptor<N> pin2) {
        if (pin1 == null || pin2 == null) {
            throw new NullPointerException();
        }
        this.pin1 = pin1;
        this.pin2 = pin2;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof EdgeDescriptor) {
            EdgeDescriptor o = (EdgeDescriptor) obj;
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
