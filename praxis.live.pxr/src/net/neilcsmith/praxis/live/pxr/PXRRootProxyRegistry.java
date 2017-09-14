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
package net.neilcsmith.praxis.live.pxr;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.neilcsmith.praxis.live.model.RootProxy;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service = RootProxy.Registry.class)
public class PXRRootProxyRegistry implements RootProxy.Registry {

    @Override
    public Optional<RootProxy> find(String id) {
        return Optional.ofNullable(PXRRootRegistry.getDefault().getRootByID(id));
    }

    @Override
    public List<RootProxy> findAll() {
        return Arrays.asList(PXRRootRegistry.getDefault().getRoots());
    }
    
}
