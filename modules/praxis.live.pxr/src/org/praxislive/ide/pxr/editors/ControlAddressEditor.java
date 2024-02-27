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
package org.praxislive.ide.pxr.editors;

import java.awt.Component;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.types.PString;
import org.praxislive.ide.properties.EditorSupport;
import org.praxislive.ide.properties.PraxisProperty;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
public class ControlAddressEditor extends EditorSupport 
    implements ExPropertyEditor {
    
    final PraxisProject project;
    
    private PropertyEnv env;
    private final boolean allowEmpty;
    
    ControlAddressEditor(PraxisProperty property, ArgumentInfo info) {
        var p = property.getValue("project");
        if (p instanceof PraxisProject) {
            project = (PraxisProject) p;
        } else {
            project = null;
        }
        allowEmpty = info.properties().getBoolean(ArgumentInfo.KEY_ALLOW_EMPTY, false);
//        property.setValue("canEditAsText", Boolean.FALSE);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        String val = text.trim();
        if (val.isEmpty()) {
            if (allowEmpty) {
                setValue(PString.EMPTY);
            } else {
                throw new IllegalArgumentException("Property doesn't support empty value");
            }
        } else {
            try {
                setValue(ControlAddress.parse(val));
            } catch (ValueFormatException ex) {
                throw new IllegalArgumentException(ex);
            }
        }
    }
    
    private ControlAddress getAddress() {
        try {
            Value arg = (Value) getValue();
            if (arg.isEmpty()) {
                return null;
            } else {
                return ControlAddress.from(arg).orElseThrow();
            }
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getPraxisInitializationString() {
        ControlAddress address = getAddress();
        if (address == null) {
            return "{}";
        } else {
            return address.toString();
        }
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }
    
    @Override
    public boolean supportsCustomEditor() {
        return env != null;
    }

    @Override
    public Component getCustomEditor() {
        return new ControlAddressCustomEditor(this, getAddress(), env);
    }
    

    public String getDisplayName() {
        return "Control Address Editor";
    }
    
    
}
