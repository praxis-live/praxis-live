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
package org.praxislive.ide.pxr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.protocols.ContainerProtocol;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.model.Connection;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.praxislive.base.Binding;
import org.praxislive.core.types.PString;

/**
 *
 */
public class PXRContainerProxy extends PXRComponentProxy implements ContainerProxy {

    private final static Logger LOG = Logger.getLogger(PXRContainerProxy.class.getName());

    private final Map<String, PXRComponentProxy> children;
    private final Set<Connection> connections;
    private final ChildrenProperty childProp;
    private final ConnectionsProperty conProp;
    private ValuePropertyAdaptor.ReadOnly conAdaptor;

    boolean ignore;

    PXRContainerProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        super(parent, type, info);
        children = new LinkedHashMap<>();
        connections = new LinkedHashSet<>();
        childProp = new ChildrenProperty();
        conProp = new ConnectionsProperty();

    }

    @Override
    List<? extends PraxisProperty<?>> getProxyProperties() {
        List<PraxisProperty<?>> proxies = new ArrayList<>(3);
        proxies.addAll(super.getProxyProperties());
        proxies.add(childProp);
        proxies.add(conProp);
        return proxies;
    }

    @Override
    public PXRComponentProxy getChild(String id) {
        return children.get(id);
    }

    @Override
    public Stream<String> children() {
        return children.keySet().stream();
    }

    @Override
    public void addChild(final String id, final ComponentType type, final Callback callback) {

        ComponentAddress childAddress = ComponentAddress.of(getAddress(), id);
        getRoot().getHelper().createComponentAndGetInfo(childAddress, type, new Callback() {
            @Override
            public void onReturn(List<Value> args) {
                try {
                    ComponentInfo info = ComponentInfo.from(args.get(0)).orElseThrow();
                    if (isContainer(info)) {
                        children.put(id, new PXRContainerProxy(PXRContainerProxy.this, type, info));
                    } else {
                        children.put(id, new PXRComponentProxy(PXRContainerProxy.this, type, info));
                    }
                    if (node != null) {
                        node.refreshChildren();
                    }
                    firePropertyChange(ContainerProtocol.CHILDREN, null, null);
                    if (callback != null) {
                        callback.onReturn(args);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    onError(args);
                }
            }

            @Override
            public void onError(List<Value> args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });

    }

    private boolean isContainer(ComponentInfo info) {
        return info.hasProtocol(ContainerProtocol.class);
    }

    @Override
    public void removeChild(final String id, final Callback callback) {
        ComponentAddress childAddress = ComponentAddress.of(getAddress(), id);
        getRoot().getHelper().removeComponent(childAddress, new Callback() {
            @Override
            public void onReturn(List<Value> args) {
                PXRComponentProxy child = children.get(id);
                if (child != null) {
                    child.dispose();
                }
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
                    firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                }
                if (node != null) {
                    node.refreshChildren();
                }
                firePropertyChange(ContainerProtocol.CHILDREN, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }

            }

            @Override
            public void onError(List<Value> args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });
    }

    @Override
    public void connect(final Connection connection, final Callback callback) {

        getRoot().getHelper().connect(getAddress(), connection, new Callback() {
            @Override
            public void onReturn(List<Value> args) {
                connections.add(connection);
                firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }
            }

            @Override
            public void onError(List<Value> args) {
                if (callback != null) {
                    callback.onError(args);
                }
            }
        });
    }

    @Override
    public void disconnect(final Connection connection, final Callback callback) {

        getRoot().getHelper().disconnect(getAddress(), connection, new Callback() {
            @Override
            public void onReturn(List<Value> args) {
                connections.remove(connection);
                firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                if (callback != null) {
                    callback.onReturn(args);
                }
            }

            @Override
            public void onError(List<Value> args) {
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
            return ComponentAddress.of(getAddress(), childID);
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
    public Stream<Connection> connections() {
        return connections.stream();
    }

    @Override
    protected boolean isProxiedProperty(String id) {
        return super.isProxiedProperty(id)
                || ContainerProtocol.CHILDREN.equals(id)
                || ContainerProtocol.CONNECTIONS.equals(id);

    }

    void revalidate(PXRComponentProxy child) {
//        String id = getChildID(child);
//        if (id == null) {
//            return;
//        }
//
//        ignore = true;
//
//        // remove all connections temporarily
//        List<Connection> tmpCons = connections;
//        connections = Collections.emptyList();
//        if (!tmpCons.isEmpty()) {
//            firePropertyChange(PROP_CONNECTIONS, null, null);
//        }
//
//        // temporarily remove child
//        Map<String, PXRComponentProxy> tmpChildren = children;
//        children = new LinkedHashMap<String, PXRComponentProxy>(tmpChildren);
//        children.remove(id);
//        firePropertyChange(PROP_CHILDREN, null, null);
//
//        // re-add child
//        children.clear();
//        children = tmpChildren;
//        firePropertyChange(PROP_CHILDREN, null, null);
//
//        // re-add and validate connections
//        connections = tmpCons;
//        List<String> ports = Arrays.asList(child.getInfo().getPorts());
//        Iterator<Connection> itr = connections.iterator();
//        while (itr.hasNext()) {
//            Connection con = itr.next();
//            if ((con.getChild1().equals(id) && !ports.contains(con.getPort1()))
//                    || (con.getChild2().equals(id) && !ports.contains(con.getPort2()))) {
//                itr.remove();
//            }
//        }
//        firePropertyChange(ContainerProxy.PROP_CONNECTIONS, null, null);
//
//        ignore = false;

    }

    @Override
    void checkSyncing() {
        super.checkSyncing();
        if (conAdaptor == null) {
            initConAdaptor();
        }
        if (syncing) {
            conAdaptor.setSyncRate(Binding.SyncRate.Low);
        } else {
            conAdaptor.setSyncRate(Binding.SyncRate.None);
        }
    }

    private void initConAdaptor() {
        conAdaptor = new ValuePropertyAdaptor.ReadOnly(this,
                ContainerProtocol.CONNECTIONS, true, Binding.SyncRate.None);
        conAdaptor.addPropertyChangeListener(new ConnectionsListener());
        getRoot().getHelper().bind(ControlAddress.of(getAddress(),
                ContainerProtocol.CONNECTIONS), conAdaptor);
    }

    @Override
    void dispose() {
        for (PXRComponentProxy child : children.values()) {
            child.dispose();
        }
        if (conAdaptor != null) {
            getRoot().getHelper().unbind(ControlAddress.of(getAddress(),
                    ContainerProtocol.CONNECTIONS), conAdaptor);
        }
        super.dispose();
    }

    private class ChildrenProperty extends PraxisProperty<PArray> {

        private ChildrenProperty() {
            super(PArray.class);
            setName(ContainerProtocol.CHILDREN);
        }

        @Override
        public PArray getValue() {
            return children().map(PString::of).collect(PArray.collector());
        }

        @Override
        public boolean canRead() {
            return true;
        }

    }

    private class ConnectionsProperty extends PraxisProperty<PArray> {

        private ConnectionsProperty() {
            super(PArray.class);
            setName(ContainerProtocol.CONNECTIONS);
        }

        @Override
        public PArray getValue() {
            return connections()
                    .map(c -> PArray.of(
                    PString.of(c.getChild1()),
                    PString.of(c.getPort1()),
                    PString.of(c.getChild2()),
                    PString.of(c.getPort2())
            )).collect(PArray.collector());
        }

        @Override
        public boolean canRead() {
            return true;
        }

    }

    private class ConnectionsListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            try {
                Set<Connection> updated = externalToConnections((Value) evt.getNewValue());
                if (connections.equals(updated)) {
                    LOG.fine("Connections change reported but we're up to date.");
                } else {
                    LOG.fine("Connections change reported - updating.");
                    connections.clear();
                    connections.addAll(updated);
                    firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Invalid Connection list", ex);
            }
        }

        private Set<Connection> externalToConnections(Value extCons) throws ValueFormatException {
            if (extCons.isEmpty()) {
                return Collections.emptySet();
            }
            PArray extArr = PArray.from(extCons).orElseThrow(ValueFormatException::new);
            Set<Connection> cons = new LinkedHashSet<>(extArr.size());
            for (Value arg : extArr) {
                PArray con = PArray.from(arg).orElseThrow(ValueFormatException::new);
                if (con.size() != 4) {
                    throw new ValueFormatException("Connection array has invalid number of parts\n" + extCons);
                }
                cons.add(new Connection(con.get(0).toString(), con.get(1).toString(),
                        con.get(2).toString(), con.get(3).toString()));
            }
            return cons;
        }

    }

}
