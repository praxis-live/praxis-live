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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.model;

import java.util.Optional;
import java.util.stream.Stream;
import org.praxislive.core.ComponentAddress;

/**
 *
 */
public interface HubProxy extends Proxy {
    
    public static final String ROOTS = "roots";
    
    public RootProxy getRoot(String id);
    
    public Stream<String> roots();
    
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
