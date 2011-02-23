/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.core.api;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.neilcsmith.praxis.live.core.api.HubManager;
import net.neilcsmith.praxis.live.core.api.HubManager.StateException;
import org.openide.util.Exceptions;

public final class RestartHubAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            HubManager.getDefault().restart();
        } catch (StateException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
