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
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.live.core.api.Task;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
abstract class SaveTask implements Task {

    private final static Logger LOG = Logger.getLogger(SaveTask.class.getName());
    private final static Map<PXRDataObject, Single> activeTasks = new HashMap<PXRDataObject, Single>();
    private final static RequestProcessor RP = new RequestProcessor();
    private PropertyChangeSupport pcs;
    private State state;

    private SaveTask() {
        pcs = new PropertyChangeSupport(this);
        state = State.NEW;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    private void firePropertyChange(String property, Object oldValue, Object newValue) {
        pcs.firePropertyChange(property, oldValue, newValue);
    }

    void updateState(State state) {
        State old = this.state;
        this.state = state;
        firePropertyChange(PROP_STATE, old, state);
    }

    @Override
    public State getState() {
        return state;
    }

    static SaveTask createSaveTask(Set<PXRDataObject> dobs) {
        if (dobs == null || dobs.isEmpty()) {
            return null;
        }
        if (dobs.size() == 1) {
            PXRDataObject dob = dobs.iterator().next();
            Single active = activeTasks.get(dob);
            if (active != null) {
                return new Compound(Collections.singleton(dob));
            } else {
                return new Single(dob);
            }
        } else {
            return new Compound(new HashSet<PXRDataObject>(dobs));
        }
    }

    private static class Single extends SaveTask implements PropertyChangeListener, ActionListener {

        private PXRDataObject dob;
        private PXRRootProxy root;
        private List<ComponentProxy> components;
        private ProgressHandle ph;

        private Single(PXRDataObject dob) {
            this.dob = dob;

        }

        @Override
        public State execute() {
            assert EventQueue.isDispatchThread();
            if (getState() != State.NEW) {
                throw new IllegalStateException();
            }
            RootProxy r = RootRegistry.getDefault().findRootForFile(dob.getPrimaryFile());
            if (r instanceof PXRRootProxy) {
                activeTasks.put(dob, this);
                updateState(State.RUNNING);
                LOG.log(Level.FINE, "Starting sync for save on {0}", r.getAddress());       
                root = (PXRRootProxy) r;
                ph = ProgressHandleFactory.createHandle("Saving " + root.getAddress(), this);
                ph.setInitialDelay(0);
                ph.start();
                ph.progress("Syncing.");
                syncComponents();
                RP.schedule(new Runnable() {

                    @Override
                    public void run() {
                        EventQueue.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                doSave();
                            }
                        });
                    }
                }, 1000, TimeUnit.MILLISECONDS);
            } else {
                LOG.log(Level.FINE, "Unable to find RootProxy for {0}", dob.getPrimaryFile().getPath());
                updateState(State.ERROR);
            }
            return getState();
        }

        private void syncComponents() {
            components = new ArrayList<ComponentProxy>();
            addComponentAndChildren(components, root);
            for (ComponentProxy component : components) {
                LOG.log(Level.FINE, "Adding SaveTask listener to {0}", component.getAddress());
                component.addPropertyChangeListener(this);
            }
        }

        private void addComponentAndChildren(List<ComponentProxy> components, ComponentProxy component) {
            components.add(component);
            if (component instanceof ContainerProxy) {
                ContainerProxy container = (ContainerProxy) component;
                for (String id : container.getChildIDs()) {
                    addComponentAndChildren(components, container.getChild(id));
                }
            }
        }

        private void unsyncComponents() {
            if (components == null) {
                return;
            }
            for (ComponentProxy component : components) {
                LOG.log(Level.FINE, "Removing SaveTask listener from {0}", component.getAddress());
                component.removePropertyChangeListener(this);
            }
            components.clear();
            components = null;
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            doSave();
        }

        private void doSave() {
            if (getState() != State.RUNNING) {
                return;
            }
            ph.progress("Saving file.");
            StringBuilder sb = new StringBuilder();
            try {
                PXRWriter.write(root, sb);
                final String contents = sb.toString();
                RP.execute(new Runnable() {

                    @Override
                    public void run() {
                        Writer writer = null;
                        boolean success = false;
                        try {
                            FileObject file = dob.getPrimaryFile();
                            writer = new OutputStreamWriter(file.getOutputStream());
                            writer.append(contents);
                            success = true;
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        } finally {
                            if (writer != null) {
                                try {
                                    writer.close();
                                } catch (IOException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        }
                        final boolean complete = success;
                        EventQueue.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                updateState(complete ? State.COMPLETED : State.ERROR);
                            }
                        });
                    }
                });
            } catch (IOException ex) {
                // should be impossible with StringBuilder!
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        void updateState(State state) {
            if (state != State.RUNNING) {
                activeTasks.remove(dob);
                ph.finish();
                ph = null;
            }
            super.updateState(state);
        }
        
        

        @Override
        public boolean cancel() {
            if (getState() == State.RUNNING) {
                unsyncComponents();
                updateState(State.CANCELLED);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            // no op, used to force sync
        }
    }

    private static class Compound extends SaveTask implements PropertyChangeListener {

        private Set<Single> childTasks;
        private Set<PXRDataObject> dobs;

        private Compound(Set<PXRDataObject> dobs) {
            this.dobs = dobs;
            childTasks = new HashSet<Single>(dobs.size());
        }

        @Override
        public State execute() {
            assert EventQueue.isDispatchThread();
            if (getState() != State.NEW) {
                throw new IllegalStateException();
            }
            updateState(State.RUNNING);

            for (PXRDataObject dob : dobs.toArray(new PXRDataObject[0])) {
                // iterate array copy of set as initChildTask() might remove elements
                initChildTask(dob);
            }
            
            return getState();
        }

        private void initChildTask(PXRDataObject dob) {
            Single child = activeTasks.get(dob);
            if (child == null) {
                child = new Single(dob);
            }
            childTasks.add(child);
            child.addPropertyChangeListener(this);
            if (child.getState() != State.RUNNING) {
                child.execute();
            }
        }

        @Override
        public boolean cancel() {
            for (Single child : childTasks) {
                child.cancel();
            }
            return true;
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            Single task = (Single) pce.getSource();
            if (this.getState() != State.RUNNING) {
                task.removePropertyChangeListener(this);
                return;
            }
            switch (task.getState()) {
                case ERROR:
                case CANCELLED:
                    childTasks.clear();
                    updateState(task.getState());
                    break;
                case COMPLETED:
                    childTasks.remove(task);
                    if (childTasks.isEmpty()) {
                        updateState(State.COMPLETED);
                    }
                    break;
                default:
                // nothing?
            }
        }
    }
}
