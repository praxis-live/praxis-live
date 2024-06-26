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
package org.praxislive.ide.model;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import org.praxislive.core.ComponentType;
import org.praxislive.core.Connection;
import org.praxislive.core.protocols.ContainerProtocol;

/**
 * A proxy of a PraxisCORE container.
 */
public interface ContainerProxy extends ComponentProxy {

    /**
     * Name of children property. Used in property change events.
     */
    public static final String CHILDREN = ContainerProtocol.CHILDREN;

    /**
     * Name of connections property. Used in property change events.
     */
    public static final String CONNECTIONS = ContainerProtocol.CONNECTIONS;

    /**
     * Name of supported types property. Used in property change events.
     */
    public static final String SUPPORTED_TYPES = ContainerProtocol.SUPPORTED_TYPES;

    /**
     * Add a child component to the underlying container.
     *
     * @param id child ID
     * @param type component type
     * @return completion stage of added child
     */
    public CompletionStage<? extends ComponentProxy> addChild(String id, ComponentType type);

    /**
     * Remove a child from the underlying container.
     *
     * @param id child ID
     * @return completion stage
     */
    public CompletionStage<?> removeChild(String id);

    /**
     * Get the proxy for the given child.
     *
     * @param id child ID
     * @return
     */
    public ComponentProxy getChild(String id);

    /**
     * Stream of all child IDs.
     *
     * @return childs IDs
     */
    public Stream<String> children();

    /**
     * Create a connection between two child component ports.
     *
     * @param connection connection description
     * @return completion stage of added connection
     */
    public CompletionStage<Connection> connect(Connection connection);

    /**
     * Break a connection between two child component ports.
     *
     * @param connection connection description
     * @return completion stage
     */
    public CompletionStage<?> disconnect(Connection connection);

    /**
     * Stream of connections.
     *
     * @return connection
     */
    public Stream<Connection> connections();

    /**
     * List of supported child types.
     *
     * @return supported child types
     */
    public List<ComponentType> supportedTypes();

}
