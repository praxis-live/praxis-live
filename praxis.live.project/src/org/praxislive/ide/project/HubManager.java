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
package org.praxislive.ide.project;

import java.awt.EventQueue;
import org.praxislive.ide.core.api.Logging;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.Timer;
import org.praxislive.hub.Hub;
import org.praxislive.ide.core.api.Task;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.praxislive.code.CodeCompilerService;
import org.praxislive.core.MainThread;
import org.praxislive.core.services.LogLevel;
import org.praxislive.core.services.LogService;
import org.praxislive.core.services.SystemManagerService;
import org.praxislive.hub.net.NetworkCoreFactory;
import org.praxislive.ide.core.api.AbstractTask;
import org.praxislive.ide.core.api.ExtensionContainer;
import org.praxislive.ide.project.spi.RootLifecycleHandler;
import org.praxislive.ide.core.api.SerialTasks;

/**
 *
 */
class HubManager {

    private final static Logger LOG = Logger.getLogger(HubManager.class.getName());

    static enum State {

        Stopped, Starting, Running, Stopping
    };

    private final DefaultPraxisProject project;
    private final HubProxyImpl proxy;
    private final Lookup lookup;
    private final InstanceContent lookupContent;

    private Hub hub;
    private ExtensionContainer container;
    private State state;
    private ServicesOverride servicesOverride;

    HubManager(DefaultPraxisProject project) {
        this.project = project;
        this.proxy = new HubProxyImpl(project);
        state = State.Stopped;
        lookupContent = new InstanceContent();
        lookupContent.add(proxy);
        lookup = new AbstractLookup(lookupContent);
    }

    Task createStartupTask() {
        return new StartUpTask(List.of(new InitHubTask()));
    }

    Task createShutdownTask() {
        var roots = servicesOverride.getKnownUserRoots();
        var description = "Shutdown"; // @TODO bundle
        var tasks = project.getLookup().lookupAll(RootLifecycleHandler.class).stream()
                .flatMap(handler -> handler.getDeletionTask(description, roots).stream())
                .collect(Collectors.toCollection(ArrayList::new));
        tasks.add(new DeinitHubTask());
        return new ShutDownTask(tasks);
    }

    State getState() {
        return state;
    }

    Lookup getLookup() {
        return lookup;
    }

    private void initHub() throws Exception {
        if (hub != null) {
            throw new IllegalStateException();
        }
        container = ExtensionContainer.create(project.getLookup());
        container.extensions().forEach(lookupContent::add);
        servicesOverride = new ServicesOverride(project);
        Logging log = Logging.create(project.getLookup());
        LogLevel logLevel = log.getLogLevel();

        var core = NetworkCoreFactory.builder()
                .childLauncher(new ChildLauncherImpl(project))
                .exposeServices(List.of(
                        CodeCompilerService.class,
                        LogService.class,
                        SystemManagerService.class
                ))
                .build();

        var fakeMain = new FakeMain();

        hub = Hub.builder()
                .setCoreRootFactory(core)
                .addExtension(servicesOverride)
                .addExtension(log)
                .addExtension(container)
                .extendLookup(logLevel)
                .extendLookup(fakeMain)
                .build();
        hub.start();
    }

    private void deinitHub() {
        container.extensions().forEach(lookupContent::remove);
        hub.shutdown();
        try {
            hub.await();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        container = null;
        servicesOverride = null;
        hub = null;
    }

    private class StartUpTask extends SerialTasks {

        public StartUpTask(List<Task> tasks) {
            super(tasks);
        }

        @Override
        public Optional<String> description() {
            return Optional.of("Initializing hub.");
        }

        @Override
        protected void beforeExecute() {
            if (HubManager.this.state != HubManager.State.Stopped) {
                throw new IllegalStateException();
            }
            HubManager.this.state = HubManager.State.Starting;
        }

        @Override
        protected void afterExecute() {
            if (hub != null && hub.isAlive()) {
                HubManager.this.state = HubManager.State.Running;
            } else {
                HubManager.this.state = HubManager.State.Stopped;
            }
        }

    }

    private class InitHubTask extends AbstractTask {

        private ProjectHelper helper;
        private int count;
        private Timer timer;

        @Override
        protected void handleExecute() throws Exception {
            initHub();
            helper = lookup.lookup(ProjectHelper.class);
            if (helper == null) {
                updateState(State.ERROR);
            } else {
                timer = new Timer(50, e -> checkHelper());
                timer.start();
                updateState(State.RUNNING);
            }
        }

        private void checkHelper() {
            if (helper.isConnected()) {
                timer.stop();
                updateState(State.COMPLETED);
            } else if (count++ > 10) {
                timer.stop();
                updateState(State.ERROR);
            }
        }

    }

    private class ShutDownTask extends SerialTasks {

        public ShutDownTask(List<Task> tasks) {
            super(tasks);
        }

        @Override
        protected void beforeExecute() {
            if (HubManager.this.state != HubManager.State.Running) {
                throw new IllegalStateException();
            }
            HubManager.this.state = HubManager.State.Stopping;
        }

        @Override
        protected void afterExecute() {
            if (getState() == State.COMPLETED) {
                HubManager.this.state = HubManager.State.Stopped;
            }
        }
    }

    private class DeinitHubTask extends AbstractTask {

        @Override
        protected void handleExecute() throws Exception {
            deinitHub();
            updateState(Task.State.COMPLETED);
        }

    }

    private static class FakeMain implements MainThread {


        @Override
        public void runLater(Runnable task) {
            // @TODO warn on first use? 
            EventQueue.invokeLater(task);
        }

        @Override
        public boolean isMainThread() {
            return EventQueue.isDispatchThread();
        }
        

    }

}
