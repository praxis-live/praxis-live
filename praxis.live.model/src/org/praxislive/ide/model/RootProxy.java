/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public interface RootProxy extends ComponentProxy {

    public static Optional<RootProxy> find(String id) {
        Collection<? extends Registry> regs = Lookup.getDefault().lookupAll(Registry.class);
        return regs.stream()
                .map(reg -> reg.find(id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public interface Registry {

        public Optional<RootProxy> find(String id);

        public List<RootProxy> findAll();

    }

}
