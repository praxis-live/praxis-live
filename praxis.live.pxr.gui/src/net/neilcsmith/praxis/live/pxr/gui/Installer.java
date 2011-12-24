/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr.gui;

import net.miginfocom.layout.LayoutUtil;
import org.openide.modules.ModuleInstall;

public class Installer extends ModuleInstall {

    @Override
    public void restored() {
        LayoutUtil.setDesignTime(null, true);
    }
}
