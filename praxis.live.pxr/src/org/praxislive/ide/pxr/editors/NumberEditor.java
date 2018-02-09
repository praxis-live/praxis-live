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

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PNumber;
import org.praxislive.ide.properties.EditorSupport;
import org.praxislive.ide.properties.PraxisProperty;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@SuppressWarnings("deprecation")
public class NumberEditor extends EditorSupport implements
        ExPropertyEditor, InplaceEditor.Factory {
    
    private final static String EDIT_AS_TEXT = "canEditAsText";

    private final ArgumentInfo info;
    private NumberInplaceEditor inplace;

    private boolean isInteger;
    private List<String> suggested;

    public NumberEditor(PraxisProperty<?> property, ArgumentInfo info) {
        this.info = info;
        init();
    }

    private void init() {
        isInteger = info.getProperties().getBoolean(PNumber.KEY_IS_INTEGER, false);
        if (isInteger) {
            initInt();
        } else {
            initFP();
        }
    }

    private void initFP() {
        PMap props = info.getProperties();
        
        double min = props.getDouble(PNumber.KEY_MINIMUM, PNumber.MIN_VALUE);
        double max = props.getDouble(PNumber.KEY_MAXIMUM, PNumber.MAX_VALUE);
        if (min > (PNumber.MIN_VALUE + 1)
                || max < (PNumber.MAX_VALUE - 1)) {
            double skew = props.getDouble(PNumber.KEY_SKEW, 1);
            inplace = new NumberInplaceEditor(min, max, skew);
        }

    }

    private void initInt() {
        PMap props = info.getProperties();
        Value arg = props.get(ArgumentInfo.KEY_SUGGESTED_VALUES);
        if (arg != null) {
            try {
                PArray arr = PArray.coerce(arg);
                suggested = new ArrayList<>(arr.getSize());
                for (Value val : arr) {
                    suggested.add(val.toString());
                }
            } catch (ValueFormatException ex) {
                // no op
            }
        }
    }

    @Override
    public Object getAttribute(String key) {
        if (suggested != null && EDIT_AS_TEXT.equals(key)) {
            return Boolean.TRUE;
        } else {
            return null;
        }
    }

    @Override
    public String[] getAttributeKeys() {
        return new String[]{EDIT_AS_TEXT};
    }
    
    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(PNumber.valueOf(text));
        } catch (ValueFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String getPraxisInitializationString() {
        try {
            return PNumber.coerce((Value) getValue()).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public String getDisplayName() {
        return "Number Editor";
    }

    @Override
    public boolean supportsCustomEditor() {
        return false;
//        return !isInteger && minimum != null && maximum != null;
    }

    @Override
    public Component getCustomEditor() {
//        return new NumberCustomEditor(this, minimum, maximum);
        return null;
    }

    @Override
    public boolean isPaintable() {
        return inplace != null;
    }

    @Override
    public void paintValue(Graphics g, Rectangle box) {
        double value;
        try {
            value = PNumber.coerce((Value) getValue()).value();
        } catch (Exception ex) {
            value = 0;
        }
        inplace.paintValue(g, box, value, false);
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        if (inplace != null) {
            env.registerInplaceEditorFactory(this);
        }
    }

    @Override
    public InplaceEditor getInplaceEditor() {
        return inplace;
    }

    @Override
    public String[] getTags() {
        if (suggested != null) {
            return suggested.toArray(new String[suggested.size()]);
        } else {
            return null;
        }
    }

}
