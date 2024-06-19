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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.praxislive.core.ComponentAddress;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.api.EditorUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.praxislive.project.GraphBuilder;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;

/**
 *
 */
class ImportRenameSupport {

    static GraphModel prepareForPaste(ContainerProxy container, GraphModel model) {
        return prepare(container, model, true);
    }

    static GraphModel prepareForImport(ContainerProxy container, GraphModel model) {
        return prepare(container, model, false);
    }

    private static GraphModel prepare(ContainerProxy container, GraphModel model, boolean paste) {
        List<String> modelIDs = model.root().children().keySet().stream().toList();

        if (modelIDs.size() == 1) {
            return prepareSingle(container, model, modelIDs.get(0), paste);
        }

        Set<String> existing = container.children().collect(
                Collectors.toCollection(LinkedHashSet::new));

        List<String> names = new ArrayList<>(modelIDs.size());
        for (String id : modelIDs) {
            String name = EditorUtils.findFreeID(existing, id, false);
            existing.add(name);
            names.add(name);
        }

        // refresh existing back to current children
        existing = container.children().collect(
                Collectors.toCollection(LinkedHashSet::new));

        RenameTableModel tableModel = new RenameTableModel(existing, modelIDs, names);
        JTable table = new JTable(tableModel);
        NotifyDescriptor dlg = constructDialog(paste ? "Paste as ..." : "Import as ...", table);
        if (DialogDisplayer.getDefault().notify(dlg) != NotifyDescriptor.OK_OPTION) {
            return null;
        }
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        Map<String, String> renames = new LinkedHashMap<>();
        for (int i = 0; i < modelIDs.size(); i++) {
            renames.put(modelIDs.get(i), names.get(i));
        }
        
        return model.withTransform(root -> renameChildren(root, renames));
    }

    private static GraphModel prepareSingle(ContainerProxy container, GraphModel model, String id, boolean paste) {
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                "ID:", "Enter an ID for " + id);
        dlg.setInputText(EditorUtils.findFreeID(container.children().collect(
                Collectors.toCollection(LinkedHashSet::new)), id, false));
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            Map<String, String> rename = Map.of(id, dlg.getInputText().strip());
            return model.withTransform(root -> renameChildren(root, rename));
        }
        return null;
    }

    private static NotifyDescriptor constructDialog(String title, JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        table.requestFocusInWindow();
        panel.add(new JScrollPane(table));
        panel.setPreferredSize(new Dimension(350, table.getRowHeight() * (table.getRowCount() + 2)));
        NotifyDescriptor dlg = new NotifyDescriptor.Confirmation(
                panel,
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE);
        return dlg;
    }

    private static void renameChildren(GraphBuilder.Root root, Map<String, String> renames) {
        root.transformChildren(children -> children
                .map(e -> Map.entry(renames.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .toList());
        root.transformConnections(connections -> connections
                .map(c -> {
                    String src = c.sourceComponent();
                    String tgt = c.targetComponent();
                    src = renames.getOrDefault(src, src);
                    tgt = renames.getOrDefault(tgt, tgt);
                    return GraphElement.connection(src, c.sourcePort(), tgt, c.targetPort());
                })
                .toList());
    }

    private static class RenameTableModel extends AbstractTableModel {

        private final Set<String> childrenIDs;
        private final List<String> modelIDs;
        private final List<String> newIDs;

        private RenameTableModel(Set<String> childrenIDs, List<String> modelIDs, List<String> newIDs) {
            this.childrenIDs = childrenIDs;
            this.modelIDs = modelIDs;
            this.newIDs = newIDs;
        }

        @Override
        public int getRowCount() {
            return modelIDs.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Existing ID";
            } else {
                return "New ID";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return modelIDs.get(rowIndex);
            } else {
                return newIDs.get(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            String val = aValue.toString();
            if (columnIndex == 1 && ComponentAddress.isValidID(val)) {
                if (childrenIDs.contains(val)) {
                    return;
                }
                int idx = newIDs.indexOf(val);
                if (idx > -1 && idx != rowIndex) {
                    return;
                }
                newIDs.set(rowIndex, val);
            }
        }

    }

}
