/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.tableeditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.TopComponent;

/**
 *
 */
@NbBundle.Messages({
    "label.table=Table",
    "label.rows=Rows",
    "label.columns=Columns",
    "dialog.delete.title=Confirm deletion",
    "dialog.delete.message=Delete last table?",
    "dialog.close.title=Unsaved changes",
    "dialog.close.message=Contains unsaved changes. Close anyway?"
})
@TopComponent.Description(preferredID = "TableEditor",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
public final class TableEditorTopComponent extends TopComponent {

    private final static String RESOURCE_DIR = "org/praxislive/ide/tableeditor/resources/";

    private final TableDataObject dob;
    private final Listener listener;

    private PraxisTableModels tableModels;
    private PraxisTable table;
    private JComboBox<String> tableSelect;
    private JSpinner rowSpinner;
    private JSpinner columnSpinner;
    private JButton addButton;
    private JButton removeButton;
    private boolean modified;

    TableEditorTopComponent(TableDataObject dob) {
        this.dob = dob;
        setDisplayName(dob.getPrimaryFile().getName());
        associateLookup(new ProxyLookup(dob.getLookup(),
                Lookups.singleton(getActionMap())));
        setLayout(new BorderLayout());
        listener = new Listener();
        tableModels = new PraxisTableModels();
        initTable();
        initTopToolbar();
        initBottomToolbar();
        updateTableModels(tableModels);
    }

    private void initTable() {
        table = new PraxisTable();
        add(new JScrollPane(table), BorderLayout.CENTER);
        getActionMap().setParent(table.getActionMap());
    }

    private void initTopToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        tableSelect = new JComboBox<>();
        tableSelect.addActionListener(listener);
        toolbar.add(tableSelect);
        toolbar.addSeparator();
        removeButton = new JButton(
                ImageUtilities.loadImageIcon(RESOURCE_DIR + "delete.png", true));
        removeButton.addActionListener(listener);
        addButton = new JButton(
                ImageUtilities.loadImageIcon(RESOURCE_DIR + "add.png", true));
        addButton.addActionListener(listener);
        toolbar.add(removeButton);
        toolbar.add(addButton);
        toolbar.addSeparator();
        add(toolbar, BorderLayout.NORTH);

    }

    private void initBottomToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        rowSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 512, 1));
        rowSpinner.addChangeListener(listener);
        columnSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 32, 1));
        columnSpinner.addChangeListener(listener);
        toolbar.add(new JLabel(Bundle.label_rows()));
        toolbar.addSeparator();
        toolbar.add(rowSpinner);
        toolbar.addSeparator();
        toolbar.add(new JLabel(Bundle.label_columns()));
        toolbar.addSeparator();
        toolbar.add(columnSpinner);
        add(toolbar, BorderLayout.SOUTH);
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        dob.editorClosed(this);
    }

    @Override
    public boolean canClose() {
        if (modified) {
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
                    Bundle.dialog_close_message(),
                    Bundle.dialog_close_title(),
                    NotifyDescriptor.YES_NO_OPTION);
            if (!NotifyDescriptor.YES_OPTION.equals(
                    DialogDisplayer.getDefault().notify(nd))) {
                return false;
            }
        }
        return super.canClose();
    }

    void updateTableModels(PraxisTableModels models) {
        listener.ignore = true;
        this.tableModels = models;
        if (models.size() == 0) {
            models.add(new PraxisTableModel(16, 4));
        }
        configurePatternSelect();
        table.setModel(models.get(0));
        configureDimensions();
        listener.ignore = false;
    }
    
    void finishEditing() {
        if (table.isEditing()) {
            if (!table.getCellEditor().stopCellEditing()) {
                table.getCellEditor().cancelCellEditing();
            }
        }
    }

    private void configurePatternSelect() {
        tableSelect.removeAllItems();
        for (int i = 0; i < tableModels.size(); i++) {
            tableSelect.addItem(Bundle.label_table() + " : " + i);
        }
        tableSelect.setSelectedIndex(0);
    }

    private void addTable() {
        PraxisTableModel model = new PraxisTableModel(16, 4);
        tableModels.add(model);
        tableSelect.addItem(Bundle.label_table() + " : "
                + tableSelect.getItemCount());
        tableSelect.setSelectedIndex(tableSelect.getItemCount() - 1);
        table.setModel(model);
        configureDimensions();
    }

    private void removeTable() {
        int count = tableModels.size();
        if (count < 2) {
            return;
        }
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
                Bundle.dialog_delete_message(),
                Bundle.dialog_delete_title(),
                NotifyDescriptor.YES_NO_OPTION);
        if (NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(nd))) {
            PraxisTableModel model = tableModels.remove(count - 1);
            tableSelect.removeItemAt(tableSelect.getItemCount() - 1);
            if (model == table.getModel()) {
                tableSelect.setSelectedIndex(0);
                table.setModel(tableModels.get(0));
                configureDimensions();
            }
        }
    }

    private void configureDimensions() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        rowSpinner.setValue(model.getRowCount());
        columnSpinner.setValue(model.getColumnCount());
    }

    void setModified(boolean modified) {
        if (this.modified == modified) {
            return;
        }
        this.modified = modified;
        if (modified) {
            setHtmlDisplayName("<html><b>" + dob.getPrimaryFile().getName() + "</b>");
        } else {
            setHtmlDisplayName(null);
        }

    }

    private class Listener implements ActionListener, ChangeListener {

        private boolean ignore;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (ignore) {
                return;
            }
            ignore = true;
            Object source = e.getSource();
            if (source == tableSelect) {
                int p = tableSelect.getSelectedIndex();
                if (p >= 0 && p < tableModels.size()) {
                    table.setModel(tableModels.get(p));
                    configureDimensions();
                }
            } else if (source == addButton) {
                addTable();
            } else if (source == removeButton) {
                removeTable();
            } else {

            }

            ignore = false;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (ignore) {
                return;
            }
            ignore = true;
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            if (e.getSource() == rowSpinner) {
                model.setRowCount((int) rowSpinner.getValue());
            } else if (e.getSource() == columnSpinner) {
                model.setColumnCount((int) columnSpinner.getValue());
            }
            ignore = false;
        }

    }

}
