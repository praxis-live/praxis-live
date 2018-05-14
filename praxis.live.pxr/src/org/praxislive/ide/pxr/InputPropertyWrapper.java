/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
package org.praxislive.ide.pxr;

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
import org.praxislive.ide.properties.EditorSupport;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.praxislive.core.CallArguments;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.model.ProxyException;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class InputPropertyWrapper extends Node.Property<String> {

    private final PXRComponentProxy cmp;
    private final String id;
    
    InputPropertyWrapper(PXRComponentProxy cmp, String id) {
        super(String.class);
        this.cmp = cmp;
        this.id = id;
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
        try {
            cmp.call(id, CallArguments.create(PString.valueOf(val)), new Callback() {
                @Override
                public void onReturn(CallArguments args) {
                }
                
                @Override
                public void onError(CallArguments args) {
                    
                }
            });
        } catch (ProxyException ex) {
            Exceptions.printStackTrace(ex);
        }
    
    }

}