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
package org.praxislive.ide.pxr;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.services.RootManagerService;
import org.praxislive.core.services.ServiceUnavailableException;
import org.praxislive.core.types.PArray;
import org.praxislive.gui.ControlBinding.SyncRate;
import org.praxislive.ide.pxr.api.RootRegistry;
import org.praxislive.ide.util.ArgumentPropertyAdaptor;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXRRootRegistry extends RootRegistry {

    private final static PXRRootRegistry INSTANCE = new PXRRootRegistry();
    private PropertyChangeSupport pcs;
    private final Set<PXRRootProxy> roots;
    private ArgumentPropertyAdaptor.ReadOnly rootsAdaptor;

    private PXRRootRegistry() {
        roots = new LinkedHashSet<PXRRootProxy>();
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

    public synchronized void register(PXRRootProxy root) {
        if (root == null) {
            throw new NullPointerException();
        }
        if (roots.contains(root)) {
            throw new IllegalArgumentException();
        }
        roots.add(root);
        fireRootsChange();
    }

    public synchronized void unregister(PXRRootProxy root) {
        if (roots.remove(root)) {
            root.dispose();
            fireRootsChange();
        }
        
    }

    private synchronized void unregisterAll() {
        if (roots.isEmpty()) {
            return;
        }
        for (PXRRootProxy root : roots) {
            root.dispose();
        }
        roots.clear();
        fireRootsChange();
    }

    private synchronized void validateRoots() {
        try {
            PArray rts = PArray.coerce(rootsAdaptor.getValue());
            List<String> ids = new ArrayList<String>(rts.getSize());
            for (Value id : rts) {
                ids.add(id.toString());
            }
            Iterator<PXRRootProxy> itr = roots.iterator();
            boolean removed = false;
            while (itr.hasNext()) {
                PXRRootProxy root = itr.next();
                if (!ids.contains(root.getAddress().getRootID())) {
                    itr.remove();
                    root.dispose();
                    removed = true;
                }
            }
            if (removed) {
                fireRootsChange();
            }
        } catch (ValueFormatException ex) {
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
                    ControlAddress.create(hlp.findService(RootManagerService.class),
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
    public synchronized PXRRootProxy[] getRoots() {
        return roots.toArray(new PXRRootProxy[roots.size()]);
    }
    
    public PXRRootProxy getRootByID(String id) {
        for (PXRRootProxy root : getRoots()) {
            if (root.getAddress().getRootID().equals(id)) {
                return root;
            }
        }
        return null;
    }
    
    public PXRRootProxy findRootForFile(FileObject file) {
        for (PXRRootProxy root : getRoots()) {
            if (root.getSourceFile().equals(file)) {
                return root;
            }
        }
        return null;
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




    public static PXRRootRegistry getDefault() {
        return INSTANCE;
    }

}
