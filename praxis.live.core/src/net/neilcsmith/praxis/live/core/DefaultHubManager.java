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
package net.neilcsmith.praxis.live.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.IllegalRootStateException;
import net.neilcsmith.praxis.hub.DefaultHub;
import net.neilcsmith.praxis.hub.TaskServiceImpl;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.core.api.RootLifecycleHandler;
import net.neilcsmith.praxis.live.core.api.Task;
import net.neilcsmith.praxis.script.impl.ScriptServiceImpl;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultHubManager {
    
    private final static Logger LOG = Logger.getLogger(DefaultHubManager.class.getName());

    static enum State {

        Stopped, /*Starting,*/ Running, Stopping
    };
    
    
    
    private final static DefaultHubManager INSTANCE = new DefaultHubManager();
    private DefaultHub hub;
    private ExtensionContainer container;
    private State state;
    private Queue<Task> shutdownTasks;
//    private Task activeTask;
    private boolean markForRestart;
    private RootManagerOverride rootManager;
    private PropertyChangeSupport pcs;

    private DefaultHubManager() {
        state = State.Stopped;
        shutdownTasks = new LinkedList<Task>();
        pcs = new PropertyChangeSupport(this);
    }

    public synchronized void start() {
        if (state == State.Running) {
            LOG.fine("start() called but already running");
            return;
        } else if (state == State.Stopping) {
            LOG.fine("start() called but in process of stopping. markForRestart set to true");
            markForRestart = true;
            return;
        }
        LOG.fine("Starting hub");
        try {
            initHub();
            updateState(State.Running);
        } catch (IllegalRootStateException ex) {
            Exceptions.printStackTrace(ex);
            deinitHub();
            updateState(State.Stopped);
        }
        markForRestart = false;
    }

    public synchronized void stop() {
        if (state == State.Stopped) {
            LOG.fine("stop() called but already stopped");
            return;
        } else if (state == State.Stopping) {
            LOG.fine("stop() called but already stopping. markForRestart set to false");
            markForRestart = false;
            return;
        }
        doShutdown();
    }

    public synchronized void restart() {
        if (state == State.Stopped) {
            start();
            return;
        } else if (state == State.Stopping) {
            markForRestart = true;
            return;
        }
        markForRestart = true;
        doShutdown();
    }
    
    
    State getState() {
        return state;
    }

    void addPropertyChangeListener(PropertyChangeListener pl) {
        pcs.addPropertyChangeListener(pl);
    }
    
    void removePropertyChangeListener(PropertyChangeListener pl) {
        pcs.removePropertyChangeListener(pl);
    }
    
    private void doShutdown() {
        updateState(State.Stopping);
        checkHandlers();
        if (shutdownTasks.isEmpty()) {
            LOG.fine("No shutdown tasks found. Going straight to completeShutdown");
            completeShutdown();
        } else {
            nextShutdownTask();
        }
    }
    
    private void completeShutdown() {
        LOG.fine("completeShutdown()");
        deinitHub();
        updateState(State.Stopped);
        if (markForRestart) {
            LOG.fine("Restarting hub");
            start();
        }
    }
    
    private void nextShutdownTask() {
        Task task = shutdownTasks.poll();
//        activeTask = task;
        if (task != null) {
            LOG.log(Level.FINE, "Executing task {0}", task.getClass());
            Task.State st = task.execute();
            switch (st) {
                case CANCELLED:
                    LOG.log(Level.FINE, "Task cancelled - {0}", task.getClass());
                    cancelShutdown();
                    break;
                case RUNNING:
                    LOG.log(Level.FINE, "Task running - {0}", task.getClass());
                    task.addPropertyChangeListener(new TaskListener(task));
                    break;
                case ERROR:
                    LOG.log(Level.WARNING, "Task error from {0}", task.getClass());
                    // notify error.
                    // fall through
                default:
                    nextShutdownTask();
            }
        } else {
            completeShutdown();
        }
        
    }
    
    private void cancelShutdown() {
        shutdownTasks.clear();
        state = State.Running;
    }

    private void updateState(State state) {
        State old = this.state;
        this.state = state;
        pcs.firePropertyChange(null, old, state);
    }
    
    

    private void initHub() throws IllegalRootStateException {
        Component[] extensions = Utils.findExtensions();
        container = new ExtensionContainer(extensions);
        rootManager = new RootManagerOverride();
        hub = new DefaultHub(
                LookupBridge.getInstance(),
                NbLookupComponentFactory.getInstance(),
                rootManager,
                new ScriptServiceImpl(),
                new TaskServiceImpl(),
                container);
        hub.activate();
    }

    private void deinitHub() {
        container.uninstallExtensions();
        hub.shutdown();
        container = null;
        rootManager = null;
        hub = null;
    }
    
    private void checkHandlers() {
        Set<String> roots = rootManager.getKnownUserRoots();
        LOG.log(Level.FINE, "Looking up handlers for {0}", Arrays.toString(roots.toArray()));
//        for (RootLifecycleHandler handler :
//                Lookup.getDefault().lookupAll(RootLifecycleHandler.class)) {
//            Task task = handler.getDeletionTask(roots);
//            if (task != null) {
//                LOG.log(Level.FINE, "Found task {0} from {1}", new Object[]{task.getClass(), handler.getClass()});
//                shutdownTasks.add(task);
//            }
//        }
        shutdownTasks.addAll(Utils.findRootDeletionTasks(roots));
    }

//    private Component[] findExtensions() {
//        Collection<? extends ExtensionProvider> providers =
//                Lookup.getDefault().lookupAll(ExtensionProvider.class);
//        List<Component> list = new ArrayList<Component>(providers.size());
//        for (ExtensionProvider provider : providers) {
//            list.add(provider.getExtensionComponent());
//        }
//        return list.toArray(new Component[list.size()]);
//        // @TODO add own monitor component???
//    }

    private class TaskListener implements PropertyChangeListener {
        
        private Task task;
        
        TaskListener(Task task) {
            this.task = task;
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            task.removePropertyChangeListener(this);
            if (task.getState() == Task.State.CANCELLED) {
                cancelShutdown();
            } else {
                nextShutdownTask();
            }
        }
    }

    public static DefaultHubManager getInstance() {
        return INSTANCE;
    }
}
