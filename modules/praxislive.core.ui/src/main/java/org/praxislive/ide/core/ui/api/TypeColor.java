/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.core.ui.api;

import java.awt.Color;
import javax.swing.UIManager;

/**
 * Standard colours that can be assigned to various entities to differentiate
 * them by type (eg. port type or component type).
 *
 */
public enum TypeColor {

    Red("TypeColor.Red", 350f),
    Orange("TypeColor.Orange", 30f),
    Yellow("TypeColor.Yellow", 50f),
    Green("TypeColor.Green", 90f),
    Cyan("TypeColor.Cyan", 170f),
    Blue("TypeColor.Blue", 195f),
    Purple("TypeColor.Purple", 285f),
    Magenta("TypeColor.Magenta", 315f);

    private final Color selection;
    private final Color shade;
    private final Color text;

    private TypeColor(String key, float defaultHue) {
        Color uiShade = UIManager.getColor(key);
        Color uiSelection = UIManager.getColor(key + ".selected");
        Color uiText = UIManager.getColor(key + ".text");
        boolean isDark = UIUtils.isDarkTheme();
        if (uiShade != null && uiSelection != null) {
            this.shade = uiShade;
            this.selection = uiSelection;
            this.text = uiText != null ? uiText : isDark ? uiShade : uiSelection;
        } else {
            this.shade = hsb(defaultHue, 50, 90);
            this.selection = hsb(defaultHue, 95, 85);
            this.text = isDark ? this.shade : this.selection;
        }
    }

    /**
     * The standard colour to use for graphical elements.
     *
     * @return standard colour
     */
    public Color shade() {
        return shade;
    }

    /**
     * The selection colour variant to use for graphical elements.
     *
     * @return selection colour
     */
    public Color selection() {
        return selection;
    }

    /**
     * The colour variant to use for text against the standard background
     * colour.
     *
     * @return text colour
     */
    public Color text() {
        return text;
    }

    private static Color hsb(float hue, float saturation, float brightness) {
        return new Color(Color.HSBtoRGB(hue / 360, saturation / 100, brightness / 100));
    }

}
