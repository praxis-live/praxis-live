/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxs;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public final class EvalAction implements ActionListener {

    private final DataObject context;

    public EvalAction(DataObject context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        try {
            FileObject file = context.getPrimaryFile();
            String script = file.asText();
            script = "set _PWD " + file.getURL().toURI() + "\n" + script;
            HelperExtension.executeScript(script);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

    }
}
