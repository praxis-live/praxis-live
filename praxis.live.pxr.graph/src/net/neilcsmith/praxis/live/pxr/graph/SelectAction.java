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
package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.openide.awt.CloseButtonFactory;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@Messages({
    "LBL.select=Select"
})
class SelectAction extends AbstractAction {

    private final GraphEditor editor;
    
    SelectAction(GraphEditor editor) {
        this.editor = editor;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Panel panel = new Panel(SwingUtilities.windowForComponent(editor.getEditorComponent()));
        Vector<String> data = new Vector(editor.getScene().getNodes());
        data.sort(Comparator.naturalOrder());
        panel.idField.setSuggestData(data);
        editor.installToActionPanel(panel);
        panel.idField.requestFocusInWindow();
    }
    
    private void completeSelection(String text) {
        if (text.isEmpty()) {
            editor.getScene().userSelectionSuggested(Collections.emptySet(), false);
            return;
        }
        Pattern search = Utils.globToRegex(text);
        LinkedHashSet<String> selection = editor.getScene().getNodes().stream()
                .filter(t -> search.matcher(t).matches())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        editor.getScene().userSelectionSuggested(selection, false);
        if (!selection.isEmpty()) {
            editor.getScene().setFocusedObject(selection.iterator().next());
        } else {
            editor.getScene().setFocusedObject(null);
        }
    }
    
    class Panel extends JPanel {
        
        private final Window parent;
        private JSuggestField idField;
        
        Panel(Window parent) {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            add(new JLabel(Bundle.LBL_select()));
            add(Box.createHorizontalStrut(16));
            idField = new JSuggestField(parent);
            idField.setColumns(18);
            idField.setMaximumSize(idField.getPreferredSize());
            add(idField);
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
                    completeSelection(idField.getText());
                    editor.clearActionPanel();
                }
                
            };
            
            InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "close");
            
            ActionMap am = getActionMap();
            am.put("close", close);

            idField.addActionListener(commit);
            
            closeButton.addActionListener(close);
            
        }
        
    }
    
}
