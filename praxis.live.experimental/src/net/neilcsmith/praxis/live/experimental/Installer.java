/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.experimental;

import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public void validate() throws IllegalStateException {

    }



    @Override
    public void restored() {
        // By default, do nothing.
        // Put your startup code here.
    }
}
