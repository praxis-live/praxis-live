/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
package org.praxislive.ide.project.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileUtil;
import org.openide.util.UserCancelException;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PBoolean;
import org.praxislive.core.types.PMap;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.project.api.ExecutionElement;

/**
 *
 */
public class ProjectDialogManager {

    private final static ProjectDialogManager INSTANCE = new ProjectDialogManager();

    private ProjectDialogManager() {

    }

    public void reportError(String message) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(message,
                        NotifyDescriptor.ERROR_MESSAGE));
    }

    public void reportWarnings(Map<Task, List<String>> warnings) {
        WarningsDialogPanel panel = new WarningsDialogPanel(warnings);
        NotifyDescriptor nd = new NotifyDescriptor.Message(panel, NotifyDescriptor.WARNING_MESSAGE);
        DialogDisplayer.getDefault().notify(nd);
    }

    @Deprecated
    public void showWarningsDialog(PraxisProject project,
            Map<Task, List<String>> warnings) {
        reportWarnings(warnings);
    }

    public boolean confirm(String title, String message) {
        return confirm(title, message, NotifyDescriptor.PLAIN_MESSAGE);
    }

    public boolean confirmOnError(String title, String message) {
        return confirm(title, message, NotifyDescriptor.ERROR_MESSAGE);
    }

    private boolean confirm(String title, String message, int type) {
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(message, title,
                NotifyDescriptor.YES_NO_OPTION, type);
        return DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.YES_OPTION;
    }

    @Deprecated
    public boolean continueOnError(PraxisProject project, ExecutionLevel level, ExecutionElement element, List<Value> args) {
        StringBuilder sb = new StringBuilder();
        if (element instanceof ExecutionElement.File) {
            var file = ((ExecutionElement.File) element).file();
            var path = FileUtil.getRelativePath(project.getProjectDirectory(), file);
            if (path == null) {
                path = file.getPath();
            }
            sb.append("Error executing ");
            sb.append(path);
            sb.append(".\n");
        } else if (element instanceof ExecutionElement.Line) {
            var cmd = ((ExecutionElement.Line) element).line();
            sb.append("Error executing ");
            sb.append(cmd);
            sb.append(".\n");
        }

        sb.append("Continue ");
        if (level == ExecutionLevel.BUILD) {
            sb.append("building");
        } else {
            sb.append("running");
        }
        sb.append(" project?");
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(sb.toString(),
                "Execution Error",
                NotifyDescriptor.YES_NO_OPTION,
                NotifyDescriptor.ERROR_MESSAGE);
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.YES_OPTION) {
            return true;
        } else {
            return false;
        }
    }

    public Value userInput(String title, List<Value> args) throws UserCancelException {
        String message = args.get(0).toString();
        String content = args.size() > 1 ? args.get(1).toString() : "";
        NotifyDescriptor.InputLine nd
                = new NotifyDescriptor.InputLine(message, title);
        if (!content.isBlank()) {
            nd.setInputText(content);
        }
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.YES_OPTION) {
            return PString.of(nd.getInputText());
        } else {
            throw new UserCancelException();
        }
    }

    public Value userInputConfirm(String title, List<Value> args) throws UserCancelException {
        String message = args.get(0).toString();
        NotifyDescriptor.Confirmation nd
                = new NotifyDescriptor.Confirmation(message, title);
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.YES_OPTION) {
            return PBoolean.TRUE;
        } else if (ret == NotifyDescriptor.NO_OPTION) {
            return PBoolean.FALSE;
        } else {
            throw new UserCancelException();
        }
    }

    public Value userInputMap(String title, List<Value> args) throws UserCancelException {
        String message = args.get(0).toString();
        PMap input = PMap.from(args.get(1)).orElseThrow(IllegalArgumentException::new);
        MapInputTableModel model = new MapInputTableModel(input);
        JTable table = new JTable(model);
        table.setTableHeader(null);
        table.setSurrendersFocusOnKeystroke(true);
        table.setCellSelectionEnabled(true);
        table.setShowGrid(true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(table));
        panel.setPreferredSize(new Dimension(350, table.getRowHeight() * (table.getRowCount() + 2)));
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
                new Object[]{message, panel},
                title,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.PLAIN_MESSAGE
        );
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.OK_OPTION) {
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            return model.toMap();
        } else {
            throw new UserCancelException();
        }
    }

    public Value userInputSelect(String title, List<Value> args) throws UserCancelException {
        String message = args.get(0).toString();
        PArray values = PArray.from(args.get(1)).orElseThrow(IllegalArgumentException::new);
        List<NotifyDescriptor.QuickPick.Item> items = values.stream()
                .map(v -> new NotifyDescriptor.QuickPick.Item(v.toString(), v.toString()))
                .toList();
        items.getFirst().setSelected(true);
        NotifyDescriptor.QuickPick nd = new NotifyDescriptor.QuickPick(message, title, items, false);
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.OK_OPTION) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).isSelected()) {
                    return values.get(i);
                }
            }
        }
        throw new UserCancelException();
    }

    public static ProjectDialogManager getDefault() {
        return INSTANCE;
    }

    public static ProjectDialogManager get(PraxisProject project) {
        return getDefault();
    }

    private static class MapInputTableModel extends AbstractTableModel {

        private final List<String> keys;
        private final List<String> values;

        private MapInputTableModel(PMap input) {
            this.keys = input.keys();
            this.values = new ArrayList<>(input.asMapOf(String.class).values());
        }

        @Override
        public int getRowCount() {
            return keys.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Key";
            } else {
                return "Value";
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return keys.get(rowIndex);
            } else {
                return values.get(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 1;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                values.set(rowIndex, aValue.toString());
            }
        }

        private PMap toMap() {
            List<PMap.Entry> entries = new ArrayList<>(keys.size());
            for (int i = 0; i < keys.size(); i++) {
                entries.add(PMap.entry(keys.get(i), values.get(i)));
            }
            return PMap.ofEntries(entries.toArray(PMap.Entry[]::new));
        }

    }

}
