/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * A base class for components to be provided via {@link ExtensionProvider} to
 * allow modules to communicate with the PraxisCORE system. Unless otherwise
 * stated, all methods should be called on, and all listeners are fired on, the
 * Swing event thread.
 */
public class AbstractHelperComponent extends AbstractComponent {

    /**
     * Property name of events fired when the component is connected into and
     * removed from a PraxisCORE hub.
     */
    public final static String PROP_HUB_CONNECTED = "connected";

    private final PropertyChangeSupport pcs;
    private final SendControl sender;
    private final String sendID;

    private boolean connected;
    private BindingContext bindingContext;
    private PacketRouter router;
    private ExecutionContext context;

    /**
     * Create a helper component.
     */
    protected AbstractHelperComponent() {
        pcs = new PropertyChangeSupport(this);
        sendID = "_send_" + Integer.toHexString(System.identityHashCode(this));
        sender = new SendControl();
        registerControl(sendID, sender);
    }

    /**
     * Add a property listener.
     *
     * @param listener property listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove a property listener.
     *
     * @param listener property listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Fire a property change event.
     *
     * @param property name of property
     * @param oldValue old value of property
     * @param newValue new value of property
     */
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

    /**
     * Query whether this helper component is connected into a running
     * PraxisCORE hub.
     *
     * @return connected to hub
     */
    public final boolean isConnected() {
        return connected;
    }

    /**
     * Find the address of a service.
     *
     * @param service type of service
     * @return address of service
     * @throws ServiceUnavailableException if no service provider found
     */
    @Override
    public ComponentAddress findService(Class<? extends Service> service) throws ServiceUnavailableException {
        return super.findService(service);
    }

    /**
     * Send a call to a control with the provided arguments.
     *
     * @param to control to call
     * @param args arguments
     * @param callback callback to handle response
     * @throws HubUnavailableException if not connected
     */
    // @TODO track and sync sends to existing bindings?
    public void send(ControlAddress to, List<Value> args, Callback callback)
            throws HubUnavailableException {
        sender.send(to, args, callback);
    }

    /**
     * Send a call to a control on a service with the provided arguments.
     *
     * @param service service to look up
     * @param control control to call
     * @param args arguments
     * @param callback callback to handle response
     * @throws HubUnavailableException if not connected
     * @throws ServiceUnavailableException if no service provider found
     */
    public void send(Class<? extends Service> service, String control,
            List<Value> args, Callback callback)
            throws HubUnavailableException, ServiceUnavailableException {
        var to = ControlAddress.of(findService(service), control);
        send(to, args, callback);
    }

    /**
     * Add a binding to the provided control.
     *
     * @param address control address
     * @param adaptor binding adaptor
     */
    public void bind(ControlAddress address, Binding.Adaptor adaptor) {
        if (address == null || adaptor == null) {
            throw new NullPointerException();
        }
        bindingContext.bind(address, adaptor);
    }

    /**
     * Remove a binding to the provided control.
     *
     * @param address control address
     * @param adaptor binding adaptor
     */
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
