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
import java.awt.event.ActionEvent;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.ComponentInterface;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRComponentProxy implements ComponentProxy {

    private final static Logger LOG = Logger.getLogger(PXRComponentProxy.class.getName());
    private final static Registry registry = new Registry();
    private PXRContainerProxy parent;
    private ComponentType type;
    private ComponentInfo info;
    private Map<String, String> attributes;
    private PropertyChangeSupport pcs;
    private PXRProxyNode delegate;
    private Map<String, PraxisProperty<?>> properties;
    private List<Action> triggers;
    private Action editorAction;
    private boolean syncing;
    private int listenerCount = 0;
    private boolean nodeSyncing;

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
        if (listener instanceof PXRProxyNode.ComponentPropListener) {
            return;
        }
        listenerCount++;
        checkSyncing();
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
        if (listener instanceof PXRProxyNode.ComponentPropListener) {
            return;
        }
        listenerCount--;
        checkSyncing();
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
    
    List<Action> getTriggerActions() {
        if (triggers == null) {
            initTriggerActions();
        }
        return triggers;
    }

    private void initTriggerActions() {
        triggers = new ArrayList<Action>();
        for (String ctlID : info.getControls()) {
            ControlInfo ctl = info.getControlInfo(ctlID);
            if (ctl.getType() == ControlInfo.Type.Trigger) {
                triggers.add(new TriggerAction(ctlID));
            }
        }
        triggers = Collections.unmodifiableList(triggers);
    }
    
    Action getEditorAction() {
        if (editorAction == null) {
            editorAction = new EditorAction();
        }
        return editorAction;
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
            setPropertiesSyncing(true);
        }
    }

    protected PraxisProperty<?> createPropertyForControl(ControlAddress address, ControlInfo info) {
        if (isIgnoredProperty(address.getID())) {
            return null;
        }
        if (info.getType() != ControlInfo.Type.Property
                && info.getType() != ControlInfo.Type.ReadOnlyProperty) {
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

    private void setNodeSyncing(boolean sync) {
        assert EventQueue.isDispatchThread();
        nodeSyncing = sync;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Setting node syncing {0} on {1}", new Object[]{sync, getAddress()});
        }
        checkSyncing();
    }

    private void checkSyncing() {
        boolean toSync = nodeSyncing || (listenerCount > 0);
        if (toSync != syncing) {
            syncing = toSync;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Setting properties syncing {0} on {1}", new Object[]{toSync, getAddress()});
            }
            setPropertiesSyncing(toSync);
        }
    }

    private void setPropertiesSyncing(boolean sync) {
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

    private class TriggerAction extends AbstractAction {

        private String control;

        TriggerAction(String control) {
            super(control);
            this.control = control;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            try {
                call(control, CallArguments.EMPTY, new Callback() {

                    @Override
                    public void onReturn(CallArguments args) {
                        // do nothing
                    }

                    @Override
                    public void onError(CallArguments args) {
                        // ???
                    }
                });
            } catch (ProxyException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    
    private class EditorAction extends AbstractAction {
        
        private PXRComponentEditor editor;

        EditorAction() {
            super("Edit...");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (editor == null) {
                editor = new PXRComponentEditor(PXRComponentProxy.this);
            }
            editor.show();
        }
    }

    private static class Registry implements PropertyChangeListener {

        private List<PXRComponentProxy> syncing;

        public Registry() {
            syncing = new ArrayList<PXRComponentProxy>();
            TopComponent.getRegistry().addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            assert EventQueue.isDispatchThread();
            if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(evt.getPropertyName())) {
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
                    cmp.setNodeSyncing(false);
                }
                syncing.clear();
                syncing.addAll(tmp);
                for (PXRComponentProxy cmp : syncing) {
                    cmp.setNodeSyncing(true);
                }
                tmp.clear();
            }

        }
    }
}
