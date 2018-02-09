/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.Value;
import org.praxislive.core.CallArguments;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PString;
import org.praxislive.gui.ControlBinding;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.properties.PraxisProperty;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class BoundArgumentProperty extends
        PraxisProperty<Value> {

    private final static Logger LOG = Logger.getLogger(BoundArgumentProperty.class.getName());

    private final PropertyChangeSupport pcs;
    private final Adaptor adaptor;
    private final ControlAddress address;
    private final ControlInfo info;
    private final boolean writable;
    private final boolean isTransient;
    private final Value defaultValue;

    private DelegatingArgumentEditor editor;
    private Value value;
    

    BoundArgumentProperty(ControlAddress address, ControlInfo info) {
        super(Value.class);
        if (address == null || info == null) {
            throw new NullPointerException();
        }
        if (info.getOutputsInfo().length != 1) {
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
        PXRHelper.getDefault().bind(address, adaptor);
        setName(address.getID());
        
        setValue("canAutoComplete", Boolean.FALSE);
        
    }
    
    private boolean isWritable(ControlInfo info) {
        boolean rw;
        switch (info.getType()) {
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
        Value[] defs = info.getDefaults();
        Value def = null;
        if (defs != null && defs.length > 0) {
            def = defs[0];
        }
        if (def == null) {
            def = PString.EMPTY;
        }
        return def;
    }
    
    private boolean isTransient(ControlInfo info) {
        return info.getProperties().getBoolean(ControlInfo.KEY_TRANSIENT, false);
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
        PXRHelper.getDefault().unbind(adaptor);
    }

    Class<? extends Value> getArgumentType() {
        return info.getOutputsInfo()[0].getType();
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
            pcs.firePropertyChange(address.getID(), oldValue, value);
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

    @Deprecated
    static BoundArgumentProperty create(ControlAddress address, ControlInfo info) {
        return new BoundArgumentProperty(address, info);
    }

    private class Adaptor extends ControlBinding.Adaptor {

        private Callback callback;

        private Adaptor() {
            setSyncRate(ControlBinding.SyncRate.Medium);
//            setActive(false);
        }

        @Override
        public void update() {
            Value arg = null;
            ControlBinding binding = getBinding();
            if (binding != null) {
                CallArguments args = binding.getArguments();
                if (args.getSize() > 0) {
                    arg = args.get(0);
                }
            }
            if (arg != null) {
                setValueImpl(arg, false, null);
            }
        }

        void sendValue(Value val, Callback callback) {
            send(CallArguments.create(val));
            if (callback != null) {
                if (this.callback != null) {
                    this.callback.onError(CallArguments.EMPTY);
                }
                this.callback = callback;
            }
        }

        @Override
        public void onResponse(CallArguments args) {
            if (callback != null) {
                Callback cb = callback;
                callback = null;
                cb.onReturn(args);
            }
        }

        @Override
        public void onError(CallArguments args) {
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
