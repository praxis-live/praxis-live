/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.core.api;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import org.praxislive.base.Binding;
import org.praxislive.core.Value;
import org.praxislive.core.types.PString;

/**
 *
 */
public abstract class ValuePropertyAdaptor extends Binding.Adaptor {

    private PropertyChangeSupport pcs;
    private String property;
    private boolean alwaysActive;
    private Value value = PString.EMPTY;

    public ValuePropertyAdaptor(Object source, String property,
            boolean alwaysActive, Binding.SyncRate rate) {
        if (property == null || rate == null) {
            throw new NullPointerException();
        }
        if (source == null) {
            pcs = new PropertyChangeSupport(this);
        } else {
            pcs = new PropertyChangeSupport(source);
        }
        this.property = property;
        this.alwaysActive = alwaysActive;
        setSyncRate(rate);
        if (alwaysActive) {
            setActive(true);
        }
    }

    void setValueImpl(Value value, boolean send) {
        if (value == null) {
            throw new NullPointerException();
        }
        Value oldValue = this.value;
        if (send) {
            send(List.of(value));
        }
        this.value = value;
        if (!(oldValue.equivalent(value) || value.equivalent(oldValue))) {
            pcs.firePropertyChange(property, oldValue, value);
        }
    }

    public Value getValue() {
        return value;
    }

    @Override
    public void update() {
        Value arg;
        Binding binding = getBinding();
        if (binding == null) {
            arg = PString.EMPTY;
        } else {
            List<Value> args = binding.getValues();
            if (args.isEmpty()) {
                arg = PString.EMPTY;
            } else {
                arg = args.get(0);
            }
        }
        setValueImpl(arg, false);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
        if (!alwaysActive) {
            setActive(true);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
        if (!alwaysActive && !pcs.hasListeners(null)) {
            setActive(false);
        }
    }

//    @Override
//    public void updateBindingConfiguration() {
//        // no op?
//    }

    public static class ReadWrite extends ValuePropertyAdaptor {

        public ReadWrite(Object source, String property,
                boolean alwaysActive, Binding.SyncRate rate) {
            super(source, property, alwaysActive, rate);
        }

        public void setValue(Value value) {
            setValueImpl(value, true);
        }
    }

    public static class ReadOnly extends ValuePropertyAdaptor {

        public ReadOnly(Object source, String property,
                boolean alwaysActive, Binding.SyncRate rate) {
            super(source, property, alwaysActive, rate);
        }

    }

}
