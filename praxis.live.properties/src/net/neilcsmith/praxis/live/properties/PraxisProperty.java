/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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
package net.neilcsmith.praxis.live.properties;

import java.beans.PropertyEditor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.core.api.Disposable;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class PraxisProperty<T> extends Node.Property<T>
        implements Disposable {

    private Editor editor;

    public PraxisProperty(Class<T> type) {
        super(type);
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (editor == null) {
            Editor ed = createEditor();
            if (ed == null) {
                return super.getPropertyEditor();
            }
            editor = ed;
        }
        return editor;
    }

    public boolean isActiveEditor(PropertyEditor editor) {
        if (this.editor == editor) {
            return true;
        } else {
            Editor ed = this.editor;
            while (ed instanceof DelegateEditor) {
                ed = ((DelegateEditor) ed).getCurrentEditor();
                if (ed == editor) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Editor createEditor() {
        return null;
    }

    @Override
    public abstract void setValue(T val);
   
    public abstract void setValue(T value, Callback callback);
    
    @Override
    public abstract T getValue(); // override without Exceptions.

    @Override
    public Object getValue(String key) {
        if (editor != null) {
            Object attr = editor.getAttribute(key);
            if (attr != null) {
                return attr;
            }
        }
        return super.getValue(key);
    }

    @Override
    public Enumeration<String> attributeNames() {
        // @TODO combine keys from editor
        return super.attributeNames();
    }

    @Override
    public void dispose() {
        if (editor != null) {
            editor.reset();
        }
    }

    
            
    public static interface Editor extends PropertyEditor {

        public String getPraxisInitializationString();

        public Object getAttribute(String key);

        public String[] getAttributeKeys();

        public void reset();

    }

    public static interface DelegateEditor extends Editor {

        public Editor getCurrentEditor();

    }

    public static interface SubCommandEditor extends Editor {

        public void setFromCommand(String command) throws Exception;

        public String[] getSupportedCommands();

    }

}
