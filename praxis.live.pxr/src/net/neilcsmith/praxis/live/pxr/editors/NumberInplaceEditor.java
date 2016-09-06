/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.types.PNumber;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class NumberInplaceEditor extends JComponent implements InplaceEditor {

    private static final Logger LOG = Logger.getLogger(NumberInplaceEditor.class.getName());
    private static final Color ACTIVE_COLOR = Color.WHITE;
    private static final Color INACTIVE_COLOR = Color.GRAY;
    private static final DecimalFormat FORMATTER = new DecimalFormat("####0.0####");
    private static final long IGNORE_CLICK_TIME = 500 * 1000000;
    private PropertyEditor propertyEditor;
    private PropertyModel propertyModel;
    private final List<ActionListener> listeners;
    private JTextField textField;
    private Object initialValue;
    private PNumber currentValue;
    private final double min;
    private final double max;
    private final double skew;

    NumberInplaceEditor(double min, double max, double skew) {
        this.min = min;
        this.max = max;
        this.skew = skew < 0.125 ? 0.125 : skew > 8 ? 8 : skew;
        listeners = new ArrayList<>();
        initThis();
        initComponents();

    }

    private void initThis() {
        setFocusable(true);
        setLayout(new GridLayout());
        MouseHandler handler = new MouseHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    private void initComponents() {
        textField = new JTextField();
        textField.setVisible(false);
        textField.setBorder(new EmptyBorder(0,0,0,0));
        textField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                fireActionEvent(true);
            }
        });
        add(textField);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (textField.isVisible()) {
            return;
        }
        Rectangle bounds = new Rectangle(3, 0, getWidth() - 3, getHeight());
        paintValue(g, bounds, currentValue.value(), true);
    }

    void paintValue(Graphics g, Rectangle box, double value, boolean highlight) {
        Color c = g.getColor();
        String stringValue = FORMATTER.format(value);
        double delta = (value - min) / (max - min);
        delta = Math.pow(delta, 1.0/skew);
        int x = (int) (delta * (box.width - 1));
        x += box.x;
        FontMetrics fm = g.getFontMetrics();
        if (!highlight) {
            g.setColor(INACTIVE_COLOR);
            g.drawLine(x, box.y, x, box.height);
            g.setColor(c);
            g.drawString(stringValue, box.x, box.y
                    + (box.height - fm.getHeight()) / 2 + fm.getAscent());
        } else {
            g.setColor(INACTIVE_COLOR);
            g.drawString(stringValue, box.x, box.y
                    + (box.height - fm.getHeight()) / 2 + fm.getAscent());
            g.setColor(ACTIVE_COLOR);
            g.drawLine(x, box.y, x, box.height);
            g.setColor(c);

        }

    }

    @Override
    public void connect(PropertyEditor pe, PropertyEnv env) {
        this.propertyEditor = pe;
        initialValue = pe.getValue();
        textField.setVisible(false);
        AWTEvent event = EventQueue.getCurrentEvent();
        LOG.log(Level.FINE, "Invoking Event: {0}", event);
        if (event instanceof KeyEvent) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    focusTextField();
                }
            });
        }
        reset();
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void clear() {
        propertyEditor = null;
        propertyModel = null;
    }

    @Override
    public void setValue(Object o) {
        try {
            currentValue = PNumber.coerce((Argument) o);
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Exception in setValue()", ex);
            if (currentValue == null) {
                currentValue = PNumber.valueOf(0);
            }
        }
        if (textField.isVisible()) {
            textField.setText(currentValue.toString());
        }
    }

    @Override
    public Object getValue() {
        if (textField.isVisible()) {
            try {
                return PNumber.valueOf(textField.getText());
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Exception in getValue()", ex);
            }
        }
        return currentValue;
    }

    @Override
    public boolean supportsTextEntry() {
        return true;
    }

    @Override
    public void reset() {
        LOG.fine("Reset Called");
        setValue(initialValue);
    }

    @Override
    public void addActionListener(ActionListener al) {
        listeners.add(al);
    }

    @Override
    public void removeActionListener(ActionListener al) {
        listeners.remove(al);
    }

    private void fireActionEvent(boolean success) {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, success ? COMMAND_SUCCESS : COMMAND_FAILURE);
        for (ActionListener l : listeners.toArray(new ActionListener[0])) {
            l.actionPerformed(ev);
        }
    }

    @Override
    public KeyStroke[] getKeyStrokes() {
        return new KeyStroke[0];
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        return propertyEditor;
    }

    @Override
    public PropertyModel getPropertyModel() {
        return propertyModel;
    }

    @Override
    public void setPropertyModel(PropertyModel pm) {
        this.propertyModel = pm;
    }

    @Override
    public boolean isKnownComponent(Component c) {
        return c == this || c == textField;
    }

    private void updateValue(double value) {
        currentValue = PNumber.valueOf(value);
        try {
            propertyModel.setValue(currentValue);
        } catch (InvocationTargetException ex) {
        }
        repaint();
    }

    private void focusTextField() {
        textField.setText(currentValue.toString());
        textField.setVisible(true);
        textField.selectAll();
        textField.requestFocusInWindow();

    }

    private class MouseHandler extends MouseAdapter {

        private int startX;
        private int valueX;
        private boolean dragging;
        private long clickTime;

        @Override
        public void mousePressed(MouseEvent me) {
            if (textField.isVisible()) {
                return;
            }
            clickTime = System.nanoTime();
            startX = me.getX();
            double delta = (currentValue.value() - min) / (max - min);
            delta = Math.pow(delta, 1.0/skew);
            valueX = (int) (delta * (getWidth()) + 0.5);
        }

        @Override
        public void mouseReleased(MouseEvent me) {
            if (dragging) {
                dragging = false;
                update(me);
                fireActionEvent(true);
            } else if ((System.nanoTime() - clickTime) < IGNORE_CLICK_TIME) {
                focusTextField();
            } else {
                fireActionEvent(false);
            }

        }

        @Override
        public void mouseDragged(MouseEvent me) {
            dragging = true;
            update(me);
        }

        private void update(MouseEvent me) {
            int curX = valueX + (me.getX() - startX);
            double delta = curX / (double) getWidth();
            delta = delta < 0 ? 0 : delta > 1 ? 1 : delta;
            delta = Math.pow(delta, skew);
            delta = delta * (max  - min) + min;
            updateValue(delta);
        }
    }
}
