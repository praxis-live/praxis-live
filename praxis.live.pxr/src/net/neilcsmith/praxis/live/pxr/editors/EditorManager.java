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

import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.core.types.PNumber;
import net.neilcsmith.praxis.core.types.PResource;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class EditorManager {

    public static PraxisPropertyEditor getDefaultEditor(
            PraxisProperty property, ControlInfo info) {

        if (info.getOutputsInfo().length == 1) {
            return getDefaultEditor(property, info.getOutputsInfo()[0]);
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }


    }

    private static PraxisPropertyEditor getDefaultEditor(
            PraxisProperty property, ArgumentInfo info) {
        Class<?> type = info.getType();
        if (PString.class.isAssignableFrom(type)) {
            return findStringEditor(property, info);
        }
        if (PNumber.class.isAssignableFrom(type)) {
            return new NumberEditor(property, info);
        }
        if (PBoolean.class.isAssignableFrom(type)) {
            return new BooleanEditor();
        }
        if (PResource.class.isAssignableFrom(type)) {
            return new ResourceEditor(property, info);
        }
        if (PArray.class.isAssignableFrom(type)) {
            return new ArrayEditor();
        }

        return new ArgumentEditor();

    }

    private static PraxisPropertyEditor findStringEditor(PraxisProperty property,
            ArgumentInfo info) {
        if (info.getProperties().get(PString.KEY_ALLOWED_VALUES) != null) {
            return new EnumEditor(property, info);
        }
        Argument mime = info.getProperties().get(PString.KEY_MIME_TYPE);
        if (mime != null) {
              String mimetype = mime.toString();
              if ("text/x-praxis-java".equals(mimetype) ||
                      "text/x-praxis-script".equals(mimetype)) {
                  return new CodeEditor(property, info, mimetype);
              }
        }
        return new StringEditor();
    }

    public static boolean hasAdditionalEditors(
            PraxisProperty property, ControlInfo info) {
        if (info.getOutputsInfo().length == 1) {
            return hasAdditionalEditors(property, info.getOutputsInfo()[0]);
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }
    }

    private static boolean hasAdditionalEditors(
            PraxisProperty<?> property, ArgumentInfo info) {
        Class<? extends Argument> type = info.getType();
        if (PArray.class.isAssignableFrom(type)) {
            return true;
        }
        return false;
    }

    public static PraxisPropertyEditor[] getAdditionalEditors(
            PraxisProperty property, ControlInfo info) {
        if (info.getOutputsInfo().length == 1) {
            return getAdditionalEditors(property, info.getOutputsInfo()[0]);
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }
    }

    private static PraxisPropertyEditor[] getAdditionalEditors(
            PraxisProperty<?> property, ArgumentInfo info) {
        Class<? extends Argument> type = info.getType();
        if (PArray.class.isAssignableFrom(type)) {
            return new PraxisPropertyEditor[]{
                        new FileListEditor(property, info)
                    };
        }
        return new PraxisPropertyEditor[0];
    }
}
