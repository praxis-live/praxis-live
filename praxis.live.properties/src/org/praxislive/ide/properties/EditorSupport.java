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
package org.praxislive.ide.properties;

import java.beans.PropertyEditorSupport;
import org.praxislive.core.types.PString;

/**
 *
 */
public class EditorSupport extends PropertyEditorSupport
        implements PraxisProperty.Editor {

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(PString.of(text));
    }

    @Override
    public String getPraxisInitializationString() {
        return SyntaxUtils.escape(getValue().toString());
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public String[] getAttributeKeys() {
        return new String[0];
    }

    @Override
    public void reset() {
        // no op
    }

}
