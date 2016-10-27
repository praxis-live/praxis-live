package com.revivius.nb.darcula;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JPanel;
import javax.swing.UIManager;

/**
 * Copy paste from o.n.swing.plaf.
 * @author Revivius
 */
public final class ToolbarXP extends JPanel {

    /** Width of grip. */
    private static final int GRIP_WIDTH = 7;
    /** Minimum size. */
    private final Dimension dim;
    /** Maximum size. */
    private final Dimension max;

    public ToolbarXP() {
        dim = new Dimension(GRIP_WIDTH, GRIP_WIDTH);
        max = new Dimension(GRIP_WIDTH, Integer.MAX_VALUE);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = 3;
        for (int i = 4; i < getHeight() - 4; i += 4) {
            //first draw the rectangular highlight below each dot
            g.setColor(UIManager.getColor("controlLtHighlight").brighter()); //NOI18N
            g.fillRect(x + 1, i + 1, 2, 2);
            //Get the shadow color.  We'll paint the darkest dot first,
            //and work our way to the lightest
            Color col = UIManager.getColor("controlShadow").brighter(); //NOI18N
            g.setColor(col);
            //draw the darkest dot
            g.drawLine(x + 1, i + 1, x + 1, i + 1);

            //Get the color components and calculate the amount each component
            //should increase per dot
            int red = col.getRed();
            int green = col.getGreen();
            int blue = col.getBlue();

            //Get the default component background - we start with the dark
            //color, and for each dot, add a percentage of the difference
            //between this and the background color
            Color back = getBackground();
            int rb = back.getRed();
            int gb = back.getGreen();
            int bb = back.getBlue();

            //Get the amount to increment each component for each dot
            int incr = (rb - red) / 5;
            int incg = (gb - green) / 5;
            int incb = (bb - blue) / 5;

            //Increment the colors
            red += incr;
            green += incg;
            blue += incb;
            //Create a slightly lighter color and draw the dot
            col = new Color(red, green, blue);
            g.setColor(col);
            g.drawLine(x + 1, i, x + 1, i);

            //And do it for the next dot, and so on, for all four dots
            red += incr;
            green += incg;
            blue += incb;
            col = new Color(red, green, blue);
            g.setColor(col);
            g.drawLine(x, i + 1, x, i + 1);

            red += incr;
            green += incg;
            blue += incb;
            col = new Color(red, green, blue);
            g.setColor(col);
            g.drawLine(x, i, x, i);
        }
    }

    /**
     * @return minimum size
     */
    @Override
    public Dimension getMinimumSize() {
        return dim;
    }

    @Override
    public Dimension getMaximumSize() {
        return max;
    }

}
