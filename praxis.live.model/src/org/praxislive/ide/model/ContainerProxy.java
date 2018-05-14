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
package org.praxislive.ide.model;

import org.praxislive.ide.core.api.Callback;
import org.praxislive.core.ComponentType;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public interface ContainerProxy extends ComponentProxy {

    public void addChild(String id, ComponentType type, Callback callback)
            throws ProxyException;

    public void removeChild(String id, Callback callback) throws ProxyException;
    
    public ComponentProxy getChild(String id);

    public String[] getChildIDs();

    public void connect(Connection connection, Callback callback) throws ProxyException;

    public void disconnect(Connection connection, Callback callback) throws ProxyException;

    public Connection[] getConnections();

}
