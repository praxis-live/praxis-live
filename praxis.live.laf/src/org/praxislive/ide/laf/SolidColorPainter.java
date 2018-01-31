
package org.praxislive.ide.laf;

import java.awt.Color;
import java.awt.Graphics2D;
import javax.swing.Painter;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class SolidColorPainter implements Painter<Object> {
    private final Color color;

     SolidColorPainter(Color color) {
        this.color = color;
    }

    @Override
    public void paint(Graphics2D g, Object object, int width, int height) {
        g.setColor(color);
        g.fillRect(0, 0, width, height);
    }
    
}
