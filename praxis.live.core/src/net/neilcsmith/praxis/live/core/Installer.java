/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.core;

import net.neilcsmith.praxis.live.core.api.HubManager;
import net.neilcsmith.praxis.live.core.api.HubStateException;
import org.openide.modules.ModuleInfo;
import org.openide.modules.ModuleInstall;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {

        String version = "EA:";

        for (ModuleInfo info : Lookup.getDefault().lookupAll(ModuleInfo.class)) {
            if (info.owns(this.getClass())) {
                version = version + info.getImplementationVersion();
            }
        }
        System.setProperty("netbeans.buildnumber", version);


        try {
            HubManager.getDefault().start();
        } catch (HubStateException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
