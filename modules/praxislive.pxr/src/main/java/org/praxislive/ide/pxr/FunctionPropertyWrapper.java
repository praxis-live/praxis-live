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
package org.praxislive.ide.pxr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.openide.awt.HtmlRenderer;
import org.openide.nodes.Node;
import org.openide.util.NbBundle.Messages;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.CallExecutionException;

@Messages({
    "# {0} - Result as text",
    "HTML_ResultOK=<font color=\"!textInactiveText\"><i>-- {0}</i></font>",
    "# {0} - Error as text",
    "HTML_ResultError=<font color=\"#FF7070\"><i>ERR : {0}</i></font>"
})
class FunctionPropertyWrapper extends Node.Property<String> {

    private final PXRProxyNode node;
    private final String control;
    private final ControlInfo info;

    private Editor editor;

    private String lastInput;
    private String lastOutput;
    private boolean lastWasError;

    public FunctionPropertyWrapper(PXRProxyNode node, String control, ControlInfo info) {
        super(String.class);
        this.node = node;
        this.control = control;
        this.info = info;
        setName(control);
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public PropertyEditor getPropertyEditor() {
        if (editor == null) {
            editor = new Editor();
        }
        return editor;
    }

    @Override
    public String getValue() throws IllegalAccessException, InvocationTargetException {
        return "???";
    }

    @Override
    public void setValue(String text) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        List<ArgumentInfo> inputs = info.inputs();
        List<Value> args;
        if (inputs.isEmpty()) {
            args = List.of();
        } else if (inputs.size() == 1) {
            args = List.of(PString.of(text));
        } else {
            args = PArray.from(PString.of(text))
                    .orElseThrow(IllegalArgumentException::new)
                    .asList();
        }
        lastInput = text;
        node.component().send(control, args).whenComplete(this::handleResponse);
    }

    private void handleResponse(List<Value> result, Throwable error) {
        if (error != null) {
            lastWasError = true;
            if (error instanceof CallExecutionException cee) {
                lastOutput = cee.error().toString();
            } else {
                lastOutput = error.toString();
            }
        } else {
            if (result.isEmpty()) {
                lastOutput = "";
            } else if (result.size() == 1) {
                lastOutput = result.get(0).toString();
            } else {
                lastOutput = PArray.of(result).toString();
            }
            lastWasError = false;
        }
        node.propertyChange(control, null, null);
    }

    private class Editor extends PropertyEditorSupport {

        @Override
        public String getAsText() {
            return lastInput == null ? "" : lastInput;
        }

        @Override
        public boolean isPaintable() {
            return true;
        }

        @Override
        public void paintValue(Graphics g, Rectangle r) {
            if (lastOutput == null) {
                return;
            }
            String html;
            if (lastWasError) {
                // @TODO colour theme
                html = Bundle.HTML_ResultError(lastOutput);
            } else {
                html = Bundle.HTML_ResultOK(lastOutput);
            }

            Font font = g.getFont();
            FontMetrics fm = g.getFontMetrics(font);
            HtmlRenderer.renderHTML(html,
                    g, r.x, r.y + (r.height - fm.getHeight()) / 2 + fm.getAscent(),
                    r.width, r.height, g.getFont(), g.getColor(),
                    HtmlRenderer.STYLE_TRUNCATE, true);

        }

        @Override
        public boolean supportsCustomEditor() {
            return lastOutput != null;
        }

        @Override
        public Component getCustomEditor() {
            JTextArea textArea = new JTextArea(lastOutput);
            textArea.setEditable(false);
            textArea.setColumns(40);
            textArea.setRows(20);
            JScrollPane scroll = new JScrollPane(textArea);
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(scroll);
            return panel;
        }

    }

}
