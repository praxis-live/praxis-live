/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.core.types.PMap;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import org.openide.awt.HtmlRenderer;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class StringEditor extends PraxisPropertyEditorSupport
        implements ExPropertyEditor {

    private PropertyEnv env;
    private PraxisProperty<?> property;
    private boolean emptyIsDefault;
    private List<String> suggested;

    public StringEditor(PraxisProperty<?> property, ArgumentInfo info) {
        if (property == null) {
            throw new NullPointerException();
        }
        this.property = property;
        emptyIsDefault = info.getProperties().getBoolean(ArgumentInfo.KEY_EMPTY_IS_DEFAULT, false);
        Argument arg = info.getProperties().get(ArgumentInfo.KEY_SUGGESTED_VALUES);
        if (arg != null) {
            try {
                PArray arr = PArray.coerce(arg);
                suggested = new ArrayList<String>(arr.getSize());
                for (Argument val : arr) {
                    suggested.add(val.toString());
                }
                property.setValue("canEditAsText", Boolean.TRUE);
            } catch (ArgumentFormatException ex) {
                // no op
            }
        }
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

    @Override
    public String getDisplayName() {
        return "String Editor";
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
        return new StringCustomEditor(this, env);
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
