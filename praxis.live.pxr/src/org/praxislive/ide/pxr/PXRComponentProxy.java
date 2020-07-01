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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ProxyException;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.pxr.api.Attributes;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.praxislive.base.Binding;
import org.praxislive.core.types.PError;
import org.praxislive.ide.core.api.HubUnavailableException;

/**
 *
 */
public class PXRComponentProxy implements ComponentProxy {

    private final static Logger LOG = Logger.getLogger(PXRComponentProxy.class.getName());
    private final static Registry registry = new Registry();

    private final Map<String, String> attributes;
    private final Set<Object> syncKeys;
    private final PropertyChangeSupport pcs;
    private final ComponentType type;
    private final boolean dynamic;
    private final InfoProperty infoProp;
    private final Lookup lookup;

    PXRProxyNode node;

    private PXRContainerProxy parent;
    private ComponentInfo info;
    private Map<String, BoundArgumentProperty> properties;
    private PropPropListener propertyListener;
    private List<Action> triggers;
    private List<Action> propActions;
    private Action codeAction;
    private EditorAction editorAction;
    boolean syncing;
//    private int listenerCount = 0;
    boolean nodeSyncing;
    boolean parentSyncing;
    private ValuePropertyAdaptor.ReadOnly dynInfoAdaptor;

    PXRComponentProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        this.parent = parent;
        this.type = type;
        this.info = info;
        attributes = new LinkedHashMap<>();
        syncKeys = new HashSet<>();
        pcs = new PropertyChangeSupport(this);
        dynamic = info.properties().getBoolean(ComponentInfo.KEY_DYNAMIC, false);
        infoProp = new InfoProperty();
        lookup = createLookup();
    }

    private Lookup createLookup() {
        return Lookups.fixed(new Sync(), new Attr());
    }

    List<? extends PraxisProperty<?>> getProxyProperties() {
        return Collections.singletonList(infoProp);
    }

    private void initProperties() {
        assert EventQueue.isDispatchThread();
        if (propertyListener == null) {
            propertyListener = new PropPropListener();
        }
        ComponentAddress cmpAd = getAddress();
        Map<String, BoundArgumentProperty> oldProps;
        // properties might not be null if called from dynamic listener
        if (properties == null) {
            oldProps = Collections.emptyMap();
        } else {
            oldProps = properties;
        }
//        if (!oldProps.isEmpty()) {
//            for (BoundArgumentProperty prop : oldProps.values()) {
//                ((BoundArgumentProperty) prop).dispose();
//            }
//            oldProps.clear();
//        }
        properties = new LinkedHashMap<>();
        File workingDir = getRoot().getWorkingDirectory();
        for (String ctlID : info.controls()) {
            ControlInfo ctl = info.controlInfo(ctlID);
            BoundArgumentProperty prop = oldProps.remove(ctlID);
            if (prop != null && prop.getInfo().equals(ctl)) {
                // existing
                properties.put(ctlID, prop);
                continue;
            }

            ControlAddress address = ControlAddress.of(cmpAd, ctlID);
            prop = createPropertyForControl(address, ctl);
            if (prop != null) {
                ((BoundArgumentProperty) prop).addPropertyChangeListener(propertyListener);
                prop.setValue("address", address);
                prop.setValue("workingDir", workingDir);
                prop.setValue("componentInfo", info);
                properties.put(ctlID, prop);
            }
        }

        if (!oldProps.isEmpty()) {
            for (PraxisProperty<?> prop : oldProps.values()) {
                ((BoundArgumentProperty) prop).dispose();
            }
            oldProps.clear();
        }
        if (syncing) {
            setPropertiesSyncing(true);
        }
    }

    private void initDynamic() {
        LOG.finest("Setting up dynamic component adaptor");
        dynInfoAdaptor = new ValuePropertyAdaptor.ReadOnly(this, "info", true, Binding.SyncRate.None);
        dynInfoAdaptor.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    refreshInfo(ComponentInfo.from((Value) evt.getNewValue()).orElseThrow());
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "", ex);
                }
            }
        });
        getRoot().getHelper().bind(ControlAddress.of(getAddress(), ComponentProtocol.INFO), dynInfoAdaptor);
    }

    void refreshInfo(ComponentInfo info) {
        if (this.info.equals(info)) {
            // should happen once on first sync?
            LOG.finest("Info is current");
            return;
        }
        LOG.finest("Info changed - revalidating");
        this.info = info;
        initProperties();
        triggers = null;
        propActions = null;

        if (node != null) {
            node.refreshProperties();
            node.refreshActions();
        }
//        if (parent != null) {
//            parent.revalidate(this);
//        }
        firePropertyChange(ComponentProtocol.INFO, null, null);
    }

    boolean isDynamic() {
        return dynamic;
    }

    @Override
    public ComponentAddress getAddress() {
        return parent == null ? null : parent.getAddress(this);
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
        if (node == null) {
//            node = new PXRProxyNode(this, getRoot().getSource());
            node = new PXRProxyNode(this);
        }
        return node;
    }

    void setAttr(String key, String value) {
        if (value == null) {
            attributes.remove(key);
        } else {
            attributes.put(key, value);
        }
    }

    String getAttr(String key) {
        return attributes.get(key);
    }

    String[] getAttrKeys() {
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
        if (node != null) {
            node.propertyChange(property, oldValue, newValue);
        }
    }

    public void send(String control, List<Value> args, final Callback callback) {
        try {
            ControlAddress to = ControlAddress.of(getAddress(), control);
            getRoot().getHelper().send(to, args, callback);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            EventQueue.invokeLater(() -> callback.onError(List.of(PError.of(ex))));
        }
    }

    List<Action> getTriggerActions() {
        if (triggers == null) {
            initTriggerActions();
        }
        return triggers;
    }

    private void initTriggerActions() {
        triggers = new ArrayList<>();
        for (String ctlID : info.controls()) {
            ControlInfo ctl = info.controlInfo(ctlID);
            if (ctl.controlType() == ControlInfo.Type.Action) {
                triggers.add(new TriggerAction(ctlID));
            }
        }
        triggers = Collections.unmodifiableList(triggers);
    }

    List<Action> getPropertyActions() {
        if (propActions == null) {
            initPropertyActions();
        }
        return propActions;
    }

    Action getCodeEditAction() {
        getPropertyActions(); // ensure initialized
        return codeAction;
    }

    private void initPropertyActions() {
        if (properties == null) {
            initProperties();
        }
        propActions = new ArrayList<>();
        BoundCodeProperty code = null;
        for (BoundArgumentProperty prop : properties.values()) {
            if (prop instanceof BoundCodeProperty) {
                // @TODO add proper key for this
                if ("code".equals(prop.getName())) {
                    code = (BoundCodeProperty) prop;
                    continue;
                }
                propActions.add(((BoundCodeProperty) prop).getEditAction());
                propActions.add(((BoundCodeProperty) prop).getResetAction());
            }
        }
        if (code != null) {
            if (!propActions.isEmpty()) {
                propActions.add(null); //separator
            }
            propActions.add(code.getEditAction());
            propActions.add(code.getResetAction());
            codeAction = code.getEditAction();
        }
        propActions = Collections.unmodifiableList(propActions);
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

    public BoundArgumentProperty getProperty(String id) {
        if (properties == null) {
            initProperties();
        }
        return properties.get(id);
    }

    protected BoundArgumentProperty createPropertyForControl(ControlAddress address, ControlInfo info) {
        if (isProxiedProperty(address.controlID())) {
            return null;
        }
        if (info.controlType() != ControlInfo.Type.Property
                && info.controlType() != ControlInfo.Type.ReadOnlyProperty) {
            return null;
        }
        var args = info.outputs();
        if (args.size() != 1) {
            return null;
        }
        if (args.get(0).type().asClass() == PString.class) {
            String mime = args.get(0).properties().getString(PString.KEY_MIME_TYPE, null);
            if (mime != null) {
                return new BoundCodeProperty(getRoot().getProject(), address, info, mime);
            }
        }

        return new BoundArgumentProperty(getRoot().getProject(), address, info);

    }

    protected boolean isProxiedProperty(String id) {
        return ComponentProtocol.INFO.equals(id);
    }

    PXRRootProxy getRoot() {
        return parent.getRoot();
    }

    void dispose() {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Dispose called on {0}", getAddress());
        }

        parent = null;

        if (dynInfoAdaptor != null) {
            getRoot().getHelper().unbind(
                    ControlAddress.of(getAddress(), ComponentProtocol.INFO),
                    dynInfoAdaptor);
        }

        if (editorAction != null && editorAction.editor != null) {
            editorAction.editor.dispose();
        }

        if (properties == null) {
            return;
        }
        for (PraxisProperty<?> prop : properties.values()) {
            if (prop instanceof BoundArgumentProperty) {
                LOG.log(Level.FINE, "Calling dispose on {0} property", prop.getName());
                ((BoundArgumentProperty) prop).dispose();
            }
        }
        properties = null;
    }

    private void setNodeSyncing(boolean sync) {
        assert EventQueue.isDispatchThread();
        nodeSyncing = sync;
        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Setting node syncing {0} on {1}", new Object[]{sync, getAddress()});
        }
        checkSyncing();
    }

    void setParentSyncing(boolean sync) {
        if (parentSyncing != sync) {
            parentSyncing = sync;
            checkSyncing();
        }
    }

    void checkSyncing() {
        boolean toSync = nodeSyncing || !syncKeys.isEmpty();
        if (toSync != syncing) {
            syncing = toSync;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Setting properties syncing {0} on {1}", new Object[]{toSync, getAddress()});
            }
            setPropertiesSyncing(toSync);
        }
        if (dynamic) {
            if (dynInfoAdaptor == null) {
                initDynamic();
            }
            if (syncing || parentSyncing) {
                dynInfoAdaptor.setSyncRate(Binding.SyncRate.Low);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Setting info syncing {0} on {1}", new Object[]{true, getAddress()});
                }
            } else {
                dynInfoAdaptor.setSyncRate(Binding.SyncRate.None);
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Setting info syncing {0} on {1}", new Object[]{false, getAddress()});
                }
            }
        }
    }

    private void setPropertiesSyncing(boolean sync) {
        if (properties == null) {
            return;
        }
        for (PraxisProperty<?> prop : properties.values()) {
            if (prop instanceof BoundArgumentProperty) {
                ((BoundArgumentProperty) prop).setSyncing(sync);
            }
        }
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private class PropPropListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            // dispatch to our listeners and node's listeners
            firePropertyChange(evt.getPropertyName(), null, null);
        }
    }

    private class Sync implements Syncable {

        @Override
        public void addKey(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            syncKeys.add(key);
            checkSyncing();
        }

        @Override
        public void removeKey(Object key) {
            if (syncKeys.remove(key)) {
                checkSyncing();
            }
        }

    }

    private class Attr implements Attributes {

        @Override
        public void setAttribute(String key, String value) {
            setAttr(key, value);
        }

        @Override
        public String getAttribute(String key) {
            return getAttr(key);
        }

    }

//    private class DynPropListener implements PropertyChangeListener {
//
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            // info changed
//            LOG.finest("Info changed event");
//
//
//        }
//    }
    private class InfoProperty extends PraxisProperty<ComponentInfo> {

        private InfoProperty() {
            super(ComponentInfo.class);
            setName(ComponentProtocol.INFO);
        }

        @Override
        public ComponentInfo getValue() {
            return info;
        }

        @Override
        public boolean canRead() {
            return true;
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
            send(control, List.of(), Callback.create(r -> {
            }));
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

        private final List<PXRComponentProxy> syncing;

        public Registry() {
            syncing = new ArrayList<>();
            TopComponent.getRegistry().addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            try {
                assert EventQueue.isDispatchThread();

                if (TopComponent.Registry.PROP_ACTIVATED_NODES.equals(evt.getPropertyName())) {
                    ArrayList<PXRComponentProxy> tmp = new ArrayList<>();
                    Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "Activated nodes = {0}", Arrays.toString(nodes));
                    }
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
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }

        }
    }
}
