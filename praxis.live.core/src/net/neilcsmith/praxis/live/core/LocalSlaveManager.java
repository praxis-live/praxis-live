/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2014 Neil C Smith.
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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.live.core.api.Task;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.ExternalProcessBuilder;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class LocalSlaveManager {

    private final static Logger LOG = Logger.getLogger(LocalSlaveManager.class.getName());

    private final Map<Integer, Future<Integer>> slaves;

    LocalSlaveManager() {
        slaves = new HashMap<>();
    }

    Task createStartupTask(List<HubSlaveInfo> info) {
        HashSet<Integer> autostart = new HashSet<>();
        for (HubSlaveInfo i : info) {
            if (i.isAutoStart() && "localhost".equals(i.getHost())) {
                autostart.add(i.getPort());
            }
        }
        return new ExecTask(autostart);
    }

    private class ExecTask implements Task {

        private final Set<Integer> autostart;

        private State state = State.NEW;

        private ExecTask(Set<Integer> autostart) {
            this.autostart = autostart;
        }

        @Override
        public State execute() {
            if (autostart.isEmpty() && slaves.isEmpty()) {
                LOG.fine("No slaves running and no autostart processes - returning");
                state = State.COMPLETED;
                return state;
            }

            Set<Integer> working = new HashSet<>(slaves.keySet());
            working.removeAll(autostart);
            // working now contains slaves that need stopping
            for (Integer port : working) {
                LOG.log(Level.FINE, "Removing slave at port : {0}", port);
                Future<Integer> process = slaves.remove(port);
                if (process != null) {
                    LOG.log(Level.FINE, "Cancelling slave at port : {0}", port);
                    process.cancel(true);
                }
            }

            for (Integer port : autostart) {
                Future<Integer> slave = slaves.get(port);
                if (slave != null && !slave.isDone()) {
                    LOG.log(Level.FINE, "Slave already running at port : {0}", port);
                    continue;
                }
                try {
                    LOG.log(Level.FINE, "Starting slave at port : {0}", port);
                    slaves.put(port, startSlaveProcess(port));
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Failed to start slave at port : " + port, ex);
                    state = State.ERROR;
                    return state;
                }
            }

            state = State.COMPLETED;
            return state;
        }

        private Future<Integer> startSlaveProcess(Integer port)
                throws Exception {

            File launcher = HubSettings.getDefault().getLocalSlaveLauncher();
            if (launcher == null || !launcher.exists()) {
                throw new IllegalStateException("No slave launcher");
            }
            String path = launcher.getAbsolutePath();
            LOG.log(Level.FINEST, "Launcher : {0}", path);
            Path userdir = Files.createTempDirectory("praxis_slave");
            LOG.log(Level.FINEST, "Userdir : {0}", userdir);

            ExecutionDescriptor descriptor = new ExecutionDescriptor()
                    /*.frontWindow(true)*/.controllable(true);

            ExternalProcessBuilder processBuilder
                    = new ExternalProcessBuilder(path)
                    .addArgument("--userdir")
                    .addArgument(userdir.toString())
                    .addArgument("--slave")
                    .addArgument("--port")
                    .addArgument(port.toString());
            
            ExecutionService service = ExecutionService.newService(processBuilder,
                    descriptor, "Slave (" + port + ")");
            
            return service.run();

        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        @Override
        public boolean cancel() {
            return false;
        }

    }

}
