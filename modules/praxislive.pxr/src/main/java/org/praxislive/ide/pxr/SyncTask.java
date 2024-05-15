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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.ide.core.api.Syncable;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.core.api.AbstractTask;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.util.RequestProcessor;

/**
 *
 */
public class SyncTask extends AbstractTask {

    private final static Logger LOG = Logger.getLogger(SyncTask.class.getName());
    private final static RequestProcessor RP = new RequestProcessor();
    
    public final static long DEFAULT_SYNC_MS = 500;

    private final Set<ComponentProxy> components;
    private final List<ComponentProxy> syncing;
    private final long milliseconds;

    private ProgressHandle ph;

    public SyncTask(Set<ComponentProxy> components) {
        this(components, DEFAULT_SYNC_MS);
    }
    
     public SyncTask(Set<ComponentProxy> components, long milliseconds) {
        this.components = components;
        this.syncing = new ArrayList<>();
        this.milliseconds = milliseconds < 10 ? 10 : milliseconds > 2000 ? 2000 : milliseconds;
    }

    @Override
    protected void handleExecute() throws Exception {
        LOG.log(Level.FINE, "Starting sync on {0}", components);
        ph = ProgressHandle.createHandle("Syncing");
        ph.setInitialDelay(0);
        ph.start();
        syncComponents();
        RP.schedule(new Runnable() {

                @Override
                public void run() {
                    EventQueue.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }, milliseconds, TimeUnit.MILLISECONDS);
    }

    
    private void finish() {
        unsyncComponents();
        ph.finish();
        ph = null;
        updateState(State.COMPLETED);
    }
    
    private void syncComponents() {
        components.stream().forEach(this::addComponentAndChildren);
        syncing.stream().forEach(cmp -> {
            Syncable sync = cmp.getLookup().lookup(Syncable.class);
                if (sync != null) {
                    LOG.log(Level.FINE, "Adding sync to {0}", cmp.getAddress());
                    sync.addKey(this);
                }
        });
    }

     private void addComponentAndChildren(ComponentProxy component) {
            syncing.add(component);
            if (component instanceof ContainerProxy) {
                ContainerProxy container = (ContainerProxy) component;
//                for (String id : container.getChildIDs()) {
//                    addComponentAndChildren(container.getChild(id));
//                }
                container.children().forEachOrdered(id -> 
                        addComponentAndChildren(container.getChild(id))
                );
            }
        }
    
    private void unsyncComponents() {
        syncing.stream().forEach(cmp -> {
            Syncable sync = cmp.getLookup().lookup(Syncable.class);
                if (sync != null) {
                    LOG.log(Level.FINE, "Removing sync from {0}", cmp.getAddress());
                    sync.removeKey(this);
                }
        });
        syncing.clear();
    }

}
