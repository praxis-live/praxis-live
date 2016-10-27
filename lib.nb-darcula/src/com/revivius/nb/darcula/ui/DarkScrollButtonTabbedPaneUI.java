/*
 * Copyright 2016 Revivius.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.revivius.nb.darcula.ui;

import com.bulenkov.darcula.ui.DarculaTabbedPaneUI;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import static javax.swing.SwingConstants.EAST;
import static javax.swing.SwingConstants.NORTH;
import static javax.swing.SwingConstants.SOUTH;
import static javax.swing.SwingConstants.WEST;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 * Overrides {@link #createScrollButton(int)} to provide a button which is more suitable to dark look and feel.
 * @author Revivius
 */
public class DarkScrollButtonTabbedPaneUI extends DarculaTabbedPaneUI {

    public static ComponentUI createUI(JComponent c) {
        return new DarkScrollButtonTabbedPaneUI();
    }

    @Override
    protected JButton createScrollButton(int direction) {
        if (direction != SOUTH && direction != NORTH && direction != EAST &&
                                  direction != WEST) {
            throw new IllegalArgumentException("Direction must be one of: " +
                                               "SOUTH, NORTH, EAST or WEST");
        }
        return new ScrollableTabButton(direction);
    }

    private class ScrollableTabButton extends BasicArrowButton implements UIResource,
                                                                            SwingConstants {
        public ScrollableTabButton(int direction) {
            super(direction,
                    new Color(60, 63, 65),
                    new Color(54, 54, 54),
                    new Color(169, 169, 169),
                    new Color(54, 54, 54));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 0, 0, 1),
                    BorderFactory.createLineBorder(new Color(169, 169, 169)))
            );
        }
    }
    
}
