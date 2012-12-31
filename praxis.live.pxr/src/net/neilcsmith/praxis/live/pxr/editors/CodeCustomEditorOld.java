/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import net.neilcsmith.praxis.texteditor.TextEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author nsigma
 */
public class CodeCustomEditorOld extends JPanel implements PropertyChangeListener {

    private final PraxisPropertyEditor editor;
    private final PropertyEnv env;
    private final String template;
    private final TextEditor textEditor;

    CodeCustomEditorOld(PraxisPropertyEditor editor, PropertyEnv env, String mime, String template) {
        this.editor = editor;
        this.env = env;
        this.template = template;
        env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        env.addPropertyChangeListener(this);

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(400,300));
        
        String contents = editor.getAsText();
        contents = contents.isEmpty() ? template : contents;
        
        textEditor = TextEditor.create(mime, contents);
        
        add(textEditor.getEditorComponent());
        

        
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (PropertyEnv.PROP_STATE.equals(evt.getPropertyName()) && evt.getNewValue() == PropertyEnv.STATE_VALID) {
            String text = textEditor.getTextComponent().getText();
            if (text.equals(template)) {
                editor.setAsText("");
            } else {
                editor.setAsText(text);
            }
        }
    }
}
