/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
import java.util.HashSet;
import java.util.Set;
import net.neilcsmith.praxis.live.core.api.RootLifecycleHandler;
import net.neilcsmith.praxis.live.core.api.Task;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
@ServiceProvider(service=RootLifecycleHandler.class)
public class RootLifecycleHandlerImpl extends RootLifecycleHandler {

    @Override
    public Task getDeletionTask(String description, Set<String> rootIDs) {
        Set<PXRDataObject> dobs = new HashSet<PXRDataObject>();
        for (String rootID : rootIDs) {
            RootProxy root = RootRegistry.getDefault().getRootByID(rootID);
            if (root instanceof PXRRootProxy) {
                dobs.add( ((PXRRootProxy) root).getSource());
            }
        }
        if (!dobs.isEmpty()) {
//            return SaveTask.createSaveTask(dobs);
            return new DeletionSaveTask(description, dobs);
        } else {
            return null;
        }
    }
   
    
    private static class DeletionSaveTask implements Task, PropertyChangeListener {
        
        private Set<PXRDataObject> dobs;
        private State state;
        private PropertyChangeSupport pcs;
        private SaveTask delegate;
        private String description;
        
        private DeletionSaveTask(String description, Set<PXRDataObject> dobs) {
            this.description = description;
            this.dobs = dobs;
            this.state = State.NEW;
            pcs = new PropertyChangeSupport(this);
        }

        @Override
        public State execute() {
            if (state != State.NEW) {
                throw new IllegalStateException();
            }
            updateState(State.RUNNING);
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(buildDialogMessage(), description);
            Object ret = DialogDisplayer.getDefault().notify(nd);
            if (ret == NotifyDescriptor.YES_OPTION) {
                // save
                delegate = SaveTask.createSaveTask(dobs);
                delegate.addPropertyChangeListener(this);
                return delegate.execute();
            } else if (ret == NotifyDescriptor.NO_OPTION) {
                // don't save - pass completed so operation can continue
                updateState(State.COMPLETED);
                return state;
            } else {
                // pass cancelled back
                updateState(State.CANCELLED);
                return state;
            }
            
        }
        
        private String buildDialogMessage() {
            StringBuilder sb = new StringBuilder("Save changes to");
            for (PXRDataObject dob : dobs) {
                sb.append(" /");
                sb.append(dob.getName());
            }
            sb.append("?");
            return sb.toString();
        }
        
        private void updateState(State state) {
            State old = this.state;
            this.state = state;
            pcs.firePropertyChange(PROP_STATE, old, state);
        }

        @Override
        public State getState() {
            return state;
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
        public boolean cancel() {
            if (delegate != null) {
                return delegate.cancel();
            } else {
                return false;
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (delegate.getState() != this.state) {
                updateState(delegate.getState());
            }
        }
        
    }
    
    
}
