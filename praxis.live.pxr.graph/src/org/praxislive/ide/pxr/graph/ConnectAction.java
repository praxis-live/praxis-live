/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
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
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.praxislive.core.PortInfo;
import org.praxislive.ide.graph.PinID;
import org.praxislive.ide.model.Connection;
import org.praxislive.ide.model.ProxyException;
import org.openide.awt.CloseButtonFactory;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@NbBundle.Messages({
    "LBL.connect=Connect",
    "LBL.disconnect=Disconnect",})
class ConnectAction extends AbstractAction {

    private final GraphEditor editor;
    private final boolean disconnect;

    ConnectAction(GraphEditor editor, boolean disconnect) {
        this.editor = editor;
        this.disconnect = disconnect;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Panel panel = new Panel(SwingUtilities.windowForComponent(editor.getEditorComponent()));

        Vector children = Arrays.asList(editor.getContainer().getChildIDs()).stream()
                .sorted()
                .collect(Collectors.toCollection(Vector::new));

        panel.srcField.setSuggestData(children);
        panel.dstField.setSuggestData(children);

        editor.installToActionPanel(panel);
        Node[] nodes = editor.getExplorerManager().getSelectedNodes();
        if (nodes.length > 0 && nodes[0] != editor.getContainer().getNodeDelegate()) {
            panel.srcField.setText(Utils.nodesToGlob(nodes));
            panel.commitSrc();
        } else {
            panel.srcField.requestFocusInWindow();
        }
    }

    private List<String> findNodes(String glob) {
        try {
            Pattern search = Utils.globToRegex(glob);
            return Stream.of(editor.getContainer().getChildIDs())
                    .filter(id -> search.matcher(id).matches())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return Collections.EMPTY_LIST;
        }
    }

