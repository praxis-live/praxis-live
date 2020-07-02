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

//import com.vdurmont.semver4j.Semver;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.model.Connection;
import org.praxislive.ide.model.ProxyException;
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
import org.praxislive.core.Value;
import org.praxislive.core.types.PError;

/**
 *
 */
@NbBundle.Messages({
    "MSG_versionWarning=File was created with a newer version of Praxis LIVE"
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
            if (registerRoot) {
                project.getLookup().lookup(PXRRootRegistry.class).register(rootProxy);
            }
            processCallback.onReturn(List.of());

        }
    }

    private boolean process(Element element) {
        if (element instanceof PropertyElement) {
            return processProperty((PropertyElement) element);
        } else if (element instanceof AttributeElement) {
            return processAttribute((AttributeElement) element);
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
                            try {
                                cmp.send("info", List.of(), new Callback() {
                                    @Override
                                    public void onReturn(List<Value> args) {
                                        try {
                                            cmp.refreshInfo(ComponentInfo.from(args.get(0)).orElseThrow());
                                        } catch (Exception ex) {
                                            Exceptions.printStackTrace(ex);
                                        }
                                        process();
                                    }

                                    @Override
                                    public void onError(List<Value> args) {
                                        process();
                                    }
                                });
                                return;
                            } catch (Exception ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        } else {
                            process();
                        }
                    }

                    @Override
                    public void onError(List<Value> args) {
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

    private boolean processAttribute(AttributeElement attr) {
        PXRComponentProxy cmp = findComponent(attr.component.address);
        if (cmp != null) {
            cmp.setAttr(attr.key, attr.value);
        }
        return true;
    }

    private boolean processConnection(final ConnectionElement con) {
        LOG.fine("Processing Connection Element : " + con.port1 + " -> " + con.port2);
        try {
            PXRComponentProxy parent = findComponent(con.container.address);
            if (parent instanceof PXRContainerProxy) {
                ((PXRContainerProxy) parent).connect(
                        new Connection(con.component1, con.port1, con.component2, con.port2),
                        new Callback() {
                    @Override
                    public void onReturn(List<Value> args) {
                        process();
                    }

                    @Override
                    public void onError(List<Value> args) {
                        connectionError(con, args);
                        process();
                    }
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
            helper.createComponentAndGetInfo(ad, type, new Callback() {
                @Override
                public void onReturn(List<Value> args) {
                    try {
                        rootProxy = new PXRRootProxy(
                                project,
                                helper,
                                source,
                                ad.rootID(),
                                type,
                                ComponentInfo.from(args.get(0)).orElseThrow());
                        process();
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                        onError(args);
                    }
                }

                @Override
                public void onError(List<Value> args) {
                    processError(args);
                }
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
            if (parent instanceof PXRContainerProxy) {
                String id = address.componentID(address.depth() - 1);
                ((PXRContainerProxy) parent).addChild(id, cmp.type, new Callback() {
                    @Override
                    public void onReturn(List<Value> args) {
                        if (parent.isDynamic()) {
                            parent.send("info", List.of(), new Callback() {
                                @Override
                                public void onReturn(List<Value> args) {
                                    try {
                                        parent.refreshInfo(ComponentInfo.from(args.get(0)).orElseThrow());
                                    } catch (Exception ex) {
                                        Exceptions.printStackTrace(ex);
                                    }
                                    process();
                                }

                                @Override
                                public void onError(List<Value> args) {
                                    process();
                                }
                            });
                            return;
                        } else {
                            process();
                        }
                    }

                    @Override
                    public void onError(List<Value> args) {
                        componentError(cmp, args);
                        process();
                    }
                });
                return false;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        componentError(cmp, List.of());
        return true;
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
        elements.addAll(Arrays.asList(component.attributes));
        elements.addAll(Arrays.asList(component.properties));
        for (ComponentElement child : component.children) {
            addComponentElements(child, elements);
        }
        elements.addAll(Arrays.asList(component.connections));
    }

}
