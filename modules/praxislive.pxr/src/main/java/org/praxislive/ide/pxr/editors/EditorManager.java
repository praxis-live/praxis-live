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

import org.praxislive.core.Value;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PResource;
import org.praxislive.core.types.PString;
import org.praxislive.ide.properties.PraxisProperty;

/**
 *
 */
public class EditorManager {

    public static PraxisProperty.Editor getDefaultEditor(
            PraxisProperty property, ControlInfo info) {

        if (info.outputs().size() == 1) {
            return getDefaultEditor(property, info.outputs().get(0));
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }

    }

    private static PraxisProperty.Editor getDefaultEditor(
            PraxisProperty property, ArgumentInfo info) {
        Class<?> type = Value.Type.fromName(info.argumentType()).get().asClass();
        if (PString.class.isAssignableFrom(type)) {
//            return findStringEditor(property, info);
            return new StringEditor(property, info);
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
        if (ControlAddress.class.isAssignableFrom(type)) {
            return new ControlAddressEditor(property, info);
        }
        if (PMap.class.isAssignableFrom(type)) {
            return new MapEditor(property, info);
        }

        return new ArgumentEditor(property, info);

    }

//    private static PraxisProperty.Editor findStringEditor(PraxisProperty property,
//            ArgumentInfo info) {
//        if (info.getProperties().get(PString.KEY_ALLOWED_VALUES) != null) {
//            return new EnumEditor(property, info);
//        }
////        Value mime = info.getProperties().get(PString.KEY_MIME_TYPE);
////        if (mime != null) {
////            String mimetype = mime.toString();
//////              if ("text/x-praxis-java".equals(mimetype) ||
//////                      "text/x-praxis-script".equals(mimetype)) {
//////            return new CodeEditor(property, info, mimetype);
//////              }
////            return new MimeTextEditor(property, info, mimetype);
////        }
//        return new StringEditor(property, info);
//    }
    public static boolean hasAdditionalEditors(
            PraxisProperty property, ControlInfo info) {
        if (info.outputs().size() == 1) {
            return hasAdditionalEditors(property, info.outputs().get(0));
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }
    }

    private static boolean hasAdditionalEditors(
            PraxisProperty<?> property, ArgumentInfo info) {
        Class<? extends Value> type = Value.Type.fromName(info.argumentType()).get().asClass();
        if (PArray.class.isAssignableFrom(type)) {
            return true;
        } else if (PMap.class.isAssignableFrom(type)) {
            return true;
        } else if (type.equals(Value.class)) {
            return true;
        }
        return false;
    }

    public static PraxisProperty.Editor[] getAdditionalEditors(
            PraxisProperty property, ControlInfo info) {
        if (info.outputs().size() == 1) {
            return getAdditionalEditors(property, info.outputs().get(0));
        } else {
            throw new UnsupportedOperationException(
                    "EditorManager cannot currently handle properties with multiple arguments.");
        }
    }

    private static PraxisProperty.Editor[] getAdditionalEditors(
            PraxisProperty<?> property, ArgumentInfo info) {
        Class<? extends Value> type = Value.Type.fromName(info.argumentType()).get().asClass();
        if (PArray.class.isAssignableFrom(type)) {
            return new PraxisProperty.Editor[]{
                new FileListEditor(property, info),
                new ArgumentEditor(property, info)
            };
        } else if (PMap.class.isAssignableFrom(type)) {
            return new PraxisProperty.Editor[]{
                new ArgumentEditor(property, info)
            };
        } else if (type.equals(Value.class)) {
            return new PraxisProperty.Editor[]{
                new ResourceEditor(property, info),
                new FileListEditor(property, info)
            };
        }
        return new PraxisProperty.Editor[0];
    }
}
