/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import net.neilcsmith.praxis.live.properties.EditorSupport;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class ActionPropertyWrapper extends Node.Property<String> {

    private final Action action;
    private final String id;
    
    ActionPropertyWrapper(Action action) {
        super(String.class);
        this.action = action;
        this.id = String.valueOf(action.getValue(Action.NAME));
        setName(id);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public String getValue() throws IllegalAccessException, InvocationTargetException {
        return "";
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public void setValue(String val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        return new Editor(action);
    }

    private static class Editor extends EditorSupport 
            implements ExPropertyEditor, InplaceEditor.Factory{

        private final Button button;
        
        Editor(Action action) {
            button = new Button(action);
        }
        
        @Override
        public void attachEnv(PropertyEnv env) {
            env.registerInplaceEditorFactory(this);
        }

        @Override
        public InplaceEditor getInplaceEditor() {
            return button;
        }

        @Override
        public boolean isPaintable() {
            return true;
        }

        @Override
        public void paintValue(Graphics gfx, Rectangle box) {
            button.setSize(box.width, box.height);
            button.doLayout();
            Graphics g = gfx.create(box.x, box.y, box.width, box.height);
            button.print(g);
            g.dispose();
        }

        @Override
        public void setAsText(String text) throws IllegalArgumentException {
        }

        
    }
    
    private static class Button extends JButton implements InplaceEditor {

        private PropertyEditor pe;
        private PropertyEnv env;
        private PropertyModel pm;

        private Button(Action action) {
            super(String.valueOf(action.getValue(Action.NAME)));
            setActionCommand(COMMAND_SUCCESS);
            addActionListener(e -> action.actionPerformed(e));
        }
                
        @Override
        public void connect(PropertyEditor pe, PropertyEnv env) {
            this.pe = pe;
            this.env = env;
            AWTEvent invokingEvent = EventQueue.getCurrentEvent();
            if (invokingEvent instanceof KeyEvent) {
                EventQueue.invokeLater(this::doClick);
            }
        }
        
        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void clear() {
            pe = null;
            env = null;
        }

        @Override
        public Object getValue() {
            return "";
        }

        @Override
        public void setValue(Object o) {
        }

        @Override
        public boolean supportsTextEntry() {
            return true;
        }

        @Override
        public void reset() {
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return new KeyStroke[0];
        }

        @Override
        public PropertyEditor getPropertyEditor() {
            return pe;
        }

        @Override
        public PropertyModel getPropertyModel() {
            return pm;
        }

        @Override
        public void setPropertyModel(PropertyModel pm) {
            this.pm = pm;
        }

        @Override
        public boolean isKnownComponent(Component c) {
            return false;
        }
        
    }
    
}
