package com.revivius.nb.darcula.ui;

import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Paint;
import javax.swing.JComponent;
import javax.swing.JToggleButton;
import javax.swing.plaf.ComponentUI;

/**
 * A minor re-write of DarculaButtonUI to prevent painting background when
 * content area filled property is set to false on button and painting a
 * noticable background if button is selected.
 *
 * Adapted from modified DarculaButtonUI.
 *
 * @author Revivius
 */
public class ContentAreaAwareToggleButtonUI extends ContentAreaAwareButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new ContentAreaAwareToggleButtonUI();
    }

    @Override
    protected Paint getBackgroundPaint(JComponent c) {
        JToggleButton b = (JToggleButton) c;
        if (b.isSelected()) {
            return new GradientPaint(
                    0.0F, 0.0F, getButtonColor1().brighter(),
                    0.0F, c.getHeight(), getButtonColor2().brighter()
            );
        }
        return new GradientPaint(
                0.0F, 0.0F, getButtonColor1(),
                0.0F, c.getHeight(), getButtonColor2()
        );
    }

    @Override
    public void update(Graphics g, JComponent c) {
        super.update(g, c);
    }

}
