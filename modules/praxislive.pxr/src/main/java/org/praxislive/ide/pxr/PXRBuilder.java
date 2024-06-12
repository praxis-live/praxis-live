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

//import com.vdurmont.semver4j.Semver;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.PraxisProject;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.pxr.PXRParser.AttributeElement;
import org.praxislive.ide.pxr.PXRParser.ComponentElement;
import org.praxislive.ide.pxr.PXRParser.ConnectionElement;
import org.praxislive.ide.pxr.PXRParser.Element;
import org.praxislive.ide.pxr.PXRParser.PropertyElement;
import org.praxislive.ide.pxr.PXRParser.RootElement;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.praxislive.core.Connection;
import org.praxislive.core.Value;
import org.praxislive.core.types.PError;
import org.praxislive.core.types.PMap;

/**
 *
 */
@NbBundle.Messages({
    "MSG_versionWarning=File was created with a newer version of PraxisLIVE"
})
class PXRBuilder {

    private final static Logger LOG = Logger.getLogger(PXRBuilder.class.getName());
    private final PraxisProject project;
    private final PXRDataObject source;
    private final RootElement root;
    private final List<String> warnings;
    private final boolean registerRoot;
    private final PXRHelper helper;

    private Iterator<Element> iterator;
    private Callback processCallback;
    private PXRRootProxy rootProxy;
    private boolean processed;

    PXRBuilder(PraxisProject project,
            PXRDataObject source,
            RootElement root,
            List<String> warnings) {
        this.project = project;
        this.source = source;
        this.root = root;
        registerRoot = true;
        this.warnings = warnings;
        helper = project.getLookup().lookup(PXRHelper.class);
    }

    PXRBuilder(PXRRootProxy rootProxy,
            RootElement root,
            List<String> warnings) {
        this.project = null;
        this.source = null;
        this.rootProxy = rootProxy;
        this.root = root;
        registerRoot = false;
        this.warnings = warnings;
        helper = rootProxy.getHelper();
    }

    void process(Callback callback) {
        if (callback == null) {
            throw new NullPointerException();
        }
        this.processCallback = callback;
//        if (Components.getRewriteDeprecated()) {
//            ElementRewriter rewriter = new ElementRewriter(root, warnings);
//            rewriter.process();
//        }
        checkVersion();
        buildElementIterator();
        process();
    }

    private void checkVersion() {
//        try {
//            for (AttributeElement attr : root.attributes) {
//                if (PXRParser.VERSION_ATTR.equals(attr.key)) {
//                    Semver fileVersion = new Semver(attr.value, Semver.SemverType.LOOSE);
//                    Semver runningVersion = new Semver(
//                            IDE.getVersion(),
//                            Semver.SemverType.LOOSE);
//                    if (fileVersion.isGreaterThan(runningVersion)) {
//                        warn(Bundle.MSG_versionWarning());
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            LOG.log(Level.WARNING, "Exception during checkVersion()", ex);
//        }

    }

    private void process() {
        while (iterator.hasNext()) {
            if (!process(iterator.next())) {
                //break;
                return;
            }
        }
        if (!processed && !iterator.hasNext()) {
            processed = true;
            processCallback.onReturn(List.of());

        }
    }

    private boolean process(Element element) {
        if (element instanceof PropertyElement) {
            return processProperty((PropertyElement) element);
        } else if (element instanceof ConnectionElement) {
            return processConnection((ConnectionElement) element);
        } else if (element instanceof RootElement) {
            return processRoot((RootElement) element);
        } else if (element instanceof ComponentElement) {
            return processComponent((ComponentElement) element);
        }
        processCallback.onError(List.of());
        return false;
    }

    private void processError(List<Value> args) {
        processed = true;
        processCallback.onError(args);
    }

    private void warn(String msg) {
        if (warnings == null) {
            return;
        }
        warnings.add(msg);
    }

    private boolean processProperty(final PropertyElement prop) {
        LOG.log(Level.FINE, "Processing Property Element : {0}", prop.property);
        final PXRComponentProxy cmp = findComponent(prop.component.address);
        if (cmp == null) {
            propertyError(prop, List.of());
            return true;
        }
        PraxisProperty<?> p = cmp.getProperty(prop.property);
        if (p instanceof BoundArgumentProperty) {
            try {
                ((BoundArgumentProperty) p).setValue(prop.args[0], new Callback() {
                    @Override
                    public void onReturn(List<Value> args) {
                        if (p instanceof BoundCodeProperty) {
                            p.setValue(BoundCodeProperty.KEY_LAST_SAVED, prop.args[0]);
                        }
                        if (cmp.isDynamic()) {
                            cmp.send("info", List.of())
                                    .thenAccept(res -> {
                                        cmp.refreshInfo(ComponentInfo.from(res.get(0)).orElseThrow());
                                    })
                                    .exceptionally(ex -> {
                                        Exceptions.printStackTrace(ex);
                                        return null;
                                    })
                                    .thenRun(() -> process());

                        } else {
                            process();
                        }
                    }

                    @Override
                    public void onError(List<Value> args) {
                        if (p instanceof BoundCodeProperty) {
                            p.setValue(BoundCodeProperty.KEY_LAST_SAVED, prop.args[0]);
                        }
                        propertyError(prop, args);
                        process();
                    }
                });
                return false;
            } catch (Exception ex) {
                LOG.warning("Couldn't set property " + prop.property);
            }
        }
        propertyError(prop, List.of());
        return true;
    }

