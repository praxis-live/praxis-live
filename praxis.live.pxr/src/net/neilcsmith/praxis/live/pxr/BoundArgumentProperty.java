/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyEditor;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.gui.ControlBinding;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public final class BoundArgumentProperty extends ArgumentProperty
    implements Syncable {

    private final static Logger LOG = Logger.getLogger(BoundArgumentProperty.class.getName());

    private final PropertyChangeSupport pcs;
    private final Adaptor adaptor;
    private final ControlAddress address;
    private final ControlInfo info;
    private final boolean writable;
    private final boolean isTransient;

    private DelegatingArgumentEditor editor;
    private Argument value;
    private Argument def;


    private BoundArgumentProperty(ControlAddress address, ControlInfo info,
            Argument def, boolean writable, boolean isTransient) {
        this.address = address;
        this.def = def;
        this.info = info;
        this.writable = writable;
        this.isTransient = isTransient;
        pcs = new PropertyChangeSupport(this);
        adaptor = new Adaptor();
        value = def;
        PXRHelper.getDefault().bind(address, adaptor);
        setName(address.getID());
    }

    @Override
    public PraxisPropertyEditor getPropertyEditor() {
        if (editor == null) {
//            editor = new ArgumentEditor();
            editor = new DelegatingArgumentEditor(this, info);
        }
        editor.setValue(value);
        return editor;
    }

    @Override
    public Argument getValue() {
        return value;
    }

    @Override
    public void setValue(Argument value) {
        if (!writable) {
            throw new UnsupportedOperationException("Read only property");
        }
        if (this.value.equals(value)) {
            return;
        }
        setValueImpl(value, true, null);
    }

    public void setValue(Argument value, Callback callback) {
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
        return Argument.equivalent(null, def, value);
    }

    @Override
    public void restoreDefaultValue() {
        if (editor != null) {
            editor.restoreDefaultEditor();
        }
        setValue(def);
    }

    @Override
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

    


    @Override
    public void setSyncing(boolean sync) {
        if (LOG.isLoggable(Level.FINE)) {
            if (sync) {
                LOG.fine("Activating binding for : " + address);
            } else {
                LOG.fine("Deactivating binding for : " + address);
            }
        }
        adaptor.setActive(sync);
    }

    @Override
    public boolean isSyncing() {
        return adaptor.isActive();
    }

    void dispose() {
        PXRHelper.getDefault().unbind(adaptor);
    }
    

    private void setValueImpl(Argument value, boolean send, Callback callback) {
        if (value == null) {
            throw new NullPointerException();
        }
        if (value instanceof SubCommandArgument) {
            LOG.finest("Delegating to Property Editor");
            PropertyEditor ed = getPropertyEditor();
            ed.setValue(value);
            value = (Argument) ed.getValue();
        }
        Argument oldValue = this.value;
        if (send) {
            adaptor.sendValue(value, callback);
        }
        this.value = value;
        if (!Argument.equivalent(null, oldValue, value)) {
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


    static BoundArgumentProperty create(ControlAddress address, ControlInfo info) {
        if (address == null || info == null) {
            throw new NullPointerException();
        }
        boolean writable;
        switch (info.getType()) {
            case Property:
                writable = true;
                break;
            case ReadOnlyProperty:
                writable = false;
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (info.getOutputsInfo().length != 1) {
            throw new IllegalArgumentException("Property doesn't accept single argument");
        }
        Argument[] defs = info.getDefaults();
        Argument def = null;
        if (defs != null && defs.length > 0) {
            def = defs[0];
        }
        if (def == null) {
            def = PString.EMPTY;
        }
        boolean isTransient = info.getProperties().getBoolean(ControlInfo.KEY_TRANSIENT, false);
        return new BoundArgumentProperty(address, info, def, writable, isTransient);
    }


    private class Adaptor extends ControlBinding.Adaptor {

        private Callback callback;

        private Adaptor() {
            setSyncRate(ControlBinding.SyncRate.Medium);
//            setActive(false);
        }

        @Override
        public void update() {
            Argument arg = null;
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

        void sendValue(Argument val, Callback callback) {
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
