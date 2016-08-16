package com.revivius.nb.darcula.ui;

import com.bulenkov.darcula.DarculaUIUtil;
import com.bulenkov.darcula.ui.DarculaButtonUI;
import com.bulenkov.iconloader.util.GraphicsConfig;
import com.bulenkov.iconloader.util.Gray;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.Border;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.UIResource;

/**
 * A minor re-write of DarculaButtonPainter to reduce border insets.
 * 
 * Mostly copy paste from DarculaButtonPainter.
 *
 * @author Revivius
 */
public class ReducedInsetsDarculaButtonPainter implements Border, UIResource {

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        boolean square = DarculaButtonUI.isSquare(c);
        if (c.hasFocus()) {
            DarculaUIUtil.paintFocusRing(g2d, 2, 2, width - 3, height - 3);
        } else {
            GraphicsConfig config = new GraphicsConfig(g);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
            g2d.setPaint(new GradientPaint(
                    width / 2, y, Gray._80.withAlpha(90),
                    width / 2, height, Gray._90.withAlpha(90)));

            g2d.setPaint(Gray._100.withAlpha(180));
            
            int arc = 5;
            if (square) {
                arc = 3;
            }
            g.drawRoundRect(x + 1, y + 1, width - 2, height - 2, arc, arc);

            config.restore();
        }
    }

    @Override
    public Insets getBorderInsets(Component c) {
        if (DarculaButtonUI.isSquare(c)) {
            return new InsetsUIResource(2, 0, 2, 0);
        }
        return new InsetsUIResource(4, 6, 4, 6);
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
