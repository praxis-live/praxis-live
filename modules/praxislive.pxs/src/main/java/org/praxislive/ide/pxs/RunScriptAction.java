/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.praxislive.ide.pxs;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public final class RunScriptAction implements ActionListener {

    private final RunScriptCookie context;

    public RunScriptAction(RunScriptCookie context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        context.runScript();
    }
}
