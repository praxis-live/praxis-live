/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Neil C Smith.
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
package net.neilcsmith.praxis.live.tracker;

import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.EventObject;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.text.DefaultEditorKit;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class TrackerTable extends JTable {

    private static final TrackerTransferHandler TRANSFER_HANDLER
            = new TrackerTransferHandler();

    public TrackerTable() {
        getTableHeader().setReorderingAllowed(false);
        setCellSelectionEnabled(true);
        setSurrendersFocusOnKeystroke(true);
        setTransferHandler(TRANSFER_HANDLER);
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
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        actionPerformed(e);
                    }
                });
                return;
            }
            int row = getSelectedRow();
            int rowCount = getSelectedRowCount();
            int column = getSelectedColumn();
            int columnCount = getSelectedColumnCount();

            if (row < 0 || column < 0 || rowCount < 1 || columnCount < 1) {
                return;
            }

            for (int r = row; r < (row + rowCount); r++) {
                for (int c = column; c < (column + columnCount); c++) {
                    setValueAt(null, r, c);
                }
            }

        }

    }
    
    private class RedirectAction extends AbstractAction {

        private final TrackerTable table;
        private final Action action;
        
        private RedirectAction(TrackerTable table, Action action) {
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
            if (c instanceof TrackerTable) {
                TrackerTable table = (TrackerTable) c;
                int row = table.getSelectedRow();
                int rowCount = table.getSelectedRowCount();
                int column = table.getSelectedColumn();
                int columnCount = table.getSelectedColumnCount();

                if (row < 0 || column < 0 || rowCount < 1 || columnCount < 1) {
                    return null;
                }

                Pattern pattern = (Pattern) table.getModel();

                StringBuilder sb = new StringBuilder();

                TrackerUtils.write(pattern, sb, row, column, rowCount, columnCount);

                return new StringSelection(sb.toString());

            }
            return null;
        }

        @Override
        public boolean canImport(TransferSupport support) {
            return support.isDataFlavorSupported(DataFlavor.stringFlavor);
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (support.getComponent() instanceof TrackerTable
                    && canImport(support)) {
                try {
                    TrackerTable table = (TrackerTable) support.getComponent();
                    int row = table.getSelectedRow();
                    int rowCount = table.getSelectedRowCount();
                    int column = table.getSelectedColumn();
                    int columnCount = table.getSelectedColumnCount();

                    if (row < 0 || column < 0 || rowCount != 1 || columnCount != 1) {
                        return false;
                    }

                    String data = (String) support.getTransferable()
                            .getTransferData(DataFlavor.stringFlavor);
                    
                    Patterns patterns = TrackerUtils.parse(data);
                    
                    if (patterns.size() != 1) {
                        return false;
                    }
                    
                    Pattern pattern = patterns.getPattern(0);
                    
                    for (int r = 0; r < pattern.getRowCount(); r++) {
                        int rd = r + row;
                        if (rd >= table.getRowCount()) {
                            break;
                        }
                        for (int c = 0; c < pattern.getColumnCount(); c++) {
                            int cd = c + column;
                            if (cd >= table.getColumnCount()) {
                                break;
                            }
                            Object value = pattern.getValueAt(r, c);
                            table.setValueAt(value, rd, cd);
                        }
                    }
                    

                } catch (UnsupportedFlavorException | IOException | ArgumentFormatException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            return false;
        }

    }

}
