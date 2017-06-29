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
import java.beans.PropertyEditor;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.live.model.ComponentProxy;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.CloseButtonFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@Messages({
    "LBL.call=Call",
    "TXT.emptySelection=<no selection>",
    "ERR.noText=Property doesn't support text values."
})
class CallAction extends AbstractAction {

    private final GraphEditor editor;

    CallAction(GraphEditor editor) {
        this.editor = editor;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Panel panel = new Panel(SwingUtilities.windowForComponent(editor.getEditorComponent()));
        Node[] nodes = editor.getExplorerManager().getSelectedNodes();
        if (nodes.length == 0) {
            panel.controlField.setText(Bundle.TXT_emptySelection());
            panel.controlField.setEnabled(false);
        } else {
            HashSet<String> ids = new HashSet<>();
            findProperties(nodes, ids);
            findActions(nodes, ids);
            Vector<String> data = new Vector<>(ids);
            data.sort(Comparator.naturalOrder());
            panel.controlField.setSuggestData(data);
        }

        editor.installToActionPanel(panel);
        panel.controlField.requestFocusInWindow();
    }

    private void findProperties(Node[] nodes, Set<String> names) {
        HashSet<Node.Property<?>> result = new HashSet<>();
        for (Node.PropertySet propertySet : nodes[0].getPropertySets()) {
            for (Node.Property<?> property : propertySet.getProperties()) {
                if (property.canWrite() && !property.isHidden()) {
                    result.add(property);
                }
            }
        }
        if (nodes.length > 1) {
            HashSet<Node.Property<?>> working = new HashSet<>();
            for (int i = 1; i < nodes.length; i++) {
                for (Node.PropertySet propertySet : nodes[i].getPropertySets()) {
                    for (Node.Property<?> property : propertySet.getProperties()) {
                        if (property.canWrite() && !property.isHidden()) {
                            working.add(property);
                        }
                    }
                }
                result.retainAll(working);
                working.clear();
                if (result.isEmpty()) {
                    break;
                }
            }
        }
        for (Node.Property<?> property : result) {
            names.add(property.getName());
        }
    }

    private void findActions(Node[] nodes, Set<String> names) {
        HashSet<String> result = new HashSet<>();
        ComponentInfo info = nodes[0].getLookup().lookup(ComponentProxy.class).getInfo();
        for (String id : info.getControls()) {
            if (info.getControlInfo(id).getType() == ControlInfo.Type.Action) {
                result.add(id);
            }
        }
        if (nodes.length > 1) {
            HashSet<String> working = new HashSet<>();
            for (int i = 1; i < nodes.length; i++) {
                info = nodes[i].getLookup().lookup(ComponentProxy.class).getInfo();
                for (String id : info.getControls()) {
                    if (info.getControlInfo(id).getType() == ControlInfo.Type.Action) {
                        working.add(id);
                    }
                }
                result.retainAll(working);
                working.clear();
                if (result.isEmpty()) {
                    break;
                }
            }
        }
        names.addAll(result);
    }

    private Node.Property<?> findProperty(String name) {
        Node[] nodes = editor.getExplorerManager().getSelectedNodes();
        if (nodes.length == 0) {
            return null;
        }
        return findProperty(nodes[0], name);
    }

    private Node.Property<?> findProperty(Node node, String name) {
        for (Node.PropertySet set : node.getPropertySets()) {
            for (Node.Property<?> property : set.getProperties()) {
                if (property.getName().equals(name) && property.canWrite() && !property.isHidden()) {
                    return property;
                }
            }
        }
        return null;
    }
    
    private void invokeAction(String controlID, ActionEvent e) throws Exception {
        outer : for (Node node : editor.getExplorerManager().getSelectedNodes()) {
            for (Action action : node.getActions(false)) {
                if (action == null) {
                    continue;
                }
                if (controlID.equals(action.getValue(Action.NAME))) {
                    action.actionPerformed(e);
                }
            }
        }
    }
    
    private void invokePropertyChange(String controlID, String value) throws Exception {
        for (Node node : editor.getExplorerManager().getSelectedNodes()) {
            Node.Property<?> property = findProperty(node, controlID);
            if (property != null) {
                PropertyEditor pe = property.getPropertyEditor();
                pe.setAsText(value);
                ((Node.Property<Object>)property).setValue(pe.getValue());
            }
        }
    }
    
    class Panel extends JPanel {

        private final Window parent;
        private JSuggestField controlField;
        private JTextField valueField;

        Panel(Window parent) {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            add(new JLabel(Bundle.LBL_call()));
            add(Box.createHorizontalStrut(16));
            controlField = new JSuggestField(parent);
            controlField.setColumns(18);
            controlField.setMaximumSize(controlField.getPreferredSize());
            controlField.setFocusTraversalKeysEnabled(false);
//            controlField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), JTextField.notifyAction);
//            controlField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JTextField.notifyAction);
            add(controlField);
            add(Box.createHorizontalStrut(16));
            valueField = new JTextField();
            valueField.setEnabled(false);
            add(valueField);
            add(Box.createHorizontalStrut(16));
            JButton closeButton = CloseButtonFactory.createBigCloseButton();
            add(closeButton);

            Action close = new AbstractAction("close") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editor.clearActionPanel();
                }
            };

            InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "close");

            ActionMap am = getActionMap();
            am.put("close", close);

            controlField.addActionListener(this::commitControl);
            valueField.addActionListener(this::commitValue);
            
            closeButton.addActionListener(close);

        }

        private void commitControl(ActionEvent e) {
            String controlID = controlField.getText();
            if (controlID.isEmpty()) {
                editor.clearActionPanel();
                return;
            }
            Node.Property<?> property = findProperty(controlID);
            if (property != null) {
                String valueText = property.getPropertyEditor().getAsText();
                if (valueText != null) {
                    valueField.setEnabled(true);
                    valueField.setText(valueText);
                    valueField.selectAll();
                    valueField.requestFocusInWindow();
                } else {
                    DialogDisplayer.getDefault().notify(
                            new NotifyDescriptor.Message(Bundle.ERR_noText(), NotifyDescriptor.ERROR_MESSAGE));
                    editor.clearActionPanel();
                }
            } else {
                try {
                    invokeAction(controlID, e);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                editor.clearActionPanel();
            }
        }
        
        private void commitValue(ActionEvent e) {
            String controlID = controlField.getText();
            if (controlID.isEmpty()) {
                editor.clearActionPanel();
                return;
            }
            String value = valueField.getText();
            try {
                invokePropertyChange(controlID, value);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            editor.clearActionPanel();
        }

    }

}
