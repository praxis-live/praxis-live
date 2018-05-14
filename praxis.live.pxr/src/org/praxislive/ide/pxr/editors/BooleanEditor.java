/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package org.praxislive.ide.pxr.editors;

import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.types.PBoolean;
import org.praxislive.ide.properties.EditorSupport;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@SuppressWarnings("deprecation")
public class BooleanEditor extends EditorSupport {

    @Override
    public void setValue(Object value) {
        try {
            Value val = (Value) value;
            super.setValue(PBoolean.coerce(val));
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(PBoolean.valueOf(text));
        } catch (ValueFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String[] getTags() {
        return new String[]{"true", "false"};
    }

    @Override
    public String getPraxisInitializationString() {
        try {
            return PBoolean.coerce((Value) getValue()).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getDisplayName() {
        return "Boolean Editor";
    }

}
