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

import org.praxislive.core.CallArguments;
import org.praxislive.core.Component;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.interfaces.ComponentInterface;
import org.praxislive.core.interfaces.ContainerInterface;
import org.praxislive.core.interfaces.RootManagerService;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.ExtensionProvider;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.model.Connection;
import org.praxislive.ide.model.ProxyException;
import org.praxislive.ide.util.AbstractHelperComponent;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRHelper extends AbstractHelperComponent {

    private static final PXRHelper INSTANCE = new PXRHelper();

    private PXRHelper() {
    }

    void createComponentAndGetInfo(ComponentAddress address, ComponentType type,
            Callback callback) throws ProxyException {
        try {
            if (address.getDepth() == 1) {
                String id = address.getRootID();
                send(RootManagerService.INSTANCE, RootManagerService.ADD_ROOT,
                        CallArguments.create(PString.valueOf(id), type),
                        new GetInfoCallback(address, callback));
            } else {
                String id = address.getComponentID(address.getDepth() - 1);
                send(ControlAddress.create(address.getParentAddress(), ContainerInterface.ADD_CHILD),
                        CallArguments.create(PString.valueOf(id), type),
                        new GetInfoCallback(address, callback));
            }
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }
    }

    void removeComponent(ComponentAddress address, Callback callback) throws ProxyException {
        try {
            if (address.getDepth() == 1) {
                String id = address.getRootID();
                send(RootManagerService.INSTANCE, RootManagerService.REMOVE_ROOT,
                        CallArguments.create(PString.valueOf(id)),
                        callback);
            } else {
                String id = address.getComponentID(address.getDepth() - 1);
                send(ControlAddress.create(address.getParentAddress(), ContainerInterface.REMOVE_CHILD),
                        CallArguments.create(PString.valueOf(id)),
                        callback);
            }
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }
    }

    void connect(ComponentAddress container,
            Connection connection, Callback callback) throws ProxyException {
        connectionImpl(container, connection, true, callback);
    }

    void disconnect(ComponentAddress container,
            Connection connection, Callback callback) throws ProxyException {
        connectionImpl(container, connection, false, callback);
    }

    private void connectionImpl(ComponentAddress container,
            Connection connection,
            boolean connect, Callback callback) throws ProxyException {
        try {
            PString c1ID = PString.valueOf(connection.getChild1());
            PString p1ID = PString.valueOf(connection.getPort1());
            PString c2ID = PString.valueOf(connection.getChild2());
            PString p2ID = PString.valueOf(connection.getPort2());

            send(ControlAddress.create(container,
                    connect ? ContainerInterface.CONNECT : ContainerInterface.DISCONNECT),
                    CallArguments.create(c1ID, p1ID, c2ID, p2ID),
                    callback);
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }
    }


    private static class GetInfoCallback implements Callback {

        private ComponentAddress address;
        private Callback infoCallback;

        private GetInfoCallback(ComponentAddress address, Callback infoCallback) {
            this.address = address;
            this.infoCallback = infoCallback;
        }

        @Override
        public void onReturn(CallArguments args) {
            ControlAddress to = ControlAddress.create(address, ComponentInterface.INFO);
            try {
                INSTANCE.send(to, CallArguments.EMPTY, infoCallback);
            } catch (HubUnavailableException ex) {
                Exceptions.printStackTrace(ex);
                onError(args);
            }
        }

        @Override
        public void onError(CallArguments args) {
            infoCallback.onError(args);
        }
    }

    public static PXRHelper getDefault() {
        return INSTANCE;
    }

    @ServiceProvider(service = ExtensionProvider.class)
    public static class Provider implements ExtensionProvider {

        @Override
        public Component getExtensionComponent() {
            return getDefault();
        }
    }
}
