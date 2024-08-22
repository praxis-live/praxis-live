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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.praxislive.core.Component;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.spi.ExtensionProvider;
import org.praxislive.ide.core.api.AbstractHelperComponent;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Connection;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.PortAddress;
import org.praxislive.core.Value;
import org.praxislive.core.types.PMap;

/**
 *
 */
public class PXRHelper extends AbstractHelperComponent {

    private static final String ALLOWED_COMMANDS = "[array @ ~ file array map cd libraries]";
    private static final String EVAL_COMMAND = "eval --trap-errors --allowed-commands "
            + ALLOWED_COMMANDS + " ";
    private static final String ROOT_SCRIPT = EVAL_COMMAND + """
                                                             {
                                                             cd %1$s
                                                             %2$s
                                                             }
                                                             """;
    private static final String SUB_SCRIPT = EVAL_COMMAND + """
                                                            {
                                                            cd %1$s
                                                            @ %2$s {
                                                            %3$s
                                                            }
                                                            }
                                                            """;

    private PXRHelper() {
    }

    CompletionStage<List<Value>> safeEval(URI workingDir, String script) {
        return execScript(ROOT_SCRIPT.formatted(workingDir, script));
    }

    CompletionStage<List<Value>> safeContextEval(URI workingDir, ComponentAddress address, String script) {
        return execScript(SUB_SCRIPT.formatted(workingDir, address, script));
    }

    CompletionStage<ComponentInfo> createComponentAndGetInfo(ComponentAddress address, ComponentType type) {
        return execScript("@ " + address + " " + type + " { .info }")
                .thenApply(result -> {
                    assert EventQueue.isDispatchThread();
                    return ComponentInfo.from(result.get(0)).orElseThrow();
                });
    }

    CompletionStage<ComponentAddress> createComponent(ComponentAddress address, ComponentType type) {
        return execScript("@ " + address + " " + type)
                .thenApply(result -> address);
    }

    CompletionStage<PMap> componentData(ComponentAddress address) {
        String script;
        if (address.depth() == 1) {
            script = address + ".serialize";
        } else {
            script = "/" + address.rootID() + ".serialize [map subtree " + address + "]";
        }
        return execScript(script)
                .thenApply(r -> PMap.from(r.get(0)).orElseThrow());
    }

    CompletionStage<ComponentInfo> componentInfo(ComponentAddress address) {
        return send(ControlAddress.of(address, "info"), List.of())
                .thenApply(res -> ComponentInfo.from(res.get(0)).orElseThrow());
    }

    CompletionStage<?> removeComponent(ComponentAddress address) {
        return execScript("!@ " + address);
    }

    CompletionStage<Connection> connect(ComponentAddress container,
            Connection connection) {
        return connectionImpl(container, connection, true);
    }

    CompletionStage<Connection> disconnect(ComponentAddress container,
            Connection connection) {
        return connectionImpl(container, connection, false);
    }

    private CompletionStage<Connection> connectionImpl(ComponentAddress container,
            Connection connection,
            boolean connect) {
        PortAddress source = PortAddress.of(
                ComponentAddress.of(container, connection.sourceComponent()),
                connection.sourcePort());
        PortAddress target = PortAddress.of(
                ComponentAddress.of(container, connection.targetComponent()),
                connection.targetPort());
        String script;
        if (connect) {
            script = "~ " + source + " " + target;
        } else {
            script = "!~ " + source + " " + target;
        }
        return execScript(script).thenApply(v -> {
            assert EventQueue.isDispatchThread();
            return connection;
        });
    }

    @ServiceProvider(service = ExtensionProvider.class)
    public static class Provider implements ExtensionProvider {

        @Override
        public Optional<Component> createExtension(Lookup context) {
            return Optional.of(new PXRHelper());
        }
    }
}
