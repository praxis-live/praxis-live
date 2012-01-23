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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.InterfaceDefinition;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.interfaces.ContainerInterface;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRContainerProxy extends PXRComponentProxy implements ContainerProxy {

    private Map<String, PXRComponentProxy> children;
    private List<Connection> connections;

    PXRContainerProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        super(parent, type, info);
        children = new LinkedHashMap<String, PXRComponentProxy>();
        connections = new ArrayList<Connection>();
    }

    @Override
    public PXRComponentProxy getChild(String id) {
        return children.get(id);
    }

    @Override
    public String[] getChildIDs() {
        Set<String> keySet = children.keySet();
        return keySet.toArray(new String[keySet.size()]);
    }

    @Override
    public void addChild(final String id, final ComponentType type, final Callback callback)
            throws ProxyException {

        ComponentAddress childAddress = ComponentAddress.create(getAddress(), id);
        PXRHelper.getDefault().createComponentAndGetInfo(childAddress, type, new Callback() {

            @Override
            public void onReturn(CallArguments args) {
                try {
                    ComponentInfo info = ComponentInfo.coerce(args.get(0));
                    if (isContainer(info)) {
                        children.put(id, new PXRContainerProxy(PXRContainerProxy.this, type, info));
                    } else {
                        children.put(id, new PXRComponentProxy(PXRContainerProxy.this, type, info));
                    }

                    firePropertyChange(ContainerProxy.PROP_CHILDREN, null, null);
                    if (callback != null) {
                        callback.onReturn(args);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    onError(args);
                }
            }

            @Override
            public void onError(CallArguments args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });

    }

    private boolean isContainer(ComponentInfo info) {
        for (InterfaceDefinition def : info.getInterfaces()) {
            if (ContainerInterface.INSTANCE.equals(def)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeChild(final String id, final Callback callback) throws ProxyException {
        ComponentAddress childAddress = ComponentAddress.create(getAddress(), id);
        PXRHelper.getDefault().removeComponent(childAddress, new Callback() {

            @Override
            public void onReturn(CallArguments args) {

                children.remove(id);
                Iterator<Connection> itr = connections.iterator();
                boolean conChanged = false;
                while (itr.hasNext()) {
                    Connection con = itr.next();
                    if (con.getChild1().equals(id)
                            || con.getChild2().equals(id)) {
                        itr.remove();
                        conChanged = true;
                    }
                }
                if (conChanged) {
                    firePropertyChange(ContainerProxy.PROP_CONNECTIONS, null, null);
                }
                firePropertyChange(ContainerProxy.PROP_CHILDREN, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }


            }

            @Override
            public void onError(CallArguments args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });
    }

    @Override
    public void connect(final Connection connection, final Callback callback) throws ProxyException {

        PXRHelper.getDefault().connect(getAddress(), connection, new Callback() {

            @Override
            public void onReturn(CallArguments args) {
                connections.add(connection);
                firePropertyChange(ContainerProxy.PROP_CONNECTIONS, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }
            }

            @Override
            public void onError(CallArguments args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });
    }

    @Override
    public void disconnect(final Connection connection, final Callback callback) throws ProxyException {

        PXRHelper.getDefault().disconnect(getAddress(), connection, new Callback() {

            @Override
            public void onReturn(CallArguments args) {
                connections.remove(connection);
                firePropertyChange(ContainerProxy.PROP_CONNECTIONS, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }
            }

            @Override
            public void onError(CallArguments args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });

    }

    @Override
    public Node getNodeDelegate() {
        Node n = super.getNodeDelegate();
        n.getChildren().getNodes();
        return n;
    }

    ComponentAddress getAddress(PXRComponentProxy child) {
        String childID = getChildID(child);
        if (childID == null) {
            return null;
        } else {
            return ComponentAddress.create(getAddress(), childID);
        }
    }

    String getChildID(PXRComponentProxy child) {
        Set<Map.Entry<String, PXRComponentProxy>> entries = children.entrySet();
        for (Map.Entry<String, PXRComponentProxy> entry : entries) {
            if (entry.getValue() == child) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Connection[] getConnections() {
        return connections.toArray(new Connection[connections.size()]);
    }

    @Override
    protected boolean isIgnoredProperty(String id) {
        return super.isIgnoredProperty(id)
                || ContainerInterface.CHILDREN.equals(id)
                || ContainerInterface.CONNECTIONS.equals(id);

    }
}
