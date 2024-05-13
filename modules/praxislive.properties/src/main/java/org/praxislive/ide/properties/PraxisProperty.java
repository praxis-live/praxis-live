/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.properties;

import java.beans.PropertyEditor;
import java.util.Enumeration;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.core.api.Disposable;
import org.openide.nodes.Node;
import org.praxislive.core.Value;

/**
 * Node property supporting PraxisCORE values.
 *
 * @param <T> value type
 */
public abstract class PraxisProperty<T extends Value> extends Node.Property<T>
        implements Disposable {

    private Editor editor;

    /**
     * Create a property for the given Value type.
     *
     * @param type value type
     */
    public PraxisProperty(Class<T> type) {
        super(type);
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (editor == null) {
            Editor ed = createEditor();
            if (ed == null) {
                ed = new EditorSupport(); // @TODO is this right?
            }
            editor = ed;
        }
        editor.setValue(getValue());
        return editor;
    }

    /**
     * Checks if the provided editor is the active editor for this property,
     * unwrapping {@link DelegateEditor} if necessary.
     *
     * @param editor property editor
     * @return is active editor
     */
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
    public void setValue(T val) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set the property value with response callback.
     *
     * @param value new value
     * @param callback response callback
     */
    public void setValue(T value, Callback callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite() {
        return false;
    }

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

    /**
     * An extension of PropertyEditor for PraxisProperty instances.
     */
    public static interface Editor extends PropertyEditor {

        /**
         * The initialization code as Pcl script.
         *
         * @return initialization code
         */
        public String getPraxisInitializationString();

        /**
         * Get attribute for the given key.
         *
         * @param key attribute key
         * @return attribute or null
         */
        public Object getAttribute(String key);

        /**
         * Array of attribute keys.
         *
         * @return attribute keys
         */
        public String[] getAttributeKeys();

        /**
         * Reset the editor.
         */
        public void reset();

    }

    /**
     * An editor that can delegate to other underlying editors.
     */
    public static interface DelegateEditor extends Editor {

        /**
         * Query the current active editor.
         *
         * @return current editor
         */
        public Editor getCurrentEditor();

    }

    /**
     * An editor that can set a value from a Pcl command.
     */
    public static interface SubCommandEditor extends Editor {

        /**
         * Set from the provided snippet (command and arguments) of Pcl script.
         *
         * @param command line of Pcl script
         * @throws Exception
         */
        public void setFromCommand(String command) throws Exception;

        /**
         * The Pcl commands that this editor supports.
         *
         * @return supported commands
         */
        public String[] getSupportedCommands();

    }

}
