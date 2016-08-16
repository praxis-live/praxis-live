package com.revivius.nb.darcula.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLabelUI;
import org.netbeans.swing.plaf.LFCustoms;

/**
 * https://praxisintermedia.wordpress.com/2011/09/29/the-dark-arts-of-netbeans-hackery/
 *
 * @author Revivius
 */
public class OptionsAwareLabelUI extends MetalLabelUI {

    private static final String OPTIONS_PANEL_NAME = "org.netbeans.modules.options.OptionsPanel";

    private static boolean bgReplaced = false;

    public static ComponentUI createUI(JComponent c) {
        if (c.getClass().getName().startsWith(OPTIONS_PANEL_NAME)) {
            return new OptionsAwareLabelUI();
        }
        return MetalLabelUI.createUI(c);
    }

    private static final Color oldHighlighted = new Color(224, 232, 246);

    private final Color fgNormal = UIManager.getColor("textText");
    private final Color bgNormal = UIManager.getColor("List.background");
    private final Color bgSelected = UIManager.getColor("List.selectionBackground");
    private final Color bgHighlighted = new Color(13, 41, 62);
    private final Border normalBorder = new EmptyBorder(6, 8, 6, 8);
    private final Border highlightBorder = new CompoundBorder(
            new LineBorder(bgNormal),
            new EmptyBorder(4, 7, 4, 7)
    );
    private boolean ignoreChanges;

    @Override
    public void update(Graphics g, JComponent c) {
        super.update(g, c);

        if (bgReplaced) {
            return;
        }

        // In NB 8.1 CategoryButtons are in a JScrollPane
        Container parent = SwingUtilities.getAncestorOfClass(JScrollPane.class, c);
        if (parent == null) {
            parent = SwingUtilities.getAncestorOfClass(JPanel.class, c);
        }
        // In NB 8.0 CategoryButtons are in a JPanel
        if (parent != null) {
            parent = parent.getParent();
        }
        if (parent != null && (parent instanceof JPanel)) {
            JPanel panel = (JPanel) parent;
            replaceBg(panel);
            panel.setBorder(BorderFactory.createMatteBorder(
                    0, 0, 1, 0, UIManager.getColor(LFCustoms.SCROLLPANE_BORDER_COLOR)));
            bgReplaced = true;
        }
    }

    private void replaceBg(JComponent component) {
        component.setBackground(bgNormal);
        if (component instanceof JScrollPane) {
            JScrollPane sc = (JScrollPane) component;
            sc.getViewport().setBackground(bgNormal);
            sc.getViewport().getView().setBackground(bgNormal);
        }
        Component[] components = component.getComponents();
        for (Component c : components) {
            if (c instanceof JPanel || c instanceof JScrollPane) {
                replaceBg((JComponent) c);
            }
        }
    }

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

}
