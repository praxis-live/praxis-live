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
package org.praxislive.ide.pxr.graph;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import org.praxislive.ide.core.api.Callback;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.praxislive.core.Value;

/**
 *
 */
class PasteActionPerformer extends AbstractAction implements Callback {

    private final static Logger LOG = Logger.getLogger(PasteActionPerformer.class.getName());
    private GraphEditor editor;
    private ExplorerManager em;

    PasteActionPerformer(GraphEditor editor, ExplorerManager em) {
        super("Paste");
        this.editor = editor;
        this.em = em;
//        em.addPropertyChangeListener(new PropertyChangeListener() {
//            @Override
//            public void propertyChange(PropertyChangeEvent evt) {
//                refresh();
//            }
//        });
//        refresh();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        assert EventQueue.isDispatchThread();
        EventQueue.invokeLater(() -> {
            if (editor.getActionSupport().pasteFromClipboard(editor.getContainer(), this)) {
                editor.syncGraph(false);
            }
        });

    }

    @Override
    public void onReturn(List<Value> args) {
        editor.syncGraph(true, true);
    }

    @Override
    public void onError(List<Value> args) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message("Error pasting.",
                        NotifyDescriptor.ERROR_MESSAGE));
        editor.syncGraph(true);
    }

}
