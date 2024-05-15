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

import java.util.Optional;
import java.util.stream.Stream;
import org.praxislive.core.ComponentAddress;

/**
 * A proxy of a PraxisCORE RootHub.
 */
public interface HubProxy extends Proxy {

    /**
     * Name of roots property. Used in property change events.
     */
    public static final String ROOTS = "roots";

    /**
     * Get the proxy for the root with the given ID.
     *
     * @param id root ID
     * @return root proxy
     */
    public RootProxy getRoot(String id);

    /**
     * Stream of root IDs.
     *
     * @return root IDs
     */
    public Stream<String> roots();

    /**
     * Find the proxy for the component with the given address (if available).
     *
     * @param address component address
     * @return proxy
     */
    public default Optional<ComponentProxy> find(ComponentAddress address) {
        RootProxy root = getRoot(address.rootID());
        if (root == null) {
            return Optional.empty();
        }
        if (address.depth() == 1) {
            return Optional.of(root);
        } else {
            ComponentProxy cmp = root;
            for (int i = 1; i < address.depth(); i++) {
                if (cmp instanceof ContainerProxy) {
                    cmp = ((ContainerProxy) cmp).getChild(address.componentID(i));
                } else {
                    return Optional.empty();
                }
            }
            return Optional.ofNullable(cmp);
        }
    }

}
