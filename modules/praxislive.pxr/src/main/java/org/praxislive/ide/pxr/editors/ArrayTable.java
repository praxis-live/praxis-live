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
package org.praxislive.ide.pxr.editors;

import java.util.ArrayList;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EventObject;
import java.util.List;
import org.praxislive.core.Value;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PString;
import javax.swing.table.DefaultTableModel;
import org.openide.util.Exceptions;

/**
 *
 */
class ArrayTable extends JTable {

    private static final TrackerTransferHandler TRANSFER_HANDLER
            = new TrackerTransferHandler();

    public ArrayTable() {
        super(1, 1);
        getTableHeader().setReorderingAllowed(false);
        setRowSelectionAllowed(true);
        setSurrendersFocusOnKeystroke(true);
        setTransferHandler(TRANSFER_HANDLER);
        setTableHeader(null);
        setShowGrid(true);
        initActions();
    }

    private void initActions() {
        Action[] actions = new Action[]{new DeleteAction()};
        InputMap im = getInputMap();
        ActionMap am = getActionMap();
        for (Action action : actions) {
            String name = (String) action.getValue(Action.NAME);
            im.put((KeyStroke) action.getValue(Action.ACCELERATOR_KEY), name);
            am.put(name, action);
        }
        am.put(DefaultEditorKit.copyAction,
                new RedirectAction(this, TransferHandler.getCopyAction()));
        am.put(DefaultEditorKit.pasteAction,
                new RedirectAction(this, TransferHandler.getPasteAction()));
        wrapTab();
    }
    
    private void wrapTab() {
        Object key = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke(
                KeyEvent.VK_TAB, 0));
        Action action = getActionMap().get(key);
        if (action != null) {
            getActionMap().put(key, new NextCellAction(action));
        }
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (e instanceof KeyEvent
                && suppressKeyEvent((KeyEvent) e)) {
            return false;
        }
        return super.editCellAt(row, column, e);
    }

    private boolean suppressKeyEvent(KeyEvent ke) {
        int mask = KeyEvent.CTRL_DOWN_MASK
                | KeyEvent.META_DOWN_MASK;
        return (ke.getModifiersEx() & mask) != 0;
    }

    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            super("delete");
            putValue(Action.SHORT_DESCRIPTION, "Delete");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                    KeyEvent.VK_DELETE, 0));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            int row = getSelectedRow();
            int rowCount = getSelectedRowCount();

            if (row < 0 || rowCount < 1) {
                return;
            }

            for (int r = row + rowCount - 1; r >= row; r--) {
//                if (r == getRowCount() - 1 && r > 0) {
                    ((DefaultTableModel) getModel()).removeRow(r);
                    getSelectionModel().setSelectionInterval(r - 1, r - 1);
//                } else {
//                    setValueAt(null, r, 0);
//                }
            }
        }

    }
    
    private class NextCellAction extends AbstractAction {

        private final Action wrapped;
        
        public NextCellAction(Action wrapped) {
            super("next-cell");
            this.wrapped = wrapped;
            putValue(Action.SHORT_DESCRIPTION, "Next Cell");
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(
                    KeyEvent.VK_TAB, 0));
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            int row = getSelectedRow() + 1;

            if (row >= getRowCount()) {
                ((DefaultTableModel) getModel()).addRow(new Object[]{""});
            }
            wrapped.actionPerformed(e);
        }

    }

    private class RedirectAction extends AbstractAction {

        private final ArrayTable table;
        private final Action action;

        private RedirectAction(ArrayTable table, Action action) {
            super("redirect-" + String.valueOf(action.getValue(NAME)));
            this.table = table;
            this.action = action;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            e = new ActionEvent(table, e.getID(), e.getActionCommand(),
                    e.getWhen(), e.getModifiers());
            action.actionPerformed(e);
        }

    }

    private static class TrackerTransferHandler extends TransferHandler {

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            if (c instanceof ArrayTable) {
                ArrayTable table = (ArrayTable) c;
                int row = table.getSelectedRow();
                int rowCount = table.getSelectedRowCount();

                if (row < 0 || rowCount < 1) {
                    return null;
                }

                List<Value> list = new ArrayList<>(rowCount);
                for (int i = 0; i < rowCount; i++) {
                    Object o = table.getValueAt(row + i, 0);
                    list.add(o == null ? PString.EMPTY : PString.of(o));
                }

                return new StringSelection(PArray.of(list).toString());

            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.getComponent() instanceof ArrayTable
                    && canImport(support)) {
                try {
                    ArrayTable table = (ArrayTable) support.getComponent();
                    int row = table.getSelectedRow();
                    int rowCount = table.getSelectedRowCount();

                    if (row < 0 || rowCount != 1) {
                        return false;
                    }

                    String data = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);

                    PArray values = PArray.parse(data);

                    for (int r = 0; r < values.size(); r++) {
                        int rd = r + row;
                        if (rd >= table.getRowCount()) {
                            break;
                        }
                        table.setValueAt(values.get(r).toString(), rd, 0);
                    }

                } catch (UnsupportedFlavorException | IOException | ValueFormatException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return false;
        }

    }

}
