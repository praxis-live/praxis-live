/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.praxislive.core.Value;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.pxr.api.Attributes;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponent;
import org.praxislive.base.Binding;
import org.praxislive.core.types.PMap;

/**
 *
 */
public class PXRComponentProxy implements ComponentProxy {

    private final static Logger LOG = Logger.getLogger(PXRComponentProxy.class.getName());
    private final static Registry registry = new Registry();

    private final Set<Object> syncKeys;
    private final PropertyChangeSupport pcs;
    private final ComponentType type;
    private final boolean dynamic;
    private final InfoProperty infoProp;
    private final MetaProperty metaProp;

    PXRProxyNode node;

    private PXRContainerProxy parent;
    private ComponentInfo info;
    private Lookup lookup;
    private Map<String, BoundArgumentProperty> properties;
    private PropPropListener propertyListener;
    private List<Action> triggers;
    private List<Action> propActions;
    private Action codeAction;
    private EditorAction editorAction;
    boolean syncing;
    boolean nodeSyncing;
    boolean parentSyncing;
    private ValuePropertyAdaptor.ReadOnly dynInfoAdaptor;
    private ValuePropertyAdaptor.ReadOnly metaAdaptor;

    PXRComponentProxy(PXRContainerProxy parent, ComponentType type,
            ComponentInfo info) {
        this.parent = parent;
        this.type = type;
        this.info = info;
        syncKeys = new HashSet<>();
        pcs = new PropertyChangeSupport(this);
        dynamic = info.properties().getBoolean(ComponentInfo.KEY_DYNAMIC, false);
        infoProp = new InfoProperty();
        metaProp = new MetaProperty(PMap.EMPTY);
    }

    Lookup createLookup() {
        return Lookups.fixed(getRoot().getProject(), metaProp, new Sync());
    }

    List<? extends PraxisProperty<?>> getProxyProperties() {
        return List.of(infoProp, metaProp);
    }

    boolean isHiddenFunction(String id) {
        return ComponentProtocol.META_MERGE.equals(id);
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

    private void initMeta() {
        metaAdaptor = new ValuePropertyAdaptor.ReadOnly(this, ComponentProtocol.META, true, Binding.SyncRate.None);
        metaAdaptor.addPropertyChangeListener(metaProp);
        getRoot().getHelper().bind(ControlAddress.of(getAddress(), ComponentProtocol.META), metaAdaptor);
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
            node.configure();
        }
        firePropertyChange(ComponentProtocol.INFO, null, null);
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
            node = new PXRProxyNode(this);
        }
        return node;
    }

    void setAttr(String key, String value) {
        metaProp.setAttribute(key, value);
    }

    String getAttr(String key) {
        return metaProp.getAttribute(key);
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

    @Override
    public CompletionStage<List<Value>> send(String control, List<Value> args) {
        try {
            ControlAddress to = ControlAddress.of(getAddress(), control);
            return getRoot().getHelper().send(to, args);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return CompletableFuture.failedStage(ex);
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
        triggers = List.copyOf(triggers);
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
            Action sharedBaseAction = code.getSharedBaseAction();
            if (sharedBaseAction != null) {
                propActions.add(sharedBaseAction);
            }
            codeAction = code.getQuickEditAction();
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

    @Override
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
        if ("String".equals(args.get(0).argumentType())) {
            String mime = args.get(0).properties().getString(PString.KEY_MIME_TYPE, null);
            if (mime != null) {
                return new BoundCodeProperty(getRoot().getProject(), address, info, mime);
            }
        }

        return new BoundArgumentProperty(getRoot().getProject(), address, info);

    }

    protected boolean isProxiedProperty(String id) {
        return ComponentProtocol.INFO.equals(id) || ComponentProtocol.META.equals(id);
    }

    PXRRootProxy getRoot() {
        return parent.getRoot();
    }

    void dispose() {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Dispose called on {0}", getAddress());
        }

        if (metaAdaptor != null) {
            getRoot().getHelper().unbind(
                    ControlAddress.of(getAddress(), ComponentProtocol.META),
                    metaAdaptor);
        }

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

        syncing = parentSyncing = false;
        parent = null;
        properties = null;
    }

    private void setNodeSyncing(boolean sync) {
        assert EventQueue.isDispatchThread();
        if (nodeSyncing != sync) {
            nodeSyncing = sync;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Setting node syncing {0} on {1}", new Object[]{sync, getAddress()});
            }
            checkSyncing();
        }
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
        if (metaAdaptor == null) {
            initMeta();
        }
        if (syncing || parentSyncing) {
            metaAdaptor.setSyncRate(Binding.SyncRate.Low);
        } else {
            metaAdaptor.setSyncRate(Binding.SyncRate.None);
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
        if (lookup == null) {
            lookup = createLookup();
        }
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

    private class MetaProperty extends PraxisProperty<PMap> implements Attributes, PropertyChangeListener {

        private PMap meta;

        private MetaProperty(PMap value) {
            super(PMap.class);
            setName(ComponentProtocol.META);
            this.meta = Objects.requireNonNull(value);
        }

        @Override
        public boolean canRead() {
            return true;
        }

        @Override
        public PMap getValue() {
            return meta;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            PMap newMeta = PMap.from((Value) evt.getNewValue()).orElse(PMap.EMPTY);
            PMap oldMeta = meta;
            meta = newMeta;
            if (!newMeta.equivalent(oldMeta)) {
                pcs.firePropertyChange(ComponentProtocol.META, oldMeta, newMeta);
            }
        }

        @Override
        public void setAttribute(String key, String value) {
            setAttributeValue(key, value == null ? null : PString.of(value));
        }

        @Override
        public String getAttribute(String key) {
            return meta.getString(key, null);
        }

        @Override
        public void setAttributeValue(String key, Value value) {
            PMap oldMeta = meta;
            PMap metaMerge = PMap.of(key, value == null ? PString.EMPTY : value);
            meta = PMap.merge(oldMeta, metaMerge, PMap.REPLACE);
            send(ComponentProtocol.META_MERGE, List.of(metaMerge));
            pcs.firePropertyChange(ComponentProtocol.META, oldMeta, meta);
        }

        @Override
        public <T extends Value> T getAttributeValue(Class<T> type, String key) {
            Value value = meta.get(key);
            if (value == null) {
                return null;
            } else if (type.isInstance(value)) {
                return type.cast(value);
            } else {
                return Value.Type.of(type).converter().apply(value).orElse(null);
            }
        }

    }

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

        private final String control;

        TriggerAction(String control) {
            super(control);
            this.control = control;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            send(control, List.of());
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