    private List<String> findPins(List<String> nodes, boolean output) {
        return nodes.stream()
                .map(nid -> editor.getContainer().getChild(nid).getInfo())
                .flatMap(info -> Stream.of(info.getPorts())
                .filter(pid -> info.getPortInfo(pid).getDirection()
                != (output ? PortInfo.Direction.IN : PortInfo.Direction.OUT))
                )
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> findPins(List<String> nodes, String glob, boolean output) {
        try {
            Pattern search = Utils.globToRegex(glob);
            return nodes.stream()
                    .map(nid -> editor.getContainer().getChild(nid).getInfo())
                    .flatMap(info -> Stream.of(info.getPorts())
                    .filter(pid -> info.getPortInfo(pid).getDirection()
                    != (output ? PortInfo.Direction.IN : PortInfo.Direction.OUT)))
                    .filter(id -> search.matcher(id).matches())
                    .distinct()
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return Collections.EMPTY_LIST;
        }
    }

    private void makeConnections(String src, String srcPin, String dst, String dstPin) throws ProxyException {

        List<Connection> connections = possibleConnections(src, srcPin, dst, dstPin, true);

        for (Connection connection : connections) {
            editor.getContainer().connect(connection, null);
        }

    }

    private void breakConnections(String src, String srcPin, String dst, String dstPin) throws ProxyException {

        List<Connection> connections = possibleConnections(src, srcPin, dst, dstPin, false);

        for (Connection connection : connections) {
            editor.getContainer().disconnect(connection, null);
        }

    }

    private List<Connection> possibleConnections(String src, String srcPin, String dst, String dstPin, boolean interleave) {

        List<PinID<String>> sources = findNodes(src).stream()
                .flatMap(n -> findPins(Collections.singletonList(n), srcPin, true).stream().map(id -> new PinID<>(n, id)))
                .collect(Collectors.toList());

        if (sources.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        List<PinID<String>> destinations = findNodes(dst).stream()
                .flatMap(n -> findPins(Collections.singletonList(n), dstPin, false).stream().map(id -> new PinID<>(n, id)))
                .collect(Collectors.toList());

        if (destinations.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (interleave && (sources.size() > 1 && destinations.size() > 1)) {
            int srcSz = sources.size();
            int dstSz = destinations.size();
            return IntStream.range(0, Math.max(srcSz, dstSz))
                    .mapToObj(i -> createConnection(
                    sources.get(i % srcSz),
                    destinations.get(i % dstSz)))
                    .collect(Collectors.toList());
        } else {
            return sources.stream()
                    .flatMap(s -> destinations.stream()
                    .map(d -> new Connection(s.getParent(), s.getName(), d.getParent(), d.getName())))
                    .collect(Collectors.toList());
        }

    }

    private Connection createConnection(PinID<String> source, PinID<String> destination) {
        return new Connection(source.getParent(), source.getName(),
                destination.getParent(), destination.getName());
    }

    class Panel extends JPanel {

        private final Window parent;
        private final JSuggestField srcField;
        private final JSuggestField srcPinField;
        private final JSuggestField dstField;
        private final JSuggestField dstPinField;

        Panel(Window parent) {
            this.parent = parent;
            setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
            setFocusCycleRoot(true);
            add(new JLabel(disconnect ? Bundle.LBL_disconnect() : Bundle.LBL_connect()));
            add(Box.createHorizontalStrut(16));

            srcField = new JSuggestField();
            Utils.configureFocusActionKeys(srcField, false);
            srcField.setColumns(12);
            add(srcField);

            add(Box.createHorizontalStrut(8));

            srcPinField = new JSuggestField();
            Utils.configureFocusActionKeys(srcPinField, false);
            srcPinField.setColumns(12);
            srcPinField.setEnabled(false);
            add(srcPinField);

            add(Box.createHorizontalStrut(8));

            dstField = new JSuggestField();
            Utils.configureFocusActionKeys(dstField, false);
            dstField.setColumns(12);
            dstField.setEnabled(false);
            add(dstField);

            add(Box.createHorizontalStrut(8));

            dstPinField = new JSuggestField();
            Utils.configureFocusActionKeys(dstPinField, true);
            dstPinField.setColumns(12);
            dstPinField.setEnabled(false);
            add(dstPinField);

            add(Box.createHorizontalStrut(8));
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

            closeButton.addActionListener(close);

            srcField.addActionListener(this::commitSrc);
            srcPinField.addActionListener(this::commitSrcPin);
            dstField.addActionListener(this::commitDst);
            dstPinField.addActionListener(this::commitDstPin);

        }

        private void commitSrc(ActionEvent e) {
            commitSrc();
        }

        private void commitSrc() {
            String srcID = srcField.getText();
            if (srcID.isEmpty()) {
                return;
            }
            srcPinField.setEnabled(true);
            List<String> pins = findPins(findNodes(srcID), true);
            srcPinField.setSuggestData(new Vector(pins));
            if (pins.size() == 1) {
                srcPinField.setText(pins.get(0));
                srcPinField.selectAll();
            }
            srcPinField.requestFocusInWindow();
        }

        private void commitSrcPin(ActionEvent e) {
            String pinID = srcPinField.getText();
            if (pinID.isEmpty()) {
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
            dstPinField.setEnabled(true);
            List<String> pins = findPins(findNodes(dstID), false);
            if (pins.size() == 1) {
                dstPinField.setText(pins.get(0));
                dstPinField.selectAll();
            } else if (disconnect) {
                dstPinField.setText(("*"));
                dstPinField.selectAll();
            }
            dstPinField.setSuggestData(new Vector(pins));
            dstPinField.requestFocusInWindow();
        }

        private void commitDstPin(ActionEvent e) {
            String dstPinID = dstPinField.getText();
            if (dstPinID.isEmpty()) {
                return;
            }
            try {
                if (disconnect) {
                    breakConnections(srcField.getText(), srcPinField.getText(),
                            dstField.getText(), dstPinID);
                } else {
                    makeConnections(srcField.getText(), srcPinField.getText(),
                            dstField.getText(), dstPinID);
                }
            } catch (ProxyException ex) {
                Exceptions.printStackTrace(ex);
            }
            editor.clearActionPanel();
        }

    }

}
