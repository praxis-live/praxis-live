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

import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.ide.pxr.graph.Bundle;
import org.praxislive.ide.model.ComponentProxy;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.CloseButtonFactory;
import org.openide.explorer.propertysheet.PropertyPanel;
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
    "TXT.parent=<parent>",
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
        if (nodes.length == 0 || nodes[0] == editor.getContainer().getNodeDelegate()) {
            panel.componentField.setText(Bundle.TXT_parent());
        } else {
            panel.componentField.setText(Utils.nodesToGlob(nodes));
        }
        panel.componentField.setSuggestData(editor.getScene().getNodes().stream().sorted().collect(Collectors.toCollection(Vector::new)));
        editor.installToActionPanel(panel);
//        panel.commitComponent(nodes);
        panel.componentField.selectAll();
        panel.componentField.requestFocusInWindow();
    }

    private Node[] findMatchingNodes(String glob) {
        if (glob.isEmpty() || glob.equals(Bundle.TXT_parent())) {
            return new Node[]{editor.getContainer().getNodeDelegate()};
        }
        try {
            Pattern search = Utils.globToRegex(glob);
            return Stream.of(editor.getContainer().getChildIDs())
                    .filter(id -> search.matcher(id).matches())
                    .map(id -> editor.getContainer().getChild(id).getNodeDelegate())
                    .toArray(Node[]::new);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return new Node[0];
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

    private Node.Property<?> findProperty(String componentGlob, String name) {
        Node[] nodes = findMatchingNodes(componentGlob);
        if (nodes.length == 0) {
            return null;
        } else {
            return findProperty(nodes[0], name);
        }
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

    private Action findAction(String componentGlob, String controlID) {
        Node[] nodes = findMatchingNodes(componentGlob);
        for (Node node : nodes) {
            for (Action action : node.getActions(false)) {
                if (action == null) {
                    continue;
                }
                if (controlID.equals(action.getValue(Action.NAME))) {
                    return action;
                }
            }
        }
        return null;
    }

    private void invokePropertyChange(String componentGlob, String controlID, String value) throws Exception {
        Node[] nodes = findMatchingNodes(componentGlob);
        for (Node node : nodes) {
            Node.Property<?> property = findProperty(node, controlID);
            if (property != null) {
                PropertyEditor pe = property.getPropertyEditor();
                pe.setAsText(value);
                ((Node.Property<Object>) property).setValue(pe.getValue());
            }
        }
    }

    private void invokeCustomPropertyEditor(String componentGlob, String controlID) {
        Node[] nodes = findMatchingNodes(componentGlob);
        if (nodes.length == 0) {
            return;
        }
        try {
            Node.Property<?> property = findProperty(nodes[0], controlID);
            if (property != null) {
                PropertyPanel panel = new PropertyPanel(property, PropertyPanel.PREF_CUSTOM_EDITOR);
                panel.setChangeImmediate(false);
                DialogDescriptor descriptor = new DialogDescriptor(
                        panel,
                        property.getDisplayName(),
                        true,
                        (e) -> {
                            if (e.getSource() == DialogDescriptor.OK_OPTION) {
                                panel.updateValue();
                            }
                        });
                if (DialogDisplayer.getDefault().notify(descriptor) == DialogDescriptor.OK_OPTION) {
                    Object value = property.getValue();
                    for (Node node : nodes) {
                        Node.Property<Object> nodeProperty = (Node.Property<Object>) findProperty(node, property.getName());
                        if (nodeProperty != property && nodeProperty != null) {
                            nodeProperty.setValue(value);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    class Panel extends JPanel {

        private final Window parent;
        private final JSuggestField componentField;
        private final JSuggestField controlField;
        private final JSuggestField valueField;

        Panel(Window parent) {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            add(new JLabel(Bundle.LBL_call()));
            add(Box.createHorizontalStrut(16));

            componentField = new JSuggestField(parent);
            componentField.setColumns(18);
            componentField.setMaximumSize(componentField.getPreferredSize());
            Utils.configureFocusActionKeys(componentField, false);
            add(componentField);

            add(Box.createHorizontalStrut(16));

            controlField = new JSuggestField(parent);
            controlField.setEnabled(false);
            controlField.setColumns(18);
            controlField.setMaximumSize(controlField.getPreferredSize());
            Utils.configureFocusActionKeys(controlField, false);
            add(controlField);

            add(Box.createHorizontalStrut(16));

            valueField = new JSuggestField(parent);
            valueField.setEnabled(false);
            valueField.setColumns(18);
            valueField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
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

            componentField.addActionListener(this::commitComponent);
            controlField.addActionListener(this::commitControl);
            valueField.addActionListener(this::commitValue);

            closeButton.addActionListener(close);

        }

        private void commitComponent(ActionEvent e) {
            Node[] nodes = findMatchingNodes(componentField.getText());
            if (nodes.length == 0) {
                return;
            }
            commitComponent(nodes);
        }

        private void commitComponent(Node[] nodes) {
            HashSet<String> ids = new HashSet<>();
            findProperties(nodes, ids);
            findActions(nodes, ids);
            Vector<String> data = new Vector<>(ids);
            data.sort(Comparator.naturalOrder());
            controlField.setSuggestData(data);
            controlField.setEnabled(true);
            controlField.requestFocusInWindow();
        }

        private void commitControl(ActionEvent e) {
            String controlID = controlField.getText();
            if (controlID.isEmpty()) {
                return;
            }
            String componentGlob = componentField.getText();
            Action action = findAction(componentGlob, controlID);
            if (action != null) {
                 try {
                    action.actionPerformed(e);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                editor.clearActionPanel();
                return;
            }
            Node.Property<?> property = findProperty(componentGlob, controlID);
            if (property != null) {
                PropertyEditor propEd = property.getPropertyEditor();
                String valueText = propEd.getAsText();
                String[] tags = propEd.getTags();
                if (valueText != null) {
                    valueField.setEnabled(true);
                    valueField.setText(valueText);
                    if (tags != null && tags.length > 0) {
                        valueField.setSuggestData(new Vector<>(Arrays.asList(tags)));
                    } else {
                        valueField.setSuggestData(new Vector<>());
                    }
                    valueField.selectAll();
                    valueField.requestFocusInWindow();
                } else {
                    if (propEd.supportsCustomEditor()) {
                        invokeCustomPropertyEditor(componentGlob, controlID);
                    }
                    editor.clearActionPanel();
                }
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
                invokePropertyChange(componentField.getText(), controlID, value);
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            editor.clearActionPanel();
        }

    }

}
