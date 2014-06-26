/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.live.core.api.Callback;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;

/**
 *
 * @author Neil C Smith
 */
class PasteActionPerformer extends AbstractAction implements Callback {

    private final static Logger LOG = Logger.getLogger(PasteActionPerformer.class.getName());
    private GraphEditor editor;
    private ExplorerManager em;

    PasteActionPerformer(GraphEditor editor, ExplorerManager em) {
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
        if (editor.getActionSupport().pasteFromClipboard(editor.getContainer(), this)) {
            editor.sync(false);
        }
    }

    @Override
    public void onReturn(CallArguments args) {
        editor.sync(true);
    }

    @Override
    public void onError(CallArguments args) {
        DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message("Error pasting.",
                        NotifyDescriptor.ERROR_MESSAGE));
        editor.sync(true);
    }
    
}
