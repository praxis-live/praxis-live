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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.praxislive.core.Value;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.protocols.ContainerProtocol;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.openide.nodes.Node;
import org.praxislive.base.Binding;
import org.praxislive.core.Connection;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;

/**
 *
 */
public class PXRContainerProxy extends PXRComponentProxy implements ContainerProxy {

    private final static Logger LOG = Logger.getLogger(PXRContainerProxy.class.getName());

    private final Map<String, PXRComponentProxy> children;
    private final Map<String, CompletionStage<PXRComponentProxy>> pendingChildren;
    private final Set<Connection> connections;
    private final ChildrenProperty childProp;
    private final ConnectionsProperty conProp;
    private final SupportedTypesProperty supportedTypesProp;

    private ValuePropertyAdaptor.ReadOnly connectionsAdaptor;
    private ValuePropertyAdaptor.ReadOnly typesAdaptor;
    private ValuePropertyAdaptor.ReadOnly childrenAdaptor;

    PXRContainerProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        super(parent, type, info);
        children = new LinkedHashMap<>();
        connections = new LinkedHashSet<>();
        pendingChildren = new LinkedHashMap<>();
        childProp = new ChildrenProperty();
        conProp = new ConnectionsProperty();
        supportedTypesProp = new SupportedTypesProperty();

    }

    @Override
    List<? extends PraxisProperty<?>> getProxyProperties() {
        List<PraxisProperty<?>> proxies = new ArrayList<>(3);
        proxies.addAll(super.getProxyProperties());
        proxies.add(childProp);
        proxies.add(conProp);
        proxies.add(supportedTypesProp);
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
    public List<ComponentType> supportedTypes() {
        return supportedTypesProp.types();
    }

    @Override
    public CompletionStage<? extends PXRComponentProxy> addChild(final String id, final ComponentType type) {
        return addChild(id, type, PMap.EMPTY);
    }

    CompletionStage<? extends PXRComponentProxy> addChild(String id, ComponentType type, PMap attrs) {
        ComponentAddress childAddress = ComponentAddress.of(getAddress(), id);
        PXRHelper helper = getRoot().getHelper();
        CompletionStage<PXRComponentProxy> stage
                = helper.createComponent(childAddress, type)
                        .thenCompose(ad -> {
                            assert EventQueue.isDispatchThread();
                            return helper.componentData(ad);
                        })
                        .thenApply(data -> {
                            assert EventQueue.isDispatchThread();
                            return addChildProxy(id, data, attrs);
                        })
                        .whenComplete((c, ex) -> {
                            assert EventQueue.isDispatchThread();
                            pendingChildren.remove(id);
                        });
        pendingChildren.put(id, stage);
        return stage;
    }

    // called from listener for pre-existing children
    private void addChildProxy(String id) {
        ComponentAddress childAddress = ComponentAddress.of(getAddress(), id);
        PXRHelper helper = getRoot().getHelper();
        CompletionStage<PXRComponentProxy> stage = helper.componentData(childAddress)
                .thenApply(data -> {
                    assert EventQueue.isDispatchThread();
                    return addChildProxy(id, data, PMap.EMPTY);
                })
                .whenComplete((c, ex) -> {
                    assert EventQueue.isDispatchThread();
                    pendingChildren.remove(id);
                });
        pendingChildren.put(id, stage);
    }

    private PXRComponentProxy addChildProxy(String id, PMap data, PMap attrs) {
        ComponentInfo info = ComponentInfo.from(data.get("%info")).orElseThrow();
        ComponentType type = ComponentType.from(data.get("%type")).orElseThrow();
        PXRComponentProxy child;
        if (isContainer(info)) {
            child = new PXRContainerProxy(PXRContainerProxy.this, type, info);
        } else {
            child = new PXRComponentProxy(PXRContainerProxy.this, type, info);
        }
        children.put(id, child);
        attrs.keys().forEach(k -> child.setAttr(k, attrs.getString(k, null)));
        if (syncing) {
            child.setParentSyncing(true);
        }
        if (node != null) {
            node.refreshChildren();
        }
        firePropertyChange(ContainerProtocol.CHILDREN, null, null);
        return child;
    }

    private boolean isContainer(ComponentInfo info) {
        return info.hasProtocol(ContainerProtocol.class);
    }

    @Override
    public CompletionStage<?> removeChild(final String id) {
        ComponentAddress childAddress = ComponentAddress.of(getAddress(), id);
        return getRoot().getHelper().removeComponent(childAddress)
                .thenRun(() -> {
                    removeChildProxies(List.of(id));
                });
    }

    private void removeChildProxies(List<String> ids) {
        boolean conChanged = false;
        for (String id : ids) {
            PXRComponentProxy child = children.get(id); // dispose needs child in map
            if (child != null) {
                child.dispose();
            }
            children.remove(id);
            Iterator<Connection> itr = connections.iterator();
            while (itr.hasNext()) {
                Connection con = itr.next();
                if (con.sourceComponent().equals(id)
                        || con.targetComponent().equals(id)) {
                    itr.remove();
                    conChanged = true;
                }
            }
        }
        if (conChanged) {
            firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
        }
        if (node != null) {
            node.refreshChildren();
        }
        firePropertyChange(ContainerProtocol.CHILDREN, null, null);
    }

    @Override
    public CompletionStage<Connection> connect(final Connection connection) {
        return getRoot().getHelper().connect(getAddress(), connection)
                .thenApply(c -> {
                    connections.add(c);
                    firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                    return c;
                });

    }

    @Override
    public CompletionStage<?> disconnect(final Connection connection) {

        return getRoot().getHelper().disconnect(getAddress(), connection)
                .thenApply(c -> {
                    connections.remove(c);
                    firePropertyChange(ContainerProtocol.CONNECTIONS, null, null);
                    return c;
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
                || ContainerProtocol.CONNECTIONS.equals(id)
                || ContainerProtocol.SUPPORTED_TYPES.equals(id);

    }

    @Override
    void checkSyncing() {
        super.checkSyncing();
        if (connectionsAdaptor == null) {
            if (syncing) {
                initAdaptors();
            } else {
                return;
            }
        }
        if (syncing) {
            childrenAdaptor.setSyncRate(Binding.SyncRate.Medium);
            connectionsAdaptor.setSyncRate(Binding.SyncRate.Medium);
            typesAdaptor.setSyncRate(Binding.SyncRate.Low);
            children.forEach((id, child) -> child.setParentSyncing(true));
        } else {
            childrenAdaptor.setSyncRate(Binding.SyncRate.None);
            connectionsAdaptor.setSyncRate(Binding.SyncRate.None);
            typesAdaptor.setSyncRate(Binding.SyncRate.None);
            children.forEach((id, child) -> child.setParentSyncing(false));
        }
    }

    private void initAdaptors() {
        childrenAdaptor = new ValuePropertyAdaptor.ReadOnly(null,
                ContainerProtocol.CHILDREN, true, Binding.SyncRate.None);
        childrenAdaptor.addPropertyChangeListener(new ChildrenListener());
        connectionsAdaptor = new ValuePropertyAdaptor.ReadOnly(null,
                ContainerProtocol.CONNECTIONS, true, Binding.SyncRate.None);
        connectionsAdaptor.addPropertyChangeListener(new ConnectionsListener());
        typesAdaptor = new ValuePropertyAdaptor.ReadOnly(null,
                SUPPORTED_TYPES, true, Binding.SyncRate.None);
        typesAdaptor.addPropertyChangeListener(supportedTypesProp);
        getRoot().getHelper().bind(ControlAddress.of(getAddress(),
                ContainerProtocol.CHILDREN), childrenAdaptor);
        getRoot().getHelper().bind(ControlAddress.of(getAddress(),
                ContainerProtocol.CONNECTIONS), connectionsAdaptor);
        getRoot().getHelper().bind(ControlAddress.of(getAddress(), SUPPORTED_TYPES),
                typesAdaptor);
    }

    @Override
    void dispose() {
        for (PXRComponentProxy child : children.values()) {
            child.dispose();
        }
        children.clear();
        if (childrenAdaptor != null) {
            getRoot().getHelper().unbind(ControlAddress.of(getAddress(),
                    ContainerProtocol.CHILDREN), childrenAdaptor);
            childrenAdaptor = null;
        }
        if (connectionsAdaptor != null) {
            getRoot().getHelper().unbind(ControlAddress.of(getAddress(),
                    ContainerProtocol.CONNECTIONS), connectionsAdaptor);
            connectionsAdaptor = null;
        }
        if (typesAdaptor != null) {
            getRoot().getHelper().unbind(ControlAddress.of(getAddress(),
                    SUPPORTED_TYPES), typesAdaptor);
            typesAdaptor = null;
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
            return PArray.of(connections);
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
                Set<Connection> updated = eventToConnections(evt);
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

        private Set<Connection> eventToConnections(PropertyChangeEvent evt) throws Exception {
            return new LinkedHashSet<>(PArray.from((Value) evt.getNewValue())
                    .orElseThrow()
                    .asListOf(Connection.class));
        }

    }

    private class ChildrenListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            try {
                Set<String> childIDs = eventToChildIDs(evt);
                Set<String> scratch = new LinkedHashSet<>(childIDs);
                scratch.removeAll(children.keySet());
                scratch.removeAll(pendingChildren.keySet());
                if (!scratch.isEmpty()) {
                    scratch.forEach(id -> addChildProxy(id));
                }
                scratch.clear();
                scratch.addAll(children.keySet());
                scratch.removeAll(childIDs);
                if (!scratch.isEmpty()) {
                    removeChildProxies(List.copyOf(scratch));
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Invalid Children list", ex);

            }
        }

        private Set<String> eventToChildIDs(PropertyChangeEvent evt) throws Exception {
            return new LinkedHashSet<>(PArray.from((Value) evt.getNewValue())
                    .orElseThrow()
                    .asListOf(String.class));
        }

    }

    private class SupportedTypesProperty extends PraxisProperty<PArray> implements PropertyChangeListener {

        private List<ComponentType> types;

        public SupportedTypesProperty() {
            super(PArray.class);
            setName(SUPPORTED_TYPES);
            types = List.of();
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public PArray getValue() {
            return PArray.of(types);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            List<ComponentType> newTypes = PArray.from((Value) evt.getNewValue())
                    .map(a -> a.stream()
                    .flatMap(v -> ComponentType.from(v).stream())
                    .toList()
                    ).orElse(List.of());
            if (!types.equals(newTypes)) {
                types = List.copyOf(newTypes);
                firePropertyChange(SUPPORTED_TYPES, null, null);
            }
        }

        private List<ComponentType> types() {
            return types;
        }

    }

}
