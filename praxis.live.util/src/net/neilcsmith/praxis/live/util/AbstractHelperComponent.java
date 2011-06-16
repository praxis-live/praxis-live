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

import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.core.api.Callback;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.InterfaceDefinition;
import net.neilcsmith.praxis.core.PacketRouter;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.gui.BindingContext;
import net.neilcsmith.praxis.gui.ControlBinding;
import net.neilcsmith.praxis.impl.AbstractComponent;
import net.neilcsmith.praxis.live.core.api.HubUnavailableException;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class AbstractHelperComponent extends AbstractComponent {

    private final static Logger LOG = Logger.getLogger(AbstractHelperComponent.class.getName());
    public final static String PROP_HUB_CONNECTED = "connected";
    private PropertyChangeSupport pcs;
    private SendControl sender;
    private boolean connected;
//    private Map<ControlAddress, List<ControlBinding.Adaptor>> bindings;
    private Map<ControlBinding.Adaptor, ControlAddress> bindings;
    private BindingContext bindingContext;

    protected AbstractHelperComponent() {
        pcs = new PropertyChangeSupport(this);
        String sendID = "_send_" + Integer.toHexString(hashCode());
        sender = new SendControl();
        registerControl(sendID, sender);
        bindings = new HashMap<ControlBinding.Adaptor, ControlAddress>();
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
        bindingContext = getLookup().get(BindingContext.class);
        if (connected) {
            if (bindingContext == null) {
                connected = false;
                pcs.firePropertyChange(PROP_HUB_CONNECTED, true, false);
            }
        } else if (bindingContext != null) {
            connected = true;
            pcs.firePropertyChange(PROP_HUB_CONNECTED, false, true);
        }

    }

    public final boolean isConnected() {
        return connected;
    }

    @Override
    public ComponentAddress findService(InterfaceDefinition service) throws ServiceUnavailableException {
        return super.findService(service);
    }

    // @TODO track and sync sends to existing bindings?
    public void send(ControlAddress to, CallArguments args, Callback callback)
            throws HubUnavailableException {
        sender.send(to, args, callback);
    }

    public void send(InterfaceDefinition service, String control,
            CallArguments args, Callback callback)
            throws HubUnavailableException, ServiceUnavailableException {
        sender.send(service, control, args, callback);
    }

    public void bind(ControlAddress address, ControlBinding.Adaptor adaptor) {
        if (address == null || adaptor == null) {
            throw new NullPointerException();
        }
        bindingContext.bind(address, adaptor);
    }

    public void unbind(ControlBinding.Adaptor adaptor) {
        if (adaptor == null) {
            throw new NullPointerException();
        }
        bindingContext.unbind(adaptor);
    }

    private void rebind() {
        for (Map.Entry<ControlBinding.Adaptor, ControlAddress> entry : bindings.entrySet()) {
            bindingContext.bind(entry.getValue(), entry.getKey());
        }
    }
}
