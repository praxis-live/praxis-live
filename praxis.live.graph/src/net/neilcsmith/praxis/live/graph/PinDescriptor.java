/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.graph;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PinDescriptor<N> {
    
    private N parent;
    private String name;
    private PinDirection direction;

    public PinDescriptor(N parent, String name) {
        this(parent, name, PinDirection.BiDirectional);
    }

    public PinDescriptor(N parent, String name, PinDirection direction) {
        if (parent == null || name == null || direction == null) {
            throw new NullPointerException();
        }
        this.parent = parent;
        this.name = name;
        this.direction = direction;
    }


    public N getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public PinDirection getDirection() {
        return direction;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PinDescriptor) {
            PinDescriptor o = (PinDescriptor) obj;
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




}
