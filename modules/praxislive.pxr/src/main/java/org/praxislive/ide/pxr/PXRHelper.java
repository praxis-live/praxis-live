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

import java.util.List;
import java.util.Optional;
import org.praxislive.core.Component;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.protocols.ContainerProtocol;
import org.praxislive.core.services.RootManagerService;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.spi.ExtensionProvider;
import org.praxislive.ide.model.Connection;
import org.praxislive.ide.model.ProxyException;
import org.praxislive.ide.core.api.AbstractHelperComponent;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.core.Value;
import org.praxislive.core.types.PError;

/**
 *
 */
public class PXRHelper extends AbstractHelperComponent {

    private PXRHelper() {
    }

    void createComponentAndGetInfo(ComponentAddress address, ComponentType type,
            Callback callback) {
        try {
            if (address.depth() == 1) {
                String id = address.rootID();
                send(RootManagerService.class, RootManagerService.ADD_ROOT,
                        List.of(PString.of(id), type),
                        new GetInfoCallback(address, callback));
            } else {
                String id = address.componentID();
                send(ControlAddress.of(address.parent(), ContainerProtocol.ADD_CHILD),
                        List.of(PString.of(id), type),
                        new GetInfoCallback(address, callback));
            }
        } catch (Exception ex) {
            callback.onError(List.of(PError.of(ex)));
        }

    }

    void removeComponent(ComponentAddress address, Callback callback) {
        try {
            if (address.depth() == 1) {
                String id = address.rootID();
                send(RootManagerService.class, RootManagerService.REMOVE_ROOT,
                        List.of(PString.of(id)),
                        callback);
            } else {
                String id = address.componentID();
                send(ControlAddress.of(address.parent(), ContainerProtocol.REMOVE_CHILD),
                        List.of(PString.of(id)),
                        callback);
            }
        } catch (Exception ex) {
            callback.onError(List.of(PError.of(ex)));
        }
    }

    void connect(ComponentAddress container,
            Connection connection,
             Callback callback) {
        connectionImpl(container, connection, true, callback);
    }

    void disconnect(ComponentAddress container,
            Connection connection,
             Callback callback) {
        connectionImpl(container, connection, false, callback);
    }

    private void connectionImpl(ComponentAddress container,
            Connection connection,
            boolean connect, Callback callback) {
        try {
            PString c1ID = PString.of(connection.getChild1());
            PString p1ID = PString.of(connection.getPort1());
            PString c2ID = PString.of(connection.getChild2());
            PString p2ID = PString.of(connection.getPort2());

            send(ControlAddress.of(container,
                    connect ? ContainerProtocol.CONNECT : ContainerProtocol.DISCONNECT),
                    List.of(c1ID, p1ID, c2ID, p2ID),
                    callback);
        } catch (Exception ex) {
            callback.onError(List.of(PError.of(ex)));
        }
    }

    private class GetInfoCallback implements Callback {

        private final ComponentAddress address;
        private final Callback infoCallback;

        private GetInfoCallback(ComponentAddress address, Callback infoCallback) {
            this.address = address;
            this.infoCallback = infoCallback;
        }

        @Override
        public void onReturn(List<Value> args) {
            ControlAddress to = ControlAddress.of(address, ComponentProtocol.INFO);
            try {
                send(to, List.of(), infoCallback);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                onError(List.of(PError.of(ex)));
            }
        }

        @Override
        public void onError(List<Value> args) {
            infoCallback.onError(args);
        }
    }

    @ServiceProvider(service = ExtensionProvider.class)
    public static class Provider implements ExtensionProvider {

        @Override
        public Optional<Component> createExtension(Lookup context) {
            return Optional.of(new PXRHelper());
        }
    }
}
