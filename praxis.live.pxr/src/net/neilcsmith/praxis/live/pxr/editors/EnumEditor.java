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

import net.neilcsmith.praxis.live.pxr.SyntaxUtils;
import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class EnumEditor extends PropertyEditorSupport implements PraxisPropertyEditor {
    
    private List<String> tags;

    public EnumEditor(PraxisProperty property, ArgumentInfo info) {
        tags = new ArrayList<String>();
        parseTagsToList(info, tags);
    }

    private void parseTagsToList(ArgumentInfo info, List<String> list) {
        Argument arg = info.getProperties().get(PString.KEY_ALLOWED_VALUES);
        if (arg == null) {
            return;
        }
        try {
            PArray arr = PArray.coerce(arg);
            for (Argument val : arr) {
                list.add(val.toString());
            }
        } catch (ArgumentFormatException ex) {
            // no op, just return
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        setValue(PString.valueOf(text));
    }



    @Override
    public String[] getTags() {
        return tags.toArray(new String[tags.size()]);
    }



    @Override
    public String getPraxisInitializationString() {
        return SyntaxUtils.escape(getValue().toString());
    }

    @Override
    public String getDisplayName() {
        return "Enum Editor (default)";
    }

}
