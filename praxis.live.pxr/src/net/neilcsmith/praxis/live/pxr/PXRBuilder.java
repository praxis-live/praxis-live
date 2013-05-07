/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import net.neilcsmith.praxis.live.pxr.PXRParser.AttributeElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.ComponentElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.ConnectionElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.Element;
import net.neilcsmith.praxis.live.pxr.PXRParser.PropertyElement;
import net.neilcsmith.praxis.live.pxr.PXRParser.RootElement;
import net.neilcsmith.praxis.live.pxr.api.Connection;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXRBuilder {

    private final static Logger LOG = Logger.getLogger(PXRBuilder.class.getName());
    private PraxisProject project;
    private PXRDataObject source;
    private RootElement root;
    private Iterator<Element> iterator;
    private Callback processCallback;
    private PXRRootProxy rootProxy;
    private boolean processed;
    private boolean registerRoot;
    private List<String> warnings;

    private PXRBuilder(PraxisProject project, PXRDataObject source, RootElement root) {
        this.project = project;
        this.source = source;
        this.root = root;
        registerRoot = true;
        warnings = new ArrayList<String>();
    }

    private PXRBuilder(PXRRootProxy rootProxy, RootElement root) {
        this.rootProxy = rootProxy;
        this.root = root;
        registerRoot = false;
        warnings = new ArrayList<String>();
    }

    void process(Callback callback) {
        if (callback == null) {
            throw new NullPointerException();
        }
        this.processCallback = callback;
        buildElementIterator();
        process();
    }

    List<String> getErrors() {
        return warnings;
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
                DefaultRootRegistry.getDefault().register(rootProxy);
            }
            processCallback.onReturn(CallArguments.EMPTY);

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
        processCallback.onError(CallArguments.EMPTY);
        return false;
    }

    private void processError(CallArguments args) {
        processed = true;
        processCallback.onError(args);
    }

    private boolean processProperty(final PropertyElement prop) {
        LOG.log(Level.FINE, "Processing Property Element : {0}", prop.property);
        final PXRComponentProxy cmp = findComponent(prop.component.address);
        if (cmp == null) {
            propertyError(prop, CallArguments.EMPTY);
            return true;
        }
        PraxisProperty<?> p = cmp.getProperty(prop.property);
        if (p instanceof BoundArgumentProperty) {
            try {
                ((BoundArgumentProperty) p).setValue(prop.args[0], new Callback() {
                    @Override
                    public void onReturn(CallArguments args) {
                        if (cmp.isDynamic()) {
                            try {
                                cmp.call("info", CallArguments.EMPTY, new Callback() {
                                    @Override
                                    public void onReturn(CallArguments args) {
                                        cmp.refreshInfo((ComponentInfo) args.get(0));
                                        process();
                                    }

                                    @Override
                                    public void onError(CallArguments args) {
                                        process();
                                    }
                                });
                                return;
                            } catch (ProxyException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                        process();
                    }

                    @Override
                    public void onError(CallArguments args) {
                        propertyError(prop, args);
                        process();
                    }
                });
                return false;
            } catch (Exception ex) {
                LOG.warning("Couldn't set property " + prop.property);
            }
        }
        propertyError(prop, CallArguments.EMPTY);
        return true;
    }

    private void propertyError(PropertyElement prop, CallArguments args) {
        String err = "Couldn't set property " + prop.component.address + "." + prop.property;
        warnings.add(err);
    }

    private boolean processAttribute(AttributeElement attr) {
        PXRComponentProxy cmp = findComponent(attr.component.address);
        if (cmp != null) {
            cmp.setAttribute(attr.key, attr.value);
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
                            public void onReturn(CallArguments args) {
                                process();
                            }

                            @Override
                            public void onError(CallArguments args) {
                                connectionError(con, args);
                                process();
                            }
                        });
                return false;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        connectionError(con, CallArguments.EMPTY);
        return true;
    }

    private void connectionError(ConnectionElement connection, CallArguments args) {
        String p1 = connection.container.address + "/" + connection.component1 + "!" + connection.port1;
        String p2 = connection.container.address + "/" + connection.component2 + "!" + connection.port2;
        String err = "Couldn't create connection " + p1 + " -> " + p2;
        warnings.add(err);
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
            PXRHelper.getDefault().createComponentAndGetInfo(ad, type, new Callback() {
                @Override
                public void onReturn(CallArguments args) {
                    try {
                        rootProxy = new PXRRootProxy(
                                project,
                                source,
                                ad.getRootID(),
                                type,
                                ComponentInfo.coerce(args.get(0)));
                        process();
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                        onError(args);
                    }
                }

                @Override
                public void onError(CallArguments args) {
                    processError(args);
                }
            });

        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            processError(CallArguments.EMPTY);
        }
        return false;
    }

    private boolean processComponent(final ComponentElement cmp) {
        LOG.log(Level.FINE, "Processing Component Element : {0}, Type : {1}", new Object[]{cmp.address, cmp.type});
        try {
            ComponentAddress address = cmp.address;
            final PXRComponentProxy parent = findComponent(address.getParentAddress());
            if (parent instanceof PXRContainerProxy) {
                String id = address.getComponentID(address.getDepth() - 1);
                ((PXRContainerProxy) parent).addChild(id, cmp.type, new Callback() {
                    @Override
                    public void onReturn(CallArguments args) {
                        if (parent.isDynamic()) {
                            try {
                                parent.call("info", CallArguments.EMPTY, new Callback() {
                                    @Override
                                    public void onReturn(CallArguments args) {
                                        parent.refreshInfo((ComponentInfo) args.get(0));
                                        process();
                                    }

                                    @Override
                                    public void onError(CallArguments args) {
                                        process();
                                    }
                                });
                                return;
                            } catch (ProxyException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                        process();
                    }

                    @Override
                    public void onError(CallArguments args) {
                        componentError(cmp, args);
                        process();
                    }
                });
                return false;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        componentError(cmp, CallArguments.EMPTY);
        return true;
    }

    private void componentError(ComponentElement cmp, CallArguments args) {
        String err = "Couldn't create component " + cmp.address;
        warnings.add(err);
    }

    private PXRComponentProxy findComponent(ComponentAddress address) {
        if (rootProxy == null) {
            return null;
        }
        if (address.getDepth() == 1 && rootProxy.getAddress().equals(address)) {
            return rootProxy;
        } else if (!rootProxy.getAddress().getRootID().equals(address.getRootID())) {
            return null;
        }

        PXRComponentProxy cmp = rootProxy;
        for (int i = 1; i < address.getDepth(); i++) {
            if (cmp instanceof PXRContainerProxy) {
                cmp = ((PXRContainerProxy) cmp).getChild(address.getComponentID(i));
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

    static PXRBuilder getBuilder(PraxisProject project, PXRDataObject source, PXRParser.RootElement root) {
        return new PXRBuilder(project, source, root);
    }
    
    static PXRBuilder getBuilder(PXRRootProxy rootProxy, PXRParser.RootElement parseRoot) {
        return new PXRBuilder(rootProxy, parseRoot);
    }
    
}
