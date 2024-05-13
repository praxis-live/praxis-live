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

import java.beans.PropertyChangeListener;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 * Base interface for proxies of PraxisCORE types.
 */
public interface Proxy extends Lookup.Provider {

    /**
     * Get the node representation of this proxy.
     *
     * @return node
     */
    public Node getNodeDelegate();

    /**
     * Add a property change listener.
     *
     * @param listener propety change listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Remove a property change listener.
     *
     * @param listener propety change listener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener);

}
