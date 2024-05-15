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
package org.praxislive.ide.core.api;

/**
 * An type capable of syncing with a source of data, such as a proxy to a
 * PraxisCORE component. Code interested in making sure the data is current
 * should add a unique key. A syncable without keys may stop syncing until a key
 * is added.
 */
public interface Syncable {

    /**
     * Add a unique key to this syncing in order to register an interest in this
     * object being in sync with its upstream data source.
     *
     * @param key unique key
     */
    public void addKey(Object key);

    /**
     * Remove a previously added key to unregister an interest in this object
     * being in sync.
     *
     * @param key unique key
     */
    public void removeKey(Object key);

}