    private void propertyError(PropertyElement prop, List<Value> args) {
        String err = "Couldn't set property " + prop.component.address + "." + prop.property;
        warn(err);
    }

    private boolean processConnection(final ConnectionElement con) {
        LOG.fine("Processing Connection Element : " + con.port1 + " -> " + con.port2);
        try {
            PXRComponentProxy parent = findComponent(con.container.address);
            if (parent instanceof PXRContainerProxy) {
                ((PXRContainerProxy) parent).connect(
                        Connection.of(con.component1, con.port1, con.component2, con.port2))
                        .whenComplete((c, ex) -> {
                            if (ex != null) {
                                connectionError(con, List.of());
                            }
                            process();
                        });
                return false;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        connectionError(con, List.of());
        return true;
    }

    private void connectionError(ConnectionElement connection, List<Value> args) {
        String p1 = connection.container.address + "/" + connection.component1 + "!" + connection.port1;
        String p2 = connection.container.address + "/" + connection.component2 + "!" + connection.port2;
        String err = "Couldn't create connection " + p1 + " -> " + p2;
        warn(err);
    }

    private boolean processRoot(RootElement root) {
        if (rootProxy != null) {
            LOG.log(Level.FINE, "Root already exists - ignoring Root Element : {0}, Type : {1}",
                    new Object[]{root.address, root.type});
            return true;
        }
        LOG.log(Level.FINE, "Processing Root Element : {0}, Type : {1}", new Object[]{root.address, root.type});
        try {
            final ComponentAddress ad = root.address;
            final ComponentType type = root.type;
            PMap attrs = attributesToMap(root.attributes);
            helper.createComponentAndGetInfo(ad, type).thenAccept(info -> {
                rootProxy = new PXRRootProxy(
                        project,
                        helper,
                        source,
                        ad.rootID(),
                        type,
                        info);
                attrs.keys().forEach(k -> rootProxy.setAttr(k, attrs.getString(k, null)));
                if (registerRoot) {
                    project.getLookup().lookup(PXRRootRegistry.class).register(rootProxy);
                }
                process();
            }).exceptionally(t -> {
                processError(List.of(PError.of(t instanceof Exception ex ? ex : new Exception())));
                return null;
            });

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            processError(List.of(PError.of(ex)));
        }
        return false;
    }

    private boolean processComponent(final ComponentElement cmp) {
        LOG.log(Level.FINE, "Processing Component Element : {0}, Type : {1}", new Object[]{cmp.address, cmp.type});
        try {
            ComponentAddress address = cmp.address;
            final PXRComponentProxy parent = findComponent(address.parent());
            PMap attrs = attributesToMap(cmp.attributes);
            if (parent instanceof PXRContainerProxy) {
                String id = address.componentID(address.depth() - 1);
                ((PXRContainerProxy) parent).addChild(id, cmp.type, attrs)
                        .thenCompose(child -> {
                            if (parent.isDynamic()) {
                                return helper.componentInfo(parent.getAddress())
                                        .thenAccept(info -> parent.refreshInfo(info));
                            } else {
                                return CompletableFuture.completedStage(null);
                            }
                        })
                        .exceptionally(ex -> {
                            componentError(cmp, List.of(PError.of((Exception) ex)));
                            return null;
                        })
                        .thenRun(() -> process());

                return false;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        componentError(cmp, List.of());
        return true;
    }

    private PMap attributesToMap(AttributeElement[] attrs) {
        if (attrs == null || attrs.length == 0) {
            return PMap.EMPTY;
        } else {
            var builder = PMap.builder();
            for (var attr : attrs) {
                builder.put(attr.key, attr.value);
            }
            return builder.build();
        }
    }

    private void componentError(ComponentElement cmp, List<Value> args) {
        String err = "Couldn't create component " + cmp.address;
        warn(err);
    }

    private PXRComponentProxy findComponent(ComponentAddress address) {
        if (rootProxy == null) {
            return null;
        }
        if (address.depth() == 1 && rootProxy.getAddress().equals(address)) {
            return rootProxy;
        } else if (!rootProxy.getAddress().rootID().equals(address.rootID())) {
            return null;
        }

        PXRComponentProxy cmp = rootProxy;
        for (int i = 1; i < address.depth(); i++) {
            if (cmp instanceof PXRContainerProxy) {
                cmp = ((PXRContainerProxy) cmp).getChild(address.componentID(i));
            } else {
                return null;
            }
        }
        return cmp;

    }

    private synchronized void buildElementIterator() {

        if (iterator != null) {
            throw new IllegalStateException();
        }
        List<Element> elements = new LinkedList<Element>();
        addComponentElements(root, elements);
        iterator = elements.iterator();

    }

    private void addComponentElements(ComponentElement component,
            List<Element> elements) {
        elements.add(component);
        elements.addAll(Arrays.asList(component.properties));
        for (ComponentElement child : component.children) {
            addComponentElements(child, elements);
        }
        elements.addAll(Arrays.asList(component.connections));
    }

}
