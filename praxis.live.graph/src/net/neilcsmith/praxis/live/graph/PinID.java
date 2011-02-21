/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.graph;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PinID<N> {
    
    private N parent;
    private String name;

    public PinID(N parent, String name) {
        if (parent == null || name == null) {
            throw new NullPointerException();
        }
        this.parent = parent;
        this.name = name;
    }


    public N getParent() {
        return parent;
    }

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




}
