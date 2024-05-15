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
package org.praxislive.ide.pxr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.base.Binding;
import org.praxislive.core.Value;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.properties.PraxisProperty;

/**
 *
 */
public class BoundArgumentProperty extends
        PraxisProperty<Value> {

    private final static Logger LOG = Logger.getLogger(BoundArgumentProperty.class.getName());

    private final PropertyChangeSupport pcs;
    private final PXRHelper helper;
    private final Adaptor adaptor;
    private final ControlAddress address;
    private final ControlInfo info;
    private final boolean writable;
    private final boolean isTransient;
    private final Value defaultValue;

    private DelegatingArgumentEditor editor;
    private Value value;
    

    BoundArgumentProperty(PraxisProject project, ControlAddress address, ControlInfo info) {
        super(Value.class);
        if (address == null || info == null) {
            throw new NullPointerException();
        }
        if (info.outputs().size() != 1) {
            throw new IllegalArgumentException("Property doesn't accept single argument");
        }  
        this.address = address;
        this.info = info;
        this.writable = isWritable(info);
        this.defaultValue = getDefault(info);
        this.isTransient = isTransient(info);
        pcs = new PropertyChangeSupport(this);
        adaptor = new Adaptor();
        value = defaultValue;
        helper = Objects.requireNonNull(project.getLookup().lookup(PXRHelper.class),
                "No helper component found");
        helper.bind(address, adaptor);
        setName(address.controlID());
        
        setValue("canAutoComplete", Boolean.FALSE);
        setValue("project", project);
        
    }
    
    private boolean isWritable(ControlInfo info) {
        boolean rw;
        switch (info.controlType()) {
            case Property:
                rw = true;
                break;
            case ReadOnlyProperty:
                rw = false;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return rw;
    }

    private Value getDefault(ControlInfo info) {
        var defs = info.defaults();
        return defs.isEmpty() ? PString.EMPTY : defs.get(0);
    }
    
    private boolean isTransient(ControlInfo info) {
        return info.properties().getBoolean(ControlInfo.KEY_TRANSIENT, false);
    }
    
    @Override
    protected Editor createEditor() {
        if (editor == null) {
            editor = new DelegatingArgumentEditor(this, info);
        }
        editor.setValue(value);
        return editor;

    }

    @Override
    public Value getValue() {
        return value;
    }

    @Override
    public void setValue(Value value) {
        if (!writable) {
            throw new UnsupportedOperationException("Read only property");
        }
        if (this.value.equals(value)) {
            return;
        }
        setValueImpl(value, true, null);
    }

    @Override
    public void setValue(Value value, Callback callback) {
        setValueImpl(value, true, callback);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public boolean supportsDefaultValue() {
        return true;
    }

    @Override
    public boolean isDefaultValue() {
        return equivalent(defaultValue, value);
    }

    @Override
    public void restoreDefaultValue() {
        if (editor != null) {
            editor.restoreDefaultEditor();
        }
        setValue(defaultValue);
    }

    public boolean isTransient() {
        return isTransient;
    }

    @Override
    public String getHtmlDisplayName() {
        if (isTransient) {
            return "<i>" + getDisplayName() + "</i>";
        } else {
            return null;
        }
    }

    public void setSyncing(boolean sync) {
        if (LOG.isLoggable(Level.FINE)) {
            if (sync) {
                LOG.log(Level.FINE, "Activating binding for : {0}", address);
            } else {
                LOG.log(Level.FINE, "Deactivating binding for : {0}", address);
            }
        }
        adaptor.setActive(sync);
    }

    public boolean isSyncing() {
        return adaptor.isActive();
    }

    @Override
    public void dispose() {
        super.dispose();
        helper.unbind(address, adaptor);
    }

    Class<? extends Value> getArgumentType() {
        return Value.Type.fromName(info.outputs().get(0).argumentType()).get().asClass();
    }
    
    ControlAddress getAddress() {
        return address;
    }
    
    ControlInfo getInfo() {
        return info;
    }

    private void setValueImpl(Value value, boolean send, Callback callback) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value instanceof SubCommandArgument) {
            LOG.finest("Delegating to Property Editor");
            PropertyEditor ed = getPropertyEditor();
            ed.setValue(value);
            value = (Value) ed.getValue();
        }
        Value oldValue = this.value;
        if (send) {
            adaptor.sendValue(value, callback);
        }
        this.value = value;
        if (!equivalent(oldValue, value)) {
            pcs.firePropertyChange(address.controlID(), oldValue, value);
        }
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return writable;
    }
    
    private boolean equivalent(Value v1, Value v2) {
        return v1.equivalent(v2) || v2.equivalent(v1);
    }


    private class Adaptor extends Binding.Adaptor {

        private Callback callback;

        private Adaptor() {
            setSyncRate(Binding.SyncRate.Medium);
//            setActive(false);
        }

        @Override
        public void update() {
            Value arg = null;
            var binding = getBinding();
            if (binding != null) {
                var args = binding.getValues();
                if (args.size() > 0) {
                    arg = args.get(0);
                }
            }
            if (arg != null) {
                setValueImpl(arg, false, null);
            }
        }

        void sendValue(Value val, Callback callback) {
            send(List.of(val));
            if (callback != null) {
                if (this.callback != null) {
                    this.callback.onError(List.of());
                }
                this.callback = callback;
            }
        }

        @Override
        public void onResponse(List<Value> args) {
            if (callback != null) {
                Callback cb = callback;
                callback = null;
                cb.onReturn(args);
            }
        }

        @Override
        public void onError(List<Value> args) {
            if (callback != null) {
                Callback cb = callback;
                callback = null;
                cb.onError(args);
            }
        }

        @Override
        public void updateBindingConfiguration() {
            // no op?
        }
    }

}
