/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.laf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLabelUI;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class OptionsAwareLabelUI extends MetalLabelUI {

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
            if (Color.WHITE.equals(c.getBackground())) {
                c.setBackground(Color.BLACK);
            } else if (!Color.BLACK.equals(c.getBackground())) {
                c.setBackground(MetalLookAndFeel.getFocusColor());
            }
            ignoreChanges = false;
        } else if ("foreground".equals(e.getPropertyName())) {
            ignoreChanges = true;
            Color fg = MetalLookAndFeel.getBlack();
            if (!fg.equals(c.getForeground())) {
                c.setForeground(fg);
            }
            ignoreChanges = false;
        } else if ("border".equals(e.getPropertyName())) {
            ignoreChanges = true;
            c.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            ignoreChanges = false;
        } else {
            super.propertyChange(e);
        }
    }

    private void checkParent(JComponent c) {
        Component parent = c.getParent();
        if (parent instanceof JPanel) {
            if (!Color.BLACK.equals(parent.getBackground())) {
                parent.setBackground(Color.BLACK);
//                parent.repaint();
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
