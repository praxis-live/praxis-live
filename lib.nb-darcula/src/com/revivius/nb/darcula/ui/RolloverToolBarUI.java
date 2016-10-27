package com.revivius.nb.darcula.ui;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicToolBarUI;
import org.netbeans.swing.plaf.LFCustoms;

/**
 * A ToolBarUI that installs a ChangeListener on buttons to enable rollover for
 * JButtons and JToggleButtons.
 *
 * @author Revivius
 */
public class RolloverToolBarUI extends BasicToolBarUI {

    private static final String LISTENER_KEY = "ToolbarUI.ListenerKey";

    private static final ChangeListener LISTENER = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            AbstractButton b = (AbstractButton) e.getSource();
            boolean rollover = b.getModel().isRollover();

            b.setContentAreaFilled(rollover || b.getModel().isSelected());
            b.setBorderPainted(rollover);
        }
    };

    // #24
    // o.openide.awt.ToolbarWithOverflow
    private static final PropertyChangeListener BORDER_UPDATER = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            JComponent src = (JComponent) evt.getSource();
            Border border = src.getBorder();
            if (border instanceof LineBorder) {
                LineBorder lb = (LineBorder) border;
                if (lb.getThickness() == 1 && Color.LIGHT_GRAY.equals(lb.getLineColor())) {
                    src.setBorder(BorderFactory.createLineBorder(UIManager.getColor(LFCustoms.SCROLLPANE_BORDER_COLOR)));
                }
            }
        }
    };

    public static ComponentUI createUI(JComponent c) {
        return new RolloverToolBarUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        // #24
        if ("overflowToolbar".equals(toolBar.getName())) {
            toolBar.addPropertyChangeListener("border", BORDER_UPDATER);
        }
    }

    @Override
    protected void uninstallDefaults() {
        super.uninstallDefaults();
        // #24
        if ("overflowToolbar".equals(toolBar.getName())) {
            toolBar.removePropertyChangeListener("border", BORDER_UPDATER);
        }
    }

    @Override
    protected void setBorderToNonRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            configureButton(b);
        }
    }

    @Override
    protected void setBorderToRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;
            configureButton(b);
        }
    }

    @Override
    protected void setBorderToNormal(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton) c;

            b.setBorderPainted(true);
            b.setContentAreaFilled(true);
            b.setRolloverEnabled(false);
            uninstallListener(b);
        }
    }

    private void configureButton(AbstractButton b) {
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setRolloverEnabled(true);
        installListener(b);
    }

    private void installListener(AbstractButton b) {
        Object o = b.getClientProperty(LISTENER_KEY);
        if (o == null) {
            b.addChangeListener(LISTENER);
            LISTENER.stateChanged(new ChangeEvent(b));
        }
    }

    private void uninstallListener(AbstractButton b) {
        Object o = b.getClientProperty(LISTENER_KEY);
        if (o != null) {
            b.addChangeListener(LISTENER);
        }
    }

}
