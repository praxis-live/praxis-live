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
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Value;
import org.praxislive.ide.properties.PraxisProperty;

/**
 * A proxy of a PraxisCORE component.
 */
public interface ComponentProxy extends Proxy {

    /**
     * Name of info property. Used in property change events.
     */
    public static final String INFO = "info";

    /**
     * Address of component.
     *
     * @return component address
     */
    public ComponentAddress getAddress();

    /**
     * Type of component.
     *
     * @return component type
     */
    public ComponentType getType();

    /**
     * Info for component.
     *
     * @return component info
     */
    public ComponentInfo getInfo();

    /**
     * Parent container.
     *
     * @return parent
     */
    public ContainerProxy getParent();

    /**
     * Access the component property with the given ID.
     *
     * @param id property ID
     * @return property
     */
    public PraxisProperty<?> getProperty(String id);

    /**
     * Send a call to the specified control on the underlying component.
     *
     * @param control control ID
     * @param args call arguments
     * @return completion stage for result
     */
    public CompletionStage<List<Value>> send(String control, List<Value> args);

}
