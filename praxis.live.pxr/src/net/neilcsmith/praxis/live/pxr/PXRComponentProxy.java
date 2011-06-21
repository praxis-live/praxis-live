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
package net.neilcsmith.praxis.live.pxr;

import java.awt.EventQueue;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.ComponentInterface;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import org.openide.nodes.Node;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRComponentProxy implements ComponentProxy, Syncable {

    private final static Logger LOG = Logger.getLogger(PXRComponentProxy.class.getName());
    private final static Registry registry = new Registry();
    private PXRContainerProxy parent;
    private ComponentType type;
    private ComponentInfo info;
    private Map<String, String> attributes;
    private PropertyChangeSupport pcs;
    private PXRProxyNode delegate;
    private Map<String, PraxisProperty<?>> properties;
    private boolean syncing;

    PXRComponentProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        this.parent = parent;
        this.type = type;
        this.info = info;
        attributes = new LinkedHashMap<String, String>();
        pcs = new PropertyChangeSupport(this);
    }

    @Override
    public ComponentAddress getAddress() {
        return parent.getAddress(this);
    }

    @Override
    public PXRContainerProxy getParent() {
        return parent;
    }

    @Override
    public ComponentType getType() {
        return type;
    }

    @Override
    public ComponentInfo getInfo() {
        return info;
    }

    @Override
    public Node getNodeDelegate() {
        if (delegate == null) {
            delegate = new PXRProxyNode(this, getRoot().getSource());
        }
        return delegate;
    }

    @Override
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    @Override
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public String[] getAttributeKeys() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    void firePropertyChange(String property, Object oldValue, Object newValue) {
        pcs.firePropertyChange(property, oldValue, newValue);
    }

    @Override
    public void call(String control, CallArguments args, final Callback callback) throws ProxyException {
        try {
            ControlAddress to = ControlAddress.create(getAddress(), control);
            PXRHelper.getDefault().send(to, args, callback);
        } catch (Exception ex) {
            throw new ProxyException(ex);
        }
    }

    public String[] getPropertyIDs() {
        if (properties == null) {
            initProperties();
        }
        return properties.keySet().toArray(new String[0]);
    }

    public PraxisProperty<?> getProperty(String id) {
        if (properties == null) {
            initProperties();
        }
        return properties.get(id);
    }

    private void initProperties() {
        assert EventQueue.isDispatchThread();
        PropertyChangeListener lst = new PropPropListener();
        ComponentAddress cmpAd = getAddress();
        properties = new LinkedHashMap<String, PraxisProperty<?>>();
        File workingDir = getRoot().getWorkingDirectory();
        for (String ctlID : info.getControls()) {
            ControlInfo ctl = info.getControlInfo(ctlID);
            ControlAddress address = ControlAddress.create(cmpAd, ctlID);
            PraxisProperty<?> prop = createPropertyForControl(address, ctl);
            if (prop != null) {
                ((BoundArgumentProperty) prop).addPropertyChangeListener(lst);
                prop.setValue("address", address);
                prop.setValue("workingDir", workingDir);
                prop.setValue("componentInfo", info);
                properties.put(ctlID, prop);
            }
        }
        if (syncing) {
            setPropertySyncing(true);
        }
    }

    protected PraxisProperty<?> createPropertyForControl(ControlAddress address, ControlInfo info) {
        if (isIgnoredProperty(address.getID())) {
            return null;
        }
        if (info.getType() == ControlInfo.Type.Function) {
            return null;
        }
        ArgumentInfo[] args = info.getOutputsInfo();
        if (args.length != 1) {
            return null;
        }

        return BoundArgumentProperty.create(address, info);

    }

    protected boolean isIgnoredProperty(String id) {
        return ComponentInterface.INFO.equals(id);
    }

    PXRRootProxy getRoot() {
        return parent.getRoot();
    }

    @Override
    public void setSyncing(boolean sync) {
        assert EventQueue.isDispatchThread();
        if (syncing != sync) {
            syncing = sync;
            setPropertySyncing(sync);

        }
    }

    @Override
    public boolean isSyncing() {
        return syncing;
    }

    private void setPropertySyncing(boolean sync) {
        if (properties == null) {
            return;
        }
        for (PraxisProperty<?> prop : properties.values()) {
            if (prop instanceof Syncable) {
                ((Syncable) prop).setSyncing(sync);
            }
        }
    }

    private class PropPropListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            pcs.firePropertyChange(evt.getPropertyName(), null, null);
        }
    }

    private static class Registry implements LookupListener, PropertyChangeListener {

//        private Lookup.Result<PXRComponentProxy> result;
        private List<PXRComponentProxy> syncing;

        public Registry() {
            syncing = new ArrayList<PXRComponentProxy>();
//            result = Utilities.actionsGlobalContext().lookupResult(PXRComponentProxy.class);
//            result.addLookupListener(this);
            TopComponent.getRegistry().addPropertyChangeListener(this);
        }

        @Override
        public void resultChanged(LookupEvent ev) {
//            if (EventQueue.isDispatchThread()) {
//                resultChanged();
//            } else {
//                EventQueue.invokeLater(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        resultChanged();
//                    }
//                });
//            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            assert EventQueue.isDispatchThread();
            ArrayList<PXRComponentProxy> tmp = new ArrayList<PXRComponentProxy>();
            Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
            for (Node node : nodes) {
                PXRComponentProxy cmp = node.getLookup().lookup(PXRComponentProxy.class);
                if (cmp != null) {
                    tmp.add(cmp);
                }
            }
            syncing.removeAll(tmp);
            for (PXRComponentProxy cmp : syncing) {
                LOG.fine("Removing sync for : " + cmp.getAddress());
                cmp.setSyncing(false);
            }
            syncing.clear();
            syncing.addAll(tmp);
            for (PXRComponentProxy cmp : syncing) {
                LOG.fine("Adding sync for : " + cmp.getAddress());
                cmp.setSyncing(true);
            }
            tmp.clear();
        }
//        private void resultChanged() {
//            Collection<? extends PXRComponentProxy> res = result.allInstances();
//            syncing.removeAll(res);
//            for (PXRComponentProxy cmp : syncing) {
//                LOG.fine("Removing sync for : " + cmp.getAddress());
//                cmp.setSyncing(false);
//            }
//            syncing.clear();
//            syncing.addAll(res);
//            for (PXRComponentProxy cmp : syncing) {
//                LOG.fine("Adding sync for : " + cmp.getAddress());
//                cmp.setSyncing(true);
//            }
//        }
    }
}
