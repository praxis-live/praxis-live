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
package org.praxislive.ide.pxr.graph;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.pxr.graph.Bundle;
import org.netbeans.spi.palette.PaletteController;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.CloseButtonFactory;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@NbBundle.Messages({
    "LBL.add=Add",
    "ERR.unknownType=Not found in component palette"
})
class AddAction extends AbstractAction {

    private final GraphEditor editor;
    private final Map<String, Object> typeMap;
    private final Vector<String> suggestedTypes;

    AddAction(GraphEditor editor) {
        this.editor = editor;
        Node paletteRoot = editor.getLookup().lookup(PaletteController.class)
                .getRoot().lookup(Node.class);
        typeMap = new LinkedHashMap<>();
        Stream.of(paletteRoot.getChildren().getNodes(true))
                .flatMap(category -> Stream.of(category.getChildren().getNodes(true)))
                .forEachOrdered(item -> {
                    ComponentType type = item.getLookup().lookup(ComponentType.class);
                    if (type != null) {
                        typeMap.put(type.toString(), type);
                    } else {
                        FileObject file = item.getLookup().lookup(FileObject.class);
                        if (file != null) {
                            typeMap.put(item.getDisplayName(), file);
                        }
                    }
                });
        suggestedTypes = new Vector(typeMap.keySet());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Panel panel = new Panel(SwingUtilities.windowForComponent(editor.getEditorComponent()));
        panel.typeField.setSuggestData(suggestedTypes);
        editor.installToActionPanel(panel);
        panel.typeField.requestFocusInWindow();
    }

    private void completeAdd(String text) {
        text = text.trim();
        if (text.isEmpty()) {
            return;
        }
        Object obj = typeMap.get(text);
        if (obj == null) {
            DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message(Bundle.ERR_unknownType(), NotifyDescriptor.ERROR_MESSAGE));
            return;
        }
        editor.clearActionPanel();
        editor.resetActivePoint();
        if (obj instanceof ComponentType) {
            editor.acceptComponentType((ComponentType) obj);
        } else {
            editor.acceptImport((FileObject) obj);
        }
    }

    class Panel extends JPanel {

        private final Window parent;
        private final JSuggestField typeField;

        Panel(Window parent) {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            add(new JLabel(Bundle.LBL_add()));
            add(Box.createHorizontalStrut(16));
            typeField = new JSuggestField(parent);
            typeField.setColumns(24);
            typeField.setMaximumSize(typeField.getPreferredSize());
            add(typeField);
            add(Box.createGlue());
            JButton closeButton = CloseButtonFactory.createBigCloseButton();
            add(closeButton);

            Action close = new AbstractAction("close") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editor.clearActionPanel();
                }
            };

            Action commit = new AbstractAction("commit") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    completeAdd(typeField.getText());
                    editor.clearActionPanel();
                }

            };

            InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "close");

            ActionMap am = getActionMap();
            am.put("close", close);

            typeField.addActionListener(commit);

            closeButton.addActionListener(close);

        }

    }

}
