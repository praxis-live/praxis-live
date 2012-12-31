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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.interfaces.RootManagerService;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.gui.ControlBinding.SyncRate;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import net.neilcsmith.praxis.live.util.ArgumentPropertyAdaptor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultRootRegistry extends RootRegistry {

    private final static DefaultRootRegistry INSTANCE = new DefaultRootRegistry();
    private PropertyChangeSupport pcs;
    private final Set<RootProxy> roots;
    private ArgumentPropertyAdaptor.ReadOnly rootsAdaptor;

    private DefaultRootRegistry() {
        roots = new LinkedHashSet<RootProxy>();
        pcs = new PropertyChangeSupport(this);
        PXRHelper.getDefault().addPropertyChangeListener(new HubListener());
        rootsAdaptor = new ArgumentPropertyAdaptor.ReadOnly(this, "roots", false, SyncRate.Medium);
        rootsAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                validateRoots();
            }
        });
        bindRootsAdaptor();
    }

    public synchronized void register(RootProxy root) {
        if (root == null) {
            throw new NullPointerException();
        }
        if (roots.contains(root)) {
            throw new IllegalArgumentException();
        }
        roots.add(root);
        fireRootsChange();
    }

    public synchronized void unregister(RootProxy root) {
        if (roots.remove(root)) {
            fireRootsChange();
        }
        
    }

    private synchronized void unregisterAll() {
        if (roots.isEmpty()) {
            return;
        }
        roots.clear();
        fireRootsChange();
    }

    private synchronized void validateRoots() {
        try {
            PArray rts = PArray.coerce(rootsAdaptor.getValue());
            List<String> ids = new ArrayList<String>(rts.getSize());
            for (Argument id : rts) {
                ids.add(id.toString());
            }
            Iterator<RootProxy> itr = roots.iterator();
            boolean removed = false;
            while (itr.hasNext()) {
                if (!ids.contains(itr.next().getAddress().getRootID())) {
                    itr.remove();
                    removed = true;
                }
            }
            if (removed) {
                fireRootsChange();
            }
        } catch (ArgumentFormatException ex) {
            // @TODO what here?
        }

    }

    private void fireRootsChange() {
        pcs.firePropertyChange(PROP_ROOTS, null, null);
    }

    private void bindRootsAdaptor() {
        try {
            PXRHelper hlp = PXRHelper.getDefault();
            hlp.bind(
                    ControlAddress.create(hlp.findService(RootManagerService.INSTANCE),
                    RootManagerService.ROOTS), rootsAdaptor);
        } catch (ServiceUnavailableException ex) {
        }
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
    public synchronized RootProxy[] getRoots() {
        return roots.toArray(new RootProxy[roots.size()]);
    }

    private class HubListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (PXRHelper.PROP_HUB_CONNECTED.equals(evt.getPropertyName())) {
                boolean connected = PXRHelper.getDefault().isConnected();
                if (connected) {
                    bindRootsAdaptor();
                } else {
                    unregisterAll();
                }
            }
        }
    }




    public static DefaultRootRegistry getDefault() {
        return INSTANCE;
    }

}
