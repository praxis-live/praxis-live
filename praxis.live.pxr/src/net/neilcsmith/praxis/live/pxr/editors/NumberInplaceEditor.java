/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.types.PNumber;
import net.neilcsmith.praxis.live.pxr.PXRHelper;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class NumberInplaceEditor implements InplaceEditor, ChangeListener {

    private static final Logger LOG = Logger.getLogger(NumberInplaceEditor.class.getName());

    private final JSpinner spinner;
    private PropertyEditor propertyEditor;
    private PropertyModel propertyModel;
    private ControlAddress address;
    private boolean ignoreChanges;
    private Object initialValue;
    private List<ActionListener> listeners;

    NumberInplaceEditor(ArgumentInfo info) {
        listeners = new ArrayList<ActionListener>();
        spinner = new JSpinner(getSpinnerModel(info));
        spinner.addChangeListener(this);
        spinner.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "resetValue");
        spinner.getActionMap().put("resetValue", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                reset();
                fireActionEvent(true);
            }
        });
    }

    private SpinnerNumberModel getSpinnerModel(ArgumentInfo info) {
        Argument minProp = info.getProperties().get(PNumber.KEY_MINIMUM);
        Argument maxProp = info.getProperties().get(PNumber.KEY_MAXIMUM);
        if (minProp != null && maxProp != null) {
            try {
                double min = PNumber.coerce(minProp).value();
                double max = PNumber.coerce(maxProp).value();
                double step;
                double diff = Math.abs(max - min);
                if (diff > 99) {
                    step = 1;
                } else if (diff > 9) {
                    step = 0.1;
                } else {
                    step = 0.01;
                }
                return new SpinnerNumberModel(min, min, max, step);
            } catch (ArgumentFormatException ex) {
                LOG.log(Level.FINE, "Error setting min and max values", ex);
                // fall through to default
            }
        }
        return new SpinnerNumberModel(0, PNumber.MIN_VALUE, PNumber.MAX_VALUE, 1);
    }

    @Override
    public void connect(PropertyEditor pe, PropertyEnv env) {
        this.propertyEditor = pe;
        address = null;
        Object ad = env.getFeatureDescriptor().getValue("address");
        if (ad instanceof ControlAddress) {
            address = (ControlAddress) ad;
        }
        initialValue = pe.getValue();
        setValue(initialValue);

    }

    @Override
    public JComponent getComponent() {
        return spinner;
    }

    @Override
    public void clear() {
        propertyEditor = null;
        propertyModel = null;
        address = null;
    }

    @Override
    public Object getValue() {
        try {
            spinner.commitEdit();
            return PNumber.valueOf(((Number) spinner.getValue()).doubleValue());
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Exception in getValue()", ex);
            return propertyEditor.getValue();

        }
    }

    @Override
    public void setValue(Object o) {
        ignoreChanges = true;
        setValueImpl(o);
        ignoreChanges = false;
    }

    private void setValueImpl(Object o) {
         try {
            PNumber val = PNumber.coerce((Argument) o);
            spinner.setValue(val.value());
        } catch (Exception ex) {
            LOG.log(Level.FINE, "Exception in setValue()", ex);
        }
    }

    @Override
    public boolean supportsTextEntry() {
        return true;
    }

    @Override
    public void reset() {
        LOG.fine("Reset Called");
        setValueImpl(initialValue);
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
        ActionEvent ev = new ActionEvent(this, 0, success ? COMMAND_SUCCESS : COMMAND_FAILURE);
        for(ActionListener l : listeners.toArray(new ActionListener[0])) {
            l.actionPerformed(ev);
        }
    }

    @Override
    public KeyStroke[] getKeyStrokes() {
        LOG.fine("getKeyStrokes() called");
        return new KeyStroke[] {
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false)
        };
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
        return c == spinner || spinner.isAncestorOf(c);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (address != null && !ignoreChanges) {
            try {
                PXRHelper.getDefault().send(address, CallArguments.create(PNumber.valueOf(((Number) spinner.getValue()).doubleValue())), null);
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Exception sending value", ex);
            }
        }
    }
}
