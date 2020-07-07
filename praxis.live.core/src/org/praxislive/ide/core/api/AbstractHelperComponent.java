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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.core.api;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.praxislive.base.AbstractComponent;
import org.praxislive.base.Binding;
import org.praxislive.base.BindingContext;
import org.praxislive.core.Call;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Control;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ExecutionContext;
import org.praxislive.core.Info;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.Value;
import org.praxislive.core.services.Service;
import org.praxislive.core.services.ServiceUnavailableException;

/**
 *
 */
public class AbstractHelperComponent extends AbstractComponent {

    public final static String PROP_HUB_CONNECTED = "connected";

    private final PropertyChangeSupport pcs;
    private final SendControl sender;
    private final String sendID;

    private boolean connected;
    private BindingContext bindingContext;
    private PacketRouter router;
    private ExecutionContext context;

    protected AbstractHelperComponent() {
        pcs = new PropertyChangeSupport(this);
        sendID = "_send_" + Integer.toHexString(System.identityHashCode(this));
        sender = new SendControl();
        registerControl(sendID, sender);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String property, Object oldValue, Object newValue) {
        pcs.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public void hierarchyChanged() {
        super.hierarchyChanged();
        router = getLookup().find(PacketRouter.class).orElse(null);
        context = getLookup().find(ExecutionContext.class).orElse(null);
        bindingContext = getLookup().find(BindingContext.class).orElse(null);
        if (connected) {
            if (bindingContext == null) {
                connected = false;
                pcs.firePropertyChange(PROP_HUB_CONNECTED, true, false);
            }
        } else if (bindingContext != null) {
            connected = true;
            sender.fromAddress = ControlAddress.of(getAddress(), sendID);
            pcs.firePropertyChange(PROP_HUB_CONNECTED, false, true);
        }

    }

    public final boolean isConnected() {
        return connected;
    }

    @Override
    public ComponentAddress findService(Class<? extends Service> service) throws ServiceUnavailableException {
        return super.findService(service);
    }
    
    // @TODO track and sync sends to existing bindings?
    public void send(ControlAddress to, List<Value> args, Callback callback)
            throws HubUnavailableException {
        sender.send(to, args, callback);
    }

    public void send(Class<? extends Service> service, String control,
            List<Value> args, Callback callback)
            throws HubUnavailableException, ServiceUnavailableException {
        var to = ControlAddress.of(findService(service), control);
        send(to, args, callback);
    }

    public void bind(ControlAddress address, Binding.Adaptor adaptor) {
        if (address == null || adaptor == null) {
            throw new NullPointerException();
        }
        bindingContext.bind(address, adaptor);
    }

    public void unbind(ControlAddress address, Binding.Adaptor adaptor) {
        if (adaptor == null) {
            throw new NullPointerException();
        }
        if (bindingContext != null) {
            bindingContext.unbind(address, adaptor);
        }
    }

    @Override
    public ComponentInfo getInfo() {
        return Info.component().build();
    }

    private class SendControl implements Control {

        private final Map<Integer, Callback> pending;

        private ControlAddress fromAddress;

        private SendControl() {
            this.pending = new HashMap<>();
        }

        @Override
        public void call(Call call, PacketRouter pr) throws Exception {
            if (call.isReply()) {
                handleResponse(call, false);
            } else if (call.isError()) {
                handleResponse(call, true);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        private void handleResponse(Call call, boolean error) {
            var cb = pending.remove(call.matchID());
            if (cb != null) {
                if (error) {
                    cb.onError(call.args());
                } else {
                    cb.onReturn(call.args());
                }
            }
        }

        private void send(ControlAddress to, List<Value> args, Callback callback)
                throws HubUnavailableException {
            if (router == null || context == null || fromAddress == null) {
                throw new HubUnavailableException();
            }
            boolean quiet = callback == null;
            Call call;
            if (quiet) {
                call = Call.createQuiet(to, fromAddress, context.getTime(), args);
            } else {
                call = Call.create(to, fromAddress, context.getTime(), args);
            }
            router.route(call);
            if (!quiet) {
                pending.put(call.matchID(), callback);
            }
        }

    }

}
