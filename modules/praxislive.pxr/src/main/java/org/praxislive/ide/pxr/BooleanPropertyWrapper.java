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
import org.praxislive.core.types.PBoolean;
import org.openide.nodes.Node;

/**
 *
 */
class BooleanPropertyWrapper extends Node.Property<Boolean> {

    private final BoundArgumentProperty wrapped;

    BooleanPropertyWrapper(BoundArgumentProperty wrapped) {
        super(Boolean.class);
        this.wrapped = wrapped;
    }

    @Override
    public boolean canRead() {
        return wrapped.canRead();
    }

    @Override
    public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
        return PBoolean.from(wrapped.getValue()).orElse(PBoolean.FALSE).value();
    }

    @Override
    public boolean canWrite() {
        return wrapped.canWrite();
    }

    @Override
    public void setValue(Boolean val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        wrapped.setValue(val ? PBoolean.TRUE : PBoolean.FALSE);
    }

    @Override
    public String getName() {
        return wrapped.getName();
    }

    @Override
    public boolean supportsDefaultValue() {
        return wrapped.supportsDefaultValue();
    }

    @Override
    public void restoreDefaultValue() {
        wrapped.restoreDefaultValue();
    }

    @Override
    public String getHtmlDisplayName() {
        return wrapped.getHtmlDisplayName();
    }

    @Override
    public String getDisplayName() {
        return wrapped.getDisplayName();
    }

}
