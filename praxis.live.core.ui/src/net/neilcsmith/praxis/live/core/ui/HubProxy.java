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
package net.neilcsmith.praxis.live.core.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.interfaces.RootManagerService;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.gui.ControlBinding.SyncRate;
import net.neilcsmith.praxis.live.util.ArgumentPropertyAdaptor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class HubProxy {

    private final static Logger LOG = Logger.getLogger(HubProxy.class.getName());
    final static String SYSTEM_PREFIX = "_";


    private ArgumentPropertyAdaptor.ReadOnly rootsAdaptor;
//    private RootProxy[] roots;
    private Map<String, RootProxy> roots;
    private RootChildren children;
    private Node delegate;
    private boolean showSystemRoots;

    HubProxy() {
//        roots = new RootProxy[0];
        roots = new LinkedHashMap<String, RootProxy>();
        children = new RootChildren();
        rootsAdaptor = new ArgumentPropertyAdaptor.ReadOnly(null, "roots", true, SyncRate.Low);
        rootsAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                refreshRoots();
            }
        });
        HubUIHelper.getDefault().addPropertyChangeListener(new HubListener());
        bindRootsAdaptor();
    }

    Node getNodeDelegate() {
        if (delegate == null) {
            delegate = new DelegateNode(children);
        }
        return delegate;
    }

    void setShowSystemRoots(boolean show) {
        if (showSystemRoots != show) {
            showSystemRoots = show;
            refreshRoots();
        }
    }

    private void refreshRoots() {
        Set<String> oldRoots = new LinkedHashSet<String>(roots.keySet());
        PArray rts = PArray.EMPTY;
        try {
            rts = PArray.coerce(rootsAdaptor.getValue());
            LOG.finest("Roots found : " + rts);
        } catch (ArgumentFormatException ex) {
            // leave list empty
        }
        for (int i = 0; i < rts.getSize(); i++) {
            String id = rts.get(i).toString();
            if (!showSystemRoots && id.startsWith(SYSTEM_PREFIX)) {
                continue;
            }
            if (!oldRoots.remove(id)) {
                // if not removed we don't have a proxy yet.
                roots.put(id, new RootProxy(id));
            }
        }
        for (String dead : oldRoots) {
            roots.remove(dead).dispose();
        }
        children.refreshRoots();
    }

    private void bindRootsAdaptor() {
        try {
            HubUIHelper hlp = HubUIHelper.getDefault();
            hlp.bind(
                    ControlAddress.create(hlp.findService(RootManagerService.INSTANCE),
                    RootManagerService.ROOTS), rootsAdaptor);
        } catch (ServiceUnavailableException ex) {
        }
    }

    private void unbindRootsAdaptor() {
        HubUIHelper.getDefault().unbind(rootsAdaptor);
    }

    private class DelegateNode extends AbstractNode {

        private DelegateNode(Children children) {
            super(children);
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }



    }

    private class RootChildren extends Children.Keys<String> {

        private void refreshRoots() {
            setKeys(roots.keySet());
        }

        @Override
        protected Node[] createNodes(String key) {
            return new Node[]{roots.get(key).getNodeDelegate()};
        }
    }

    private class HubListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {

            if (evt.getPropertyName().equals(HubUIHelper.PROP_HUB_CONNECTED)) {
                if (HubUIHelper.getDefault().isConnected()) {
                    bindRootsAdaptor();
                }
//                } else {
//                    unbindRootsAdaptor();
//                }
                
            }

        }
    }
}
