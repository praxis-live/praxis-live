/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.praxislive.core.Value;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.properties.EditorSupport;
import org.praxislive.ide.properties.PraxisProperty;
import org.openide.awt.HtmlRenderer;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@SuppressWarnings("deprecation")
public class StringEditor extends EditorSupport
        implements ExPropertyEditor {

    private final static String EMPTY_DEFAULT_STRING = "[default]";
    private final static String EDIT_AS_TEXT = "canEditAsText";

    private PropertyEnv env;
    private boolean emptyIsDefault;
    private boolean rewriteDefaultTag;
    private boolean limitToTags;
    private List<String> tags;

    public StringEditor(PraxisProperty<?> property, ArgumentInfo info) {
        if (property == null) {
            throw new NullPointerException();
        }
        PMap props = info.getProperties();
        emptyIsDefault = props.getBoolean(ArgumentInfo.KEY_EMPTY_IS_DEFAULT, false);
        Value tagArray = props.get(ArgumentInfo.KEY_ALLOWED_VALUES);
        if (tagArray != null) {
            limitToTags = true;
        } else {
            tagArray = props.get(ArgumentInfo.KEY_SUGGESTED_VALUES);
        }
        if (tagArray != null) {
            createTagList(tagArray);
        }
    }

    private void createTagList(Value tagArray) {
        try {
            PArray arr = PArray.coerce(tagArray);
            tags = new ArrayList<>(arr.getSize());
            for (Value val : arr) {
                tags.add(val.toString());
            }
            if (emptyIsDefault && tags.contains("")
                    && !tags.contains(EMPTY_DEFAULT_STRING)) {
                tags.remove("");
                tags.add(0, EMPTY_DEFAULT_STRING);
                rewriteDefaultTag = true;
            }
        } catch (ValueFormatException ex) {
            // no op
        }
    }

    @Override
    public Object getAttribute(String key) {
        if (tags != null && !limitToTags && EDIT_AS_TEXT.equals(key)) {
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
    public boolean isPaintable() {
        if (emptyIsDefault && getAsText().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void paintValue(Graphics g, Rectangle r) {
        Font font = g.getFont();
        FontMetrics fm = g.getFontMetrics(font);
        HtmlRenderer.renderHTML("<font color=\"!textInactiveText\">[default]</font>",
                g, r.x, r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent(),
                r.width, r.height, g.getFont(), g.getColor(),
                HtmlRenderer.STYLE_TRUNCATE, true);
    }

    public String getDisplayName() {
        return "String Editor";
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public boolean supportsCustomEditor() {
        return env != null && !limitToTags;
    }

    @Override
    public Component getCustomEditor() {
        return new StringCustomEditor(this, env);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (rewriteDefaultTag && EMPTY_DEFAULT_STRING.equals(text)) {
            text = "";
        }
        super.setAsText(text);
    }

    @Override
    public String[] getTags() {
        if (tags != null) {
            return tags.toArray(new String[tags.size()]);
        } else {
            return null;
        }
    }
}
