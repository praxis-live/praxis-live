/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.project;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.api.extexecution.ExecutionDescriptor;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.base.Environment;
import org.netbeans.api.extexecution.base.ProcessBuilder;
import org.netbeans.api.extexecution.base.input.InputProcessor;
import org.netbeans.api.extexecution.base.input.InputProcessors;
import org.netbeans.api.extexecution.base.input.LineProcessor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PString;
import org.praxislive.hub.net.ChildLauncher;
import org.praxislive.ide.core.embedder.CORE;

/**
 *
 */
class ChildLauncherImpl implements ChildLauncher {

    private static final Map<DefaultPraxisProject, InputOutput> IO_CACHE
            = new ConcurrentHashMap<>();
    private static final String LISTENING_STATUS = "Listening at : ";

    private final DefaultPraxisProject project;

    ChildLauncherImpl(DefaultPraxisProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public Info launch(List<String> javaOptions, List<String> arguments)
            throws Exception {
        String name = project.getProjectDirectory().getName();
        File launcher = CORE.launcherFile().toFile();
        CompletableFuture<SocketAddress> socketFuture = new CompletableFuture<>();
        StopAction stopAction = new StopAction(project);
        IO_CACHE.entrySet().removeIf(e -> e.getValue().isClosed());
        InputOutput io = IO_CACHE.get(project);
        if (io != null) {
            io.closeInputOutput();
        }
        io = IOProvider.getDefault().getIO(name, true,
                new Action[]{stopAction},
                null);
        IO_CACHE.put(project, io);
        ExecutionDescriptor desc = new ExecutionDescriptor()
                .frontWindow(true)
                .inputVisible(true)
                .inputOutput(io)
                .outProcessorFactory((InputProcessor defaultProcessor) -> {
                    return InputProcessors.proxy(
                            InputProcessors.bridge(new PortLineProcessor(socketFuture)),
                            defaultProcessor);
                })
                .preExecution(() -> EventQueue.invokeLater(() -> stopAction.setEnabled(true)))
                .postExecution(() -> EventQueue.invokeLater(() -> stopAction.setEnabled(false)));
        ProcessBuilder pb = ProcessBuilder.getLocal();
        pb.setExecutable(launcher.getCanonicalPath());
        pb.setArguments(List.of("--port", "auto", "--interactive"));
        Environment env = pb.getEnvironment();
        env.setVariable("JAVA_HOME", System.getProperty("java.home"));
        env.setVariable("JAVA_OPTS", javaOptions.stream()
                .map(PString::of)
                .collect(PArray.collector())
                .toString());
        CompletableFuture<Process> processFuture = new CompletableFuture<>();
        ExecutionService exec = ExecutionService.newService(() -> {
            var process = pb.call();
            processFuture.complete(process);
            return process;
        }, desc, name);
        exec.run();
        return new Info(processFuture.get(30, TimeUnit.SECONDS),
                socketFuture.get(30, TimeUnit.SECONDS));
    }

    private static SocketAddress parseListeningLine(String line) {
        if (line.startsWith(LISTENING_STATUS)) {
            try {
                int port = Integer.parseInt(line.substring(LISTENING_STATUS.length()).trim());
                return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
            } catch (Exception ex) {
                throw new IllegalArgumentException(ex);
            }
        }
        throw new IllegalArgumentException();
    }

    private static class PortLineProcessor implements LineProcessor {

        private final CompletableFuture<SocketAddress> socketFuture;

        private PortLineProcessor(CompletableFuture<SocketAddress> socketFuture) {
            this.socketFuture = socketFuture;
        }

        @Override
        public void processLine(String line) {
            if (socketFuture.isDone()) {
                return;
            }
            if (line.startsWith(LISTENING_STATUS)) {
                try {
                    socketFuture.complete(parseListeningLine(line));
                } catch (Exception ex) {
                    socketFuture.completeExceptionally(ex);
                }
            }
        }

        @Override
        public void reset() {
        }

        @Override
        public void close() {
            if (!socketFuture.isDone()) {
                socketFuture.cancel(true);
            }
        }

    }

    @Messages({
        "TXT_StopExecution=Stop project"
    })
    private static class StopAction extends AbstractAction {

        private final DefaultPraxisProject project;

        private StopAction(DefaultPraxisProject project) {
            super(Bundle.TXT_StopExecution(),
                    ImageUtilities.loadImageIcon("org/praxislive/ide/project/resources/stop.png",
                            true));
            this.project = project;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            project.clean();
        }

    }

}
