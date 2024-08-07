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
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.CloseButtonFactory;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ComponentType;
import org.praxislive.core.Connection;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.PortInfo;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ProxyException;
import org.praxislive.ide.pxr.spi.RootEditor;

@Messages({
    "LBL_AddAction=Add",
    "LBL_CallAction=Call",
    "LBL_ConnectAction=Connect",
    "LBL_DisconnectAction=Disconnect",
    "ERR_UnknownType=Not found in component palette",
    "LBL_SelectAction=Select",
    "TXT_EmptySelection=<no selection>",
    "TXT_Parent=<parent>",
    "ERR_NoSetAsText=Property doesn't support text values"
})
class ToolActions {

    private final ActionEditorContext editor;
    private final JPanel actionPanel;
    private final Set<RootEditor.ToolAction> supportedActions;

    ToolActions(ActionEditorContext editor, Set<RootEditor.ToolAction> supportedActions) {
        this.editor = editor;
        this.supportedActions = Set.copyOf(supportedActions);
        actionPanel = new JPanel(new BorderLayout());
    }

    JPanel getActionPanel() {
        return actionPanel;
    }

    void installActions(InputMap im, ActionMap am) {
        for (RootEditor.ToolAction action : supportedActions) {
            switch (action) {
                case ADD -> {
                    im.put(KeyStroke.getKeyStroke("typed @"), "add-component");
                    am.put("add-component", new Add());
                }
                case SELECT -> {
                    im.put(KeyStroke.getKeyStroke("typed /"), "select");
                    am.put("select", new Select());
                }
                case CALL -> {
                    im.put(KeyStroke.getKeyStroke("typed ."), "call");
                    am.put("call", new Call());
                }
                case CONNECT -> {
                    im.put(KeyStroke.getKeyStroke("typed ~"), "connect");
                    am.put("connect", new Connect(false));
                }
                case DISCONNECT -> {
                    im.put(KeyStroke.getKeyStroke("typed !"), "disconnect");
                    am.put("disconnect", new Connect(true));
                }
            }
        }

    }

    private void installToActionPanel(JComponent component) {
        actionPanel.add(component);
        actionPanel.revalidate();
    }

    private void clearActionPanel() {
        actionPanel.removeAll();
        actionPanel.revalidate();
        editor.editor().requestFocus();
    }

    private List<PXRComponentProxy> findMatchingComponents(String glob) {
        PXRContainerProxy container = editor.context();
        if (glob.isEmpty() || glob.equals(Bundle.TXT_Parent())) {
            return List.of(container);
        }
        try {
            Pattern search = globToRegex(glob);
            return container.children()
                    .filter(id -> search.matcher(id).matches())
                    .map(id -> container.getChild(id))
                    .toList();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return List.of();
        }
    }

    class Add extends AbstractAction {

        private final Map<String, Object> typeMap;
        private final List<String> suggestedTypes;

