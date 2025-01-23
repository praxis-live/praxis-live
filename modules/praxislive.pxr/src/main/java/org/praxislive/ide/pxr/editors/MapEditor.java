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

package org.praxislive.ide.pxr.editors;

import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.properties.EditorSupport;
import org.praxislive.ide.properties.PraxisProperty;
import java.awt.Component;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 */
public class MapEditor extends EditorSupport implements ExPropertyEditor {
    
    private String text;
    private PropertyEnv env;
    
    
    public MapEditor(PraxisProperty<?> property, ArgumentInfo info) {
    }

    public String getDisplayName() {
        return "Map Editor";
    }

    @Override
    public void setValue(Object value) {
        try {
            super.setValue(PMap.from((Value) value).get());
            text = null;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            super.setValue(PMap.parse(text));
            this.text = text;
        } catch (ValueFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String getAsText() {
        if (text == null) {
            text = getValue().toString();
        }
        return text;
    }
    

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    @Override
    public Component getCustomEditor() {
        return new MapCustomEditor(this, env);
    }



}
