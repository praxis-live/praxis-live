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
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.Component;
import java.beans.PropertyEditorSupport;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.types.PMap;
import net.neilcsmith.praxis.core.types.PNumber;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class NumberEditor extends PropertyEditorSupport implements 
        PraxisPropertyEditor { //, ExPropertyEditor, InplaceEditor.Factory {

    private ArgumentInfo info;
    private NumberInplaceEditor inplace;

    private PNumber minimum;
    private PNumber maximum;
    private boolean isInteger;

    public NumberEditor(PraxisProperty<?> property, ArgumentInfo info) {
        this.info = info;
        checkBounds();
    }

    private void checkBounds() {
        PMap props = info.getProperties();
        Argument minProp = props.get(PNumber.KEY_MINIMUM);
        Argument maxProp = props.get(PNumber.KEY_MAXIMUM);
        if (minProp != null && maxProp != null) {
            try {
                minimum = PNumber.coerce(minProp);
                maximum = PNumber.coerce(maxProp);
            } catch (Exception ex) {
                minimum = maximum = null;
            }
        }
        isInteger = props.getBoolean(PNumber.KEY_IS_INTEGER, false);
        
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(PNumber.valueOf(text));
        } catch (ArgumentFormatException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public String getPraxisInitializationString() {
        try {
            return PNumber.coerce((Argument) getValue()).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return "Number Editor";
    }

    @Override
    public boolean supportsCustomEditor() {
        return !isInteger && minimum != null && maximum != null;
    }

    @Override
    public Component getCustomEditor() {
        return new NumberCustomEditor(this, minimum, maximum);
    }



//
//    @Override
//    public void attachEnv(PropertyEnv env) {
//        env.registerInplaceEditorFactory(this);
//    }
//
//    @Override
//    public InplaceEditor getInplaceEditor() {
//        if (inplace == null) {
//            inplace = new NumberInplaceEditor(info);
//        }
//        return inplace;
//    }
}
