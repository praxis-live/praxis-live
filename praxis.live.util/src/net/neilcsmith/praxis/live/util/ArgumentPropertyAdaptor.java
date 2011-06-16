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
package net.neilcsmith.praxis.live.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.gui.ControlBinding;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class ArgumentPropertyAdaptor extends ControlBinding.Adaptor {

    private PropertyChangeSupport pcs;
    private String property;
    private boolean alwaysActive;
    private Argument value = PString.EMPTY;

    public ArgumentPropertyAdaptor(Object source, String property,
            boolean alwaysActive, ControlBinding.SyncRate rate) {
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

    void setValueImpl(Argument value, boolean send) {
        if (value == null) {
            throw new NullPointerException();
        }
        Argument oldValue = this.value;
        if (send) {
            send(CallArguments.create(value));
        }
        this.value = value;
        if (!Argument.equivalent(null, oldValue, value)) {
            pcs.firePropertyChange(property, oldValue, value);
        }
    }

    public Argument getValue() {
        return value;
    }

    @Override
    public void update() {
        Argument arg;
        ControlBinding binding = getBinding();
        if (binding == null) {
            arg = PString.EMPTY;
        } else {
            CallArguments args = binding.getArguments();
            if (args.getSize() > 0) {
                arg = args.get(0);
            } else {
                arg = PString.EMPTY;
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

    @Override
    public void updateBindingConfiguration() {
        // no op?
    }

    public static class ReadWrite extends ArgumentPropertyAdaptor {

        public ReadWrite(Object source, String property,
                boolean alwaysActive, ControlBinding.SyncRate rate) {
            super(source, property, alwaysActive, rate);
        }

        public void setValue(Argument value) {
            setValueImpl(value, true);
        }
    }

    public static class ReadOnly extends ArgumentPropertyAdaptor {

        public ReadOnly(Object source, String property,
                boolean alwaysActive, ControlBinding.SyncRate rate) {
            super(source, property, alwaysActive, rate);
        }

    }

}
