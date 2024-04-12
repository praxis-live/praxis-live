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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.model;

import java.util.List;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentType;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Value;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.properties.PraxisProperty;

/**
 *
 */
public interface ComponentProxy extends Proxy {
    
    public static final String INFO = "info";

    public ComponentAddress getAddress();

    public ComponentType getType();

    public ComponentInfo getInfo();

    public ContainerProxy getParent();
    
    public PraxisProperty<?> getProperty(String id);
    
    public void send(String control, List<Value> args, Callback callback);

}
