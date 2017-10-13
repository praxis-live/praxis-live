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
package net.neilcsmith.praxis.live.editor.saveflash;

import java.util.Objects;
import javax.swing.JEditorPane;
import javax.swing.text.Element;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class FlashOnSaveTask implements OnSaveTask {

    private final OnSaveTask.Context context;

    private FlashOnSaveTask(OnSaveTask.Context context) {
        this.context = context;
    }

    @Override
    public void performTask() {
        DataObject dob = NbEditorUtilities.getDataObject(context.getDocument());
        if (dob != null) {
            EditorCookie ec = dob.getLookup().lookup(EditorCookie.class);
            if (ec != null) {
                JEditorPane[] panes = ec.getOpenedPanes();
                if (panes != null) {
                    for (JEditorPane pane : panes) {
                        performTask(pane);
                    }
                }
            }
        }
    }

    private void performTask(JEditorPane pane) {
        Object prop = pane.getClientProperty(FlashOnSaveHighlight.class);
        Element root = context.getModificationsRootElement();
        if (prop instanceof FlashOnSaveHighlight && root != null) {
            ((FlashOnSaveHighlight) prop).highlight(root);
        }
    }

    @Override
    public void runLocked(Runnable run) {
        run.run();
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class)
    public static class TaskFactory implements OnSaveTask.Factory {

        @Override
        public OnSaveTask createTask(Context context) {
            return new FlashOnSaveTask(Objects.requireNonNull(context));
        }

    }

}
