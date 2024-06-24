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
package org.praxislive.ide.pxr;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.openide.nodes.Node;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;

/**
 *
 */
class InputPropertyWrapper extends Node.Property<String> {

    private final PXRComponentProxy cmp;
    private final String id;
    
    InputPropertyWrapper(PXRComponentProxy cmp, String id) {
        super(String.class);
        this.cmp = cmp;
        this.id = id;
        setName(id);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public String getValue() throws IllegalAccessException, InvocationTargetException {
        return "";
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        cmp.send(id, List.of(PString.of(val)));
    }

}