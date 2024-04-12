/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith
 *
 * Derived from code licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.
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
package org.praxislive.ide.pxr.graph;

import org.netbeans.api.visual.action.InplaceEditorProvider;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;

import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

final class SuggestFieldInplaceEditorProvider implements InplaceEditorProvider<JSuggestField> {

    private SuggestFieldInplaceEditor editor;
    private EnumSet<InplaceEditorProvider.ExpansionDirection> expansionDirections;

    private KeyListener keyListener;
    private FocusListener focusListener;
    private DocumentListener documentListener;

    public SuggestFieldInplaceEditorProvider(SuggestFieldInplaceEditor editor) {
        this(editor, null);
    }

    SuggestFieldInplaceEditorProvider(SuggestFieldInplaceEditor editor, EnumSet<InplaceEditorProvider.ExpansionDirection> expansionDirections) {
        this.editor = editor;
        this.expansionDirections = expansionDirections;
    }

    public JSuggestField createEditorComponent(EditorController controller, Widget widget) {
        if (!editor.isEnabled(widget)) {
            return null;
        }
        JSuggestField field = new JSuggestField();
        List<String> suggestedValues = editor.getSuggestedValues(widget);
        if (suggestedValues != null && !suggestedValues.isEmpty()) {
            field.setSuggestData(new Vector<>(suggestedValues));
            field.showSuggest();
        } else {
            field.setText(editor.getText(widget));
            field.selectAll();
        }
        Scene scene = widget.getScene();
        double zoomFactor = scene.getZoomFactor();
        if (zoomFactor > 1.0) {
            Font font = scene.getDefaultFont();
            font = font.deriveFont((float) (font.getSize2D() * zoomFactor));
            field.setFont(font);
        }
        return field;
    }

    public void notifyOpened(final EditorController controller, Widget widget, JSuggestField editor) {
        editor.setMinimumSize(new Dimension(64, 19));
        keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case KeyEvent.VK_ESCAPE:
                        e.consume();
                        controller.closeEditor(false);
                        break;
                    case KeyEvent.VK_ENTER:
                        e.consume();
                        controller.closeEditor(true);
                        break;
                }
            }
        };
        focusListener = new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                controller.closeEditor(true);
            }
        };
        documentListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }

            public void changedUpdate(DocumentEvent e) {
                controller.notifyEditorComponentBoundsChanged();
            }
        };
        editor.addKeyListener(keyListener);
        editor.addFocusListener(focusListener);
        editor.getDocument().addDocumentListener(documentListener);
//        editor.selectAll();
    }

    public void notifyClosing(EditorController controller, Widget widget, JSuggestField editor, boolean commit) {
        editor.getDocument().removeDocumentListener(documentListener);
        editor.removeFocusListener(focusListener);
        editor.removeKeyListener(keyListener);
        if (commit) {
            this.editor.setText(widget, editor.getText());
            if (widget != null) {
                widget.getScene().validate();
            }
        }
    }

    public Rectangle getInitialEditorComponentBounds(EditorController controller, Widget widget, JSuggestField editor, Rectangle viewBounds) {
        return null;
    }

    public EnumSet<ExpansionDirection> getExpansionDirections(EditorController controller, Widget widget, JSuggestField editor) {
        return expansionDirections;
    }

}
