/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Linking this work statically or dynamically with other modules is making a
 * combined work based on this work. Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this work give you permission
 * to link this work with independent modules to produce an executable,
 * regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that
 * you also meet, for each linked independent module, the terms and conditions of
 * the license of that module. An independent module is a module which is not
 * derived from or based on this work. If you modify this work, you may extend
 * this exception to your version of the work, but you are not obligated to do so.
 * If you do not wish to do so, delete this exception statement from your version.
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.laf;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class OptionsAwareLabelUI extends MetalLabelUI {

    private static final Color oldHighlighted = new Color(224, 232, 246);
    private final Color fgNormal = MetalLookAndFeel.getBlack();
    private final Color bgNormal = MetalLookAndFeel.getWhite();
    private final Color bgSelected = MetalLookAndFeel.getPrimaryControlShadow();
    private final Color bgHighlighted = bgSelected.darker().darker();
    private final Border normalBorder = new EmptyBorder(6, 8, 6, 8);
    private final Border highlightBorder = new CompoundBorder(
            new CompoundBorder(
            new LineBorder(bgNormal), 
            new BevelBorder(BevelBorder.LOWERED)),
            new EmptyBorder(3, 5, 3, 5));
    private boolean ignoreChanges;

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if (ignoreChanges) {
            super.propertyChange(e);
            return;
        }
        if (!(e.getSource() instanceof JLabel)) {
            super.propertyChange(e);
            return;
        }
        JLabel c = (JLabel) e.getSource();
        checkParent(c);
        if ("background".equals(e.getPropertyName())) {
            ignoreChanges = true;
            Color bgCurrent = c.getBackground();
            if (Color.WHITE.equals(bgCurrent)) {
                c.setBackground(bgNormal);
            } else if (oldHighlighted.equals(bgCurrent)) {
                c.setBackground(bgHighlighted);
            } else if (!bgNormal.equals(bgCurrent)) {
                c.setBackground(bgSelected);
            }
            ignoreChanges = false;
        } else if ("foreground".equals(e.getPropertyName())) {
            ignoreChanges = true;
            if (!fgNormal.equals(c.getForeground())) {
                c.setForeground(fgNormal);
            }
            ignoreChanges = false;
        } else if ("border".equals(e.getPropertyName())) {
            ignoreChanges = true;
            Border current = c.getBorder();
            if (current instanceof EmptyBorder) {
                c.setBorder(normalBorder);
            } else {
                c.setBorder(highlightBorder);
            }
            ignoreChanges = false;
        } else {
            super.propertyChange(e);
        }
    }

    private void checkParent(JComponent c) {
        Component parent = c.getParent();
        if (parent instanceof JPanel) {
            if (!bgNormal.equals(parent.getBackground())) {
                parent.setBackground(bgNormal);
            }
        }
    }

    public static ComponentUI createUI(JComponent c) {
        if (c.getClass().getName().startsWith("org.netbeans.modules.options")) {
            return new OptionsAwareLabelUI();
        } else {
            return MetalLabelUI.createUI(c);
        }
    }
}
