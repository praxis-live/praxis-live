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

import java.beans.PropertyEditorSupport;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class BooleanEditor extends PropertyEditorSupport implements PraxisPropertyEditor {

    @Override
    public void setValue(Object value) {
        try {
            Argument val = (Argument) value;
            super.setValue(PBoolean.coerce(val));
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }



    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        try {
            setValue(PBoolean.valueOf(text));
        } catch (ArgumentFormatException ex) {
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
            return PBoolean.coerce((Argument)getValue()).toString();
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return "Boolean Editor";
    }

}
