package com.revivius.nb.darcula.ui;

import com.bulenkov.iconloader.util.GraphicsConfig;
import com.bulenkov.iconloader.util.GraphicsUtil;
import com.bulenkov.iconloader.util.SystemInfo;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import sun.swing.SwingUtilities2;

/**
 * A minor re-write of DarculaButtonUI to prevent painting background when
 * content area filled property is set to false on button.
 *
 * Mostly copy paste from DarculaButtonUI.
 *
 * @author Revivius
 */
public class ContentAreaAwareButtonUI extends BasicButtonUI {

    public static ComponentUI createUI(JComponent c) {
        return new ContentAreaAwareButtonUI();
    }

    public static boolean isSquare(Component c) {
        if (c instanceof JButton) {
            JButton b = (JButton) c;
            return "square".equals(b.getClientProperty("JButton.buttonType"));
        }
        return false;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Border border = c.getBorder();
        GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
        boolean square = isSquare(c);
        Graphics2D g2d = (Graphics2D) g;
        AbstractButton b = (AbstractButton) c;
        if ((c.isEnabled()) && (border != null)) {
            //if (!square) {
            g2d.setPaint(getBackgroundPaint(c));
            //}

            if (b.isContentAreaFilled()) {
                int arc = 5;
                if (square) {
                    arc = 3;
                }

                g.fillRoundRect(1, 1, c.getWidth() - 2, c.getHeight() - 2, arc, arc);
            }
        }
        super.paint(g, c);
        config.restore();
    }

    protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
        AbstractButton button = (AbstractButton) c;
        ButtonModel model = button.getModel();
        Color fg = button.getForeground();
        if (((fg instanceof UIResource)) && ((button instanceof JButton)) && (((JButton) button).isDefaultButton())) {
            Color selectedFg = UIManager.getColor("Button.darcula.selectedButtonForeground");
            if (selectedFg != null) {
                fg = selectedFg;
            }
        }
        g.setColor(fg);

        FontMetrics metrics = SwingUtilities2.getFontMetrics(c, g);
        int mnemonicIndex = button.getDisplayedMnemonicIndex();
        if (model.isEnabled()) {
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text, mnemonicIndex, textRect.x
                    + getTextShiftOffset(), textRect.y + metrics
                    .getAscent() + getTextShiftOffset());
        } else {
            g.setColor(UIManager.getColor("Button.disabledText"));
            SwingUtilities2.drawStringUnderlineCharAt(c, g, text, -1, textRect.x
                    + getTextShiftOffset(), textRect.y + metrics
                    .getAscent() + getTextShiftOffset());
        }
    }
    

    protected Paint getBackgroundPaint(JComponent c) {
        JButton b = (JButton) c;
        if (b.isDefaultButton()) {
            return new GradientPaint(
                    0.0F, 0.0F, getSelectedButtonColor1(),
                    0.0F, c.getHeight(), getSelectedButtonColor2()
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
        if (c instanceof JButton) {
            JButton b = (JButton) c;
            if (b.isDefaultButton() && !SystemInfo.isMac && !c.getFont().isBold()) {
                c.setFont(c.getFont().deriveFont(1));
            }
        }
    }

    protected Color getButtonColor1() {
        return UIManager.getColor("Button.darcula.color1");
    }

    protected Color getButtonColor2() {
        return UIManager.getColor("Button.darcula.color2");
    }

    protected Color getSelectedButtonColor1() {
        return UIManager.getColor("Button.darcula.selection.color1");
    }

    protected Color getSelectedButtonColor2() {
        return UIManager.getColor("Button.darcula.selection.color2");
    }

}
