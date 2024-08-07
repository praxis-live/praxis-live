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
package org.praxislive.ide.project;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Action;
import javax.swing.Timer;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.praxislive.base.Binding;

// @TODO replace compiler service access with script command
//import org.praxislive.code.CodeCompilerService;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.Info;
import org.praxislive.core.Value;
import org.praxislive.core.services.RootManagerService;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.praxislive.ide.model.HubProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.project.spi.RootRegistry;
import org.praxislive.ide.properties.PraxisProperty;

/**
 *
 */
class HubProxyImpl implements HubProxy {

    private final DefaultPraxisProject project;
    private final ValuePropertyAdaptor.ReadOnly rootsAdaptor;
    private final RootsChildren rootsChildren;
    private final Node hubNode;
    private final PropertyChangeSupport pcs;
    private final Map<String, RootProxy> roots;
    private final List<RootRegistry> rootRegistries;
    private final PropertyChangeListener regListener;

    private boolean ignoreChanges;
    private PArray libs;
    private Timer libsTimer;

    HubProxyImpl(DefaultPraxisProject project) {
        this.project = project;
        rootsAdaptor = new ValuePropertyAdaptor.ReadOnly(this, "roots", true, Binding.SyncRate.Low);
        rootsAdaptor.addPropertyChangeListener(e -> refreshRoots());
        regListener = e -> refreshProxies();
        rootsChildren = new RootsChildren();
        hubNode = new HubNode(rootsChildren);
        pcs = new PropertyChangeSupport(this);
        roots = new LinkedHashMap<>();
        rootRegistries = new ArrayList<>();
        libs = PArray.EMPTY;
    }

    @Override
    public RootProxy getRoot(String id) {
        return roots.get(id);
    }

    @Override
    public Stream<String> roots() {
        return roots.keySet().stream();
    }

    @Override
    public Node getNodeDelegate() {
        return hubNode;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }

    void start() {
        var helper = project.getLookup().lookup(ProjectHelper.class);
        try {
            var address = ControlAddress.of(
                    helper.findService(RootManagerService.class),
                    RootManagerService.ROOTS);
            helper.bind(address, rootsAdaptor);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        libsTimer = new Timer(10, e -> {
            helper.execScript("libraries-path")
                    .thenAccept(args -> {
                        if (!args.isEmpty()) {
                            PArray newLibs = PArray.from(args.get(0)).orElse(PArray.EMPTY);
                            if (!Objects.equals(libs, newLibs)) {
                                libs = newLibs;
                                project.updateLibs(libs);
                            }
                        }
                    });
        });
        libsTimer.setDelay(1000);
        libsTimer.start();
        project.getLookup().lookupAll(RootRegistry.class).forEach(r -> {
            if (rootRegistries.add(r)) {
                r.addPropertyChangeListener(regListener);
            }
        });
        ignoreChanges = false;
    }

    void stop() {
        ignoreChanges = true;
        var helper = project.getLookup().lookup(ProjectHelper.class);
        try {
            var address = ControlAddress.of(
                    helper.findService(RootManagerService.class),
                    RootManagerService.ROOTS);
            helper.unbind(address, rootsAdaptor);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        if (libsTimer != null) {
            libsTimer.stop();
            libsTimer = null;
        }
        rootsAdaptor.removePropertyChangeListener(regListener);
        rootRegistries.forEach(r -> r.removePropertyChangeListener(regListener));
        rootRegistries.clear();
        roots.forEach((id, proxy) -> {
            if (proxy instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) proxy).close();
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        roots.clear();
        rootsChildren.refreshRoots();
        pcs.firePropertyChange(ROOTS, null, null);
        ignoreChanges = false;
    }

    private void refreshRoots() {
        if (ignoreChanges) {
            return;
        }
        ignoreChanges = true;
        List<String> rts = PArray.from(rootsAdaptor.getValue()).orElse(PArray.EMPTY)
                .stream()
                .map(Value::toString)
                .collect(Collectors.toList());
        roots.forEach((id, proxy) -> {
            if (!rts.contains(id)) {
                if (proxy instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) proxy).close();
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        });
        roots.keySet().retainAll(rts);
        rts.forEach(r -> roots.compute(r, this::findRootProxy));
        rootsChildren.refreshRoots();
        pcs.firePropertyChange(ROOTS, null, null);
        ignoreChanges = false;
    }

    private void refreshProxies() {
        if (ignoreChanges) {
            return;
        }
        ignoreChanges = true;
        roots.replaceAll(this::findRootProxy);
        rootsChildren.refreshRoots();
        pcs.firePropertyChange(ROOTS, null, null);
        ignoreChanges = false;
    }

    private RootProxy findRootProxy(String id, RootProxy existing) {
        var proxy = project.getLookup().lookupAll(RootRegistry.class)
                .stream()
                .flatMap(reg -> reg.find(id).stream())
                .findFirst()
                .orElseGet(() -> existing instanceof FallbackRootProxy
                ? existing : new FallbackRootProxy(id));
        if (proxy != existing && existing instanceof AutoCloseable) {
            try {
                ((AutoCloseable) existing).close();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return proxy;
    }

    private class HubNode extends AbstractNode {

        private HubNode(Children children) {
            super(children);
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }
    }

    private class RootsChildren extends Children.Keys<RootProxy> {

        @Override
        protected Node[] createNodes(RootProxy key) {
            return new Node[]{key.getNodeDelegate()};
        }

        private void refreshRoots() {
            setKeys(roots.values());
        }

    }

    private class FallbackRootProxy implements RootProxy {

        private final ComponentAddress address;
        private final Node node;

        private FallbackRootProxy(String id) {
            this.address = ComponentAddress.of("/" + id);
            node = new FallbackRootNode(this);
        }

        @Override
        public ComponentAddress getAddress() {
            return address;
        }

        @Override
        public ComponentType getType() {
            return ComponentType.of("root:unknown");
        }

        @Override
        public ComponentInfo getInfo() {
            return Info.component(c -> c);
        }

        @Override
        public CompletionStage<List<Value>> send(String control, List<Value> args) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Node getNodeDelegate() {
            return node;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public Lookup getLookup() {
            return Lookup.EMPTY;
        }

        @Override
        public PraxisProperty<?> getProperty(String id) {
            return null;
        }

    }

    private class FallbackRootNode extends AbstractNode {

        private final String name;

        private FallbackRootNode(FallbackRootProxy proxy) {
            super(Children.LEAF, Lookups.singleton(proxy));
            name = proxy.address.rootID();
            setExpert(true);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

    }

}