        Add() {
            typeMap = new LinkedHashMap<>();
            suggestedTypes = new ArrayList<>();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display panel = new Display();
            buildTypeMap();
            panel.typeField.setSuggestData(suggestedTypes);
            installToActionPanel(panel);
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
                        new NotifyDescriptor.Message(Bundle.ERR_UnknownType(), NotifyDescriptor.ERROR_MESSAGE));
                return;
            }
            clearActionPanel();
            if (obj instanceof ComponentType type) {
                editor.acceptComponentType(type);
            } else {
                editor.acceptImport((FileObject) obj);
            }
        }

        private void buildTypeMap() {
            typeMap.clear();
            suggestedTypes.clear();
            Node paletteRoot = editor.palette();
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
            suggestedTypes.addAll(typeMap.keySet());
        }

        class Display extends ActionDisplay {

            private final JSuggestField typeField;

            Display() {
                add(new JLabel(Bundle.LBL_AddAction()));
                addPadding();
                typeField = new JSuggestField();
                typeField.setColumns(24);
                typeField.setMaximumSize(typeField.getPreferredSize());
                add(typeField);

                typeField.addActionListener(e -> {
                    completeAdd(typeField.getText());
                    clearActionPanel();
                });

                appendClose();
            }

        }
    }

    class Call extends AbstractAction {

        Call() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display panel = new Display();
            PXRContainerProxy container = editor.context();
            List<PXRComponentProxy> selection = editor.selection();
            List<String> components = new ArrayList<>(container.children().toList());
            components.sort(Comparator.naturalOrder());
            panel.componentField.setSuggestData(components);
            installToActionPanel(panel);
            if (selection.isEmpty() || selection.get(0) == container) {
                panel.componentField.setText(Bundle.TXT_Parent());
                panel.componentField.selectAll();
                panel.componentField.requestFocusInWindow();
            } else {
                panel.componentField.setText(selectionToGlob(selection));
                panel.commitComponent(selection);
            }
        }

        private void findProperties(List<PXRComponentProxy> components, Set<String> names) {
            HashSet<Node.Property<?>> result = new HashSet<>();
            for (Node.PropertySet propertySet : components.get(0).getNodeDelegate().getPropertySets()) {
                for (Node.Property<?> property : propertySet.getProperties()) {
                    if (property.canWrite() && !property.isHidden()) {
                        result.add(property);
                    }
                }
            }
            if (components.size() > 1) {
                HashSet<Node.Property<?>> working = new HashSet<>();
                for (int i = 1; i < components.size(); i++) {
                    for (Node.PropertySet propertySet : components.get(i).getNodeDelegate().getPropertySets()) {
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

        private void findActions(List<PXRComponentProxy> components, Set<String> names) {
            HashSet<String> result = new HashSet<>();
            ComponentInfo info = components.get(0).getInfo();
            for (String id : info.controls()) {
                if (info.controlInfo(id).controlType() == ControlInfo.Type.Action) {
                    result.add(id);
                }
            }
            if (components.size() > 1) {
                HashSet<String> working = new HashSet<>();
                for (int i = 1; i < components.size(); i++) {
                    info = components.get(i).getInfo();
                    for (String id : info.controls()) {
                        if (info.controlInfo(id).controlType() == ControlInfo.Type.Action) {
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
            List<PXRComponentProxy> cmps = findMatchingComponents(componentGlob);
            if (cmps.isEmpty()) {
                return null;
            } else {
                return findProperty(cmps.get(0), name);
            }
        }

        private Node.Property<?> findProperty(PXRComponentProxy cmp, String name) {
            for (Node.PropertySet set : cmp.getNodeDelegate().getPropertySets()) {
                for (Node.Property<?> property : set.getProperties()) {
                    if (property.getName().equals(name) && property.canWrite() && !property.isHidden()) {
                        return property;
                    }
                }
            }
            return null;
        }

        private Action findAction(String componentGlob, String controlID) {
            List<PXRComponentProxy> cmps = findMatchingComponents(componentGlob);
            for (PXRComponentProxy cmp : cmps) {
                for (Action action : cmp.getNodeDelegate().getActions(false)) {
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
            List<PXRComponentProxy> cmps = findMatchingComponents(componentGlob);
            for (PXRComponentProxy cmp : cmps) {
                Node.Property<?> property = findProperty(cmp, controlID);
                if (property != null) {
                    PropertyEditor pe = property.getPropertyEditor();
                    pe.setAsText(value);
                    ((Node.Property<Object>) property).setValue(pe.getValue());
                }
            }
        }

        private void invokeCustomPropertyEditor(String componentGlob, String controlID) {
            List<PXRComponentProxy> cmps = findMatchingComponents(componentGlob);
            if (cmps.isEmpty()) {
                return;
            }
            try {
                Node.Property<?> property = findProperty(cmps.get(0), controlID);
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
                        for (PXRComponentProxy cmp : cmps) {
                            Node.Property<Object> nodeProperty = (Node.Property<Object>) findProperty(cmp, property.getName());
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

        class Display extends ActionDisplay {

            private final JSuggestField componentField;
            private final JSuggestField controlField;
            private final JSuggestField valueField;

            Display() {
                add(new JLabel(Bundle.LBL_CallAction()));
                addPadding();

                componentField = new JSuggestField();
                componentField.setColumns(18);
                componentField.setMaximumSize(componentField.getPreferredSize());
                configureFocusActionKeys(componentField, false);
                add(componentField);
                addPadding();

                controlField = new JSuggestField();
                controlField.setEnabled(false);
                controlField.setColumns(18);
                controlField.setMaximumSize(controlField.getPreferredSize());
                configureFocusActionKeys(controlField, false);
                add(controlField);
                addPadding();

                valueField = new JSuggestField();
                valueField.setEnabled(false);
                valueField.setColumns(18);
                valueField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
                add(valueField);

                componentField.addActionListener(this::commitComponent);
                controlField.addActionListener(this::commitControl);
                valueField.addActionListener(this::commitValue);

                appendClose();
            }

            private void commitComponent(ActionEvent e) {
                List<PXRComponentProxy> cmps = findMatchingComponents(componentField.getText());
                if (cmps.isEmpty()) {
                    return;
                }
                commitComponent(cmps);
            }

            private void commitComponent(List<PXRComponentProxy> cmps) {
                HashSet<String> ids = new HashSet<>();
                findProperties(cmps, ids);
                findActions(cmps, ids);
                List<String> data = new ArrayList<>(ids);
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
                    clearActionPanel();
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
                            valueField.setSuggestData(List.of(tags));
                        } else {
                            valueField.setSuggestData(List.of());
                        }
                        valueField.selectAll();
                        valueField.requestFocusInWindow();
                    } else {
//                    if (propEd.supportsCustomEditor()) {
                        invokeCustomPropertyEditor(componentGlob, controlID);
//                    }
                        clearActionPanel();
                    }
                }
            }

            private void commitValue(ActionEvent e) {
                String controlID = controlField.getText();
                if (controlID.isEmpty()) {
                    clearActionPanel();
                    return;
                }

                String value = valueField.getText();
                try {
                    invokePropertyChange(componentField.getText(), controlID, value);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                clearActionPanel();
            }

        }

    }

    class Connect extends AbstractAction {

        private final boolean disconnect;

        Connect(boolean disconnect) {
            this.disconnect = disconnect;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display panel = new Display();

            List<String> childIDs = new ArrayList<>(editor.context().children().toList());
            childIDs.sort(Comparator.naturalOrder());

            panel.srcField.setSuggestData(childIDs);
            panel.dstField.setSuggestData(childIDs);

            installToActionPanel(panel);
            List<PXRComponentProxy> selection = editor.selection();
            if (selection.isEmpty() || selection.get(0) == editor.context()) {
                panel.srcField.requestFocusInWindow();
            } else {
                panel.srcField.setText(selectionToGlob(selection));
                panel.commitSrc();
            }
        }

        private List<String> findPorts(List<PXRComponentProxy> components, boolean output) {
            PortInfo.Direction direction = output ? PortInfo.Direction.OUT : PortInfo.Direction.IN;
            return components.stream()
                    .map(PXRComponentProxy::getInfo)
                    .flatMap(info -> {
                        return info.ports().stream()
                                .filter(pid -> info.portInfo(pid).direction() == direction);
                    })
                    .distinct()
                    .toList();
        }

        private List<String> findPorts(List<PXRComponentProxy> components, String glob, boolean output) {
            PortInfo.Direction direction = output ? PortInfo.Direction.OUT : PortInfo.Direction.IN;
            try {
                Pattern search = globToRegex(glob);
                return components.stream()
                        .map(PXRComponentProxy::getInfo)
                        .flatMap(info -> {
                            return info.ports().stream()
                                    .filter(pid -> info.portInfo(pid).direction() == direction);
                        })
                        .filter(id -> search.matcher(id).matches())
                        .distinct()
                        .toList();
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                return List.of();
            }
        }

        private void makeConnections(String src, String srcPort, String dst, String dstPort) throws ProxyException {
            List<Connection> connections = possibleConnections(src, srcPort, dst, dstPort, true);
            for (Connection connection : connections) {
                editor.context().connect(connection);
            }
        }

        private void breakConnections(String src, String srcPort, String dst, String dstPort) throws ProxyException {
            List<Connection> connections = possibleConnections(src, srcPort, dst, dstPort, false);
            for (Connection connection : connections) {
                editor.context().disconnect(connection);
            }
        }

        private List<Connection> possibleConnections(String src, String srcPort,
                String dst, String dstPort, boolean interleave) {

            record PortRef(String component, String port) {

            }

            List<PortRef> sources = findMatchingComponents(src).stream()
                    .flatMap(cmp -> {
                        String cmpID = cmp.getID();
                        return findPorts(List.of(cmp), srcPort, true).stream()
                                .map(port -> new PortRef(cmpID, port));
                    })
                    .toList();

            if (sources.isEmpty()) {
                return Collections.EMPTY_LIST;
            }

            List<PortRef> destinations = findMatchingComponents(dst).stream()
                    .flatMap(cmp -> {
                        String cmpID = cmp.getID();
                        return findPorts(List.of(cmp), dstPort, false).stream()
                                .map(port -> new PortRef(cmpID, port));
                    })
                    .toList();

            if (destinations.isEmpty()) {
                return Collections.EMPTY_LIST;
            }

            if (interleave && (sources.size() > 1 && destinations.size() > 1)) {
                int srcSz = sources.size();
                int dstSz = destinations.size();
                return IntStream.range(0, Math.max(srcSz, dstSz))
                        .mapToObj(i -> {
                            PortRef s = sources.get(i % srcSz);
                            PortRef d = destinations.get(i % dstSz);
                            return Connection.of(s.component(), s.port(), d.component(), d.port());
                        })
                        .toList();
            } else {
                return sources.stream()
                        .flatMap(s -> {
                            return destinations.stream()
                                    .map(d -> Connection.of(s.component(), s.port(),
                                    d.component(), d.port()));
                        })
                        .toList();
            }

        }

        class Display extends ActionDisplay {

            private final JSuggestField srcField;
            private final JSuggestField srcPortField;
            private final JSuggestField dstField;
            private final JSuggestField dstPortField;

            Display() {
                add(new JLabel(disconnect ? Bundle.LBL_DisconnectAction() : Bundle.LBL_ConnectAction()));
                addPadding();

                srcField = new JSuggestField();
                configureFocusActionKeys(srcField, false);
                srcField.setColumns(12);
                add(srcField);
                addPadding();

                srcPortField = new JSuggestField();
                configureFocusActionKeys(srcPortField, false);
                srcPortField.setColumns(12);
                srcPortField.setEnabled(false);
                add(srcPortField);
                addPadding();

                dstField = new JSuggestField();
                configureFocusActionKeys(dstField, false);
                dstField.setColumns(12);
                dstField.setEnabled(false);
                add(dstField);
                addPadding();

                dstPortField = new JSuggestField();
                configureFocusActionKeys(dstPortField, true);
                dstPortField.setColumns(12);
                dstPortField.setEnabled(false);
                add(dstPortField);
                addPadding();

                srcField.addActionListener(this::commitSrc);
                srcPortField.addActionListener(this::commitSrcPort);
                dstField.addActionListener(this::commitDst);
                dstPortField.addActionListener(this::commitDstPort);

                appendClose();

            }

            private void commitSrc(ActionEvent e) {
                commitSrc();
            }

            private void commitSrc() {
                String srcID = srcField.getText();
                if (srcID.isEmpty()) {
                    return;
                }
                srcPortField.setEnabled(true);
                List<String> ports = findPorts(findMatchingComponents(srcID), true);
                srcPortField.setSuggestData(ports);
                if (ports.size() == 1) {
                    srcPortField.setText(ports.get(0));
                    srcPortField.selectAll();
                }
                srcPortField.requestFocusInWindow();
            }

            private void commitSrcPort(ActionEvent e) {
                String portID = srcPortField.getText();
                if (portID.isEmpty()) {
                    return;
                }
                dstField.setEnabled(true);
                if (disconnect) {
                    dstField.setText(("*"));
                    dstField.selectAll();
                }
                dstField.requestFocusInWindow();
            }

            private void commitDst(ActionEvent e) {
                String dstID = dstField.getText();
                if (dstID.isEmpty()) {
                    return;
                }
                dstPortField.setEnabled(true);
                List<String> ports = findPorts(findMatchingComponents(dstID), false);
                if (ports.size() == 1) {
                    dstPortField.setText(ports.get(0));
                    dstPortField.selectAll();
                } else if (disconnect) {
                    dstPortField.setText(("*"));
                    dstPortField.selectAll();
                }
                dstPortField.setSuggestData(ports);
                dstPortField.requestFocusInWindow();
            }

            private void commitDstPort(ActionEvent e) {
                String portID = dstPortField.getText();
                if (portID.isEmpty()) {
                    return;
                }
                try {
                    if (disconnect) {
                        breakConnections(srcField.getText(), srcPortField.getText(),
                                dstField.getText(), portID);
                    } else {
                        makeConnections(srcField.getText(), srcPortField.getText(),
                                dstField.getText(), portID);
                    }
                } catch (ProxyException ex) {
                    Exceptions.printStackTrace(ex);
                }
                clearActionPanel();
            }

        }
    }

    class Select extends AbstractAction {

        Select() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Display panel = new Display();
            List<String> components = new ArrayList<>(editor.context().children().toList());
            components.sort(Comparator.naturalOrder());
            panel.idField.setSuggestData(components);
            installToActionPanel(panel);
            panel.idField.requestFocusInWindow();
        }

        private void completeSelection(String text) {
            if (text.isEmpty()) {
                editor.select(editor.context(), List.of());
                return;
            }
            Pattern search = globToRegex(text);
            PXRContainerProxy container = editor.context();
            List<PXRComponentProxy> selection = container.children()
                    .filter(id -> search.matcher(id).matches())
                    .map(container::getChild)
                    .toList();
            editor.select(container, selection);
        }

        class Display extends ActionDisplay {

            private JSuggestField idField;

            Display() {
                add(new JLabel(Bundle.LBL_SelectAction()));
                addPadding();
                idField = new JSuggestField();
                idField.setColumns(18);
                idField.setMaximumSize(idField.getPreferredSize());
                add(idField);
                idField.addActionListener(e -> {
                    completeSelection(idField.getText());
                    clearActionPanel();
                });
                appendClose();
            }

        }
    }

    private abstract class ActionDisplay extends JPanel {

        private ActionDisplay() {
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            addPadding();
        }

        final void appendClose() {

            Action close = new AbstractAction("close") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    clearActionPanel();
                }
            };
            JButton closeButton = CloseButtonFactory.createBigCloseButton();
            closeButton.addActionListener(close);

            InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "close");
            ActionMap am = getActionMap();
            am.put("close", close);

            add(Box.createGlue());
            addPadding();
            add(closeButton);
        }

        final void addPadding() {
            add(Box.createHorizontalStrut(8));
        }

        final void configureFocusActionKeys(JTextField textField, boolean primary) {
            if (!primary) {
                textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), JTextField.notifyAction);
                textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JTextField.notifyAction);
            }
            textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.EMPTY_SET);
            textField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.selectAll();
                }

                @Override
                public void focusLost(FocusEvent e) {
                }
            });
        }

    }

    private static Pattern globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' ->
                    regex.append(".*");
                case '?' ->
                    regex.append('.');
                case '|' ->
                    regex.append('|');
                case '_' ->
                    regex.append('_');
                case '-' ->
                    regex.append("\\-");
                default -> {
                    if (Character.isJavaIdentifierPart(c)) {
                        regex.append(c);
                    } else {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }
        return Pattern.compile(regex.toString());
    }

    private static String selectionToGlob(List<PXRComponentProxy> selection) {
        return selection.stream().map(ComponentProxy::getID).collect(Collectors.joining("|"));
    }

}
