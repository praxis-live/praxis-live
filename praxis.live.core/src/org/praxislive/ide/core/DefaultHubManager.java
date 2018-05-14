/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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
package org.praxislive.ide.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.Component;
import org.praxislive.hub.Hub;
import org.praxislive.hub.net.MasterFactory;
import org.praxislive.ide.core.api.LogHandler;
import org.praxislive.ide.core.api.Task;
import org.praxislive.logging.LogLevel;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultHubManager {

    private final static Logger LOG = Logger.getLogger(DefaultHubManager.class.getName());

    static enum State {

        Stopped, Starting, Running, Stopping
    };

    private final static DefaultHubManager INSTANCE = new DefaultHubManager();

    private final LocalSlaveManager localSlaves;
    private final Queue<Task> startupTasks;
    private final Queue<Task> shutdownTasks;
    private final PropertyChangeSupport pcs;

    private boolean distributed;
    private List<HubSlaveInfo> slaves;
    private Hub hub;
    private ExtensionContainer container;
    private State state;
    private boolean markForRestart;
    private RootManagerOverride rootManager;

    private DefaultHubManager() {
        state = State.Stopped;
        localSlaves = new LocalSlaveManager();
        startupTasks = new LinkedList<>();
        shutdownTasks = new LinkedList<>();
        pcs = new PropertyChangeSupport(this);
    }

    public synchronized void start() {
        switch (state) {
            case Stopped:
                doStartup();
                break;
            case Running:
                LOG.fine("start() called but already running");
                return;
            case Starting:
                LOG.fine("start() called but already starting");
                return;
            case Stopping:
                LOG.fine("start() called but in process of stopping. markForRestart set to true");
                markForRestart = true;
                return;
        }
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
        markForRestart = false;
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

    private void doStartup() {
        updateState(State.Starting);
        distributed = HubSettings.getDefault().isDistributedHub();
        if (distributed) {
            slaves = HubSettings.getDefault().getSlaveInfo();
        } else {
            slaves = Collections.emptyList();
        }
        initStartupTasks();
        if (startupTasks.isEmpty()) {
            LOG.fine("No startup tasks found. Going straight to completeStartup");
            completeStartup();
        } else {
            nextStartupTask();
        }
    }

    private void completeStartup() {
        if (state != State.Starting) {
            LOG.fine("Unexpected state in completeStartup() - stopping");
            updateState(State.Stopped);
            return;
        }
        LOG.fine("completeStartup()");
        try {
            initHub();
            updateState(State.Running);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            deinitHub();
            updateState(State.Stopped);
        }
        markForRestart = false;
    }

    private void nextStartupTask() {
        Task task = startupTasks.poll();
//        activeTask = task;
        if (task != null) {
            LOG.log(Level.FINE, "Executing task {0}", task.getClass());
            Task.State st = task.execute();
            switch (st) {
                case CANCELLED:
                    LOG.log(Level.FINE, "Task cancelled - {0}", task.getClass());
                    cancelStartup();
                    break;
                case RUNNING:
                    LOG.log(Level.FINE, "Task running - {0}", task.getClass());
                    task.addPropertyChangeListener(new TaskListener(task, true));
                    break;
                case ERROR:
                    LOG.log(Level.WARNING, "Task error from {0}", task.getClass());
                // notify error.
                // fall through
                default:
                    nextStartupTask();
            }
        } else {
            completeStartup();
        }

    }

    private void cancelStartup() {
        startupTasks.clear();
        updateState(State.Stopped);
    }

    private void doShutdown() {
        updateState(State.Stopping);
        initShutdownTasks();
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
                    task.addPropertyChangeListener(new TaskListener(task, false));
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
        updateState(State.Running);
    }

    private void updateState(State state) {
        State old = this.state;
        this.state = state;
        pcs.firePropertyChange(null, old, state);
    }

    private void initHub() throws Exception {
        Component[] extensions = Utils.findExtensions();
        container = new ExtensionContainer(extensions);
        rootManager = new RootManagerOverride();
        List<LogHandler> logHandlers = Utils.findLogHandlers();
        Logging log = new Logging(logHandlers);
        LogLevel logLevel = Utils.findLogLevel(logHandlers);
        Hub.Builder builder = Hub.builder();
        if (distributed) {
            builder.setCoreRootFactory(new MasterFactory(slaves));
        }
        builder
                .addExtension(rootManager)
                .addExtension(log)
                .addExtension(container)
                .extendLookup(logLevel);
        hub = builder.build();
        hub.start();
    }

    private void deinitHub() {
        container.uninstallExtensions();
        hub.shutdown();
        try {
            hub.await();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        container = null;
        rootManager = null;
        hub = null;
    }

    private void initStartupTasks() {
        startupTasks.add(localSlaves.createStartupTask(slaves));
    }

    private void initShutdownTasks() {
        Set<String> roots = rootManager.getKnownUserRoots();
        LOG.log(Level.FINE, "Looking up handlers for {0}", Arrays.toString(roots.toArray()));
        String description = markForRestart ? "Hub Restart" : "Hub Shutdown";
        shutdownTasks.addAll(Utils.findRootDeletionTasks(description, roots));

    }

    private class TaskListener implements PropertyChangeListener {

        private final boolean startup;
        private final Task task;

        TaskListener(Task task, boolean startup) {
            this.task = task;
            this.startup = startup;
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            task.removePropertyChangeListener(this);
            if (startup) {
                if (task.getState() == Task.State.CANCELLED) {
                    cancelStartup();
                } else {
                    nextStartupTask();
                }
            } else {
                if (task.getState() == Task.State.CANCELLED) {
                    cancelShutdown();
                } else {
                    nextShutdownTask();
                }
            }

        }
    }

    public static DefaultHubManager getInstance() {
        return INSTANCE;
    }
}
