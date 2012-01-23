/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.LifecycleManager;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
@ServiceProvider(service = LifecycleManager.class, position = 1)
public class LifecycleManagerImpl extends LifecycleManager {

    private final static Logger LOG = Logger.getLogger(LifecycleManagerImpl.class.getName());

    @Override
    public void saveAll() {
        for (LifecycleManager lm : Lookup.getDefault().lookupAll(LifecycleManager.class)) {
            if (lm != this) {
                LOG.log(Level.FINE, "Delegating saveAll() to {0}", lm);
                lm.saveAll();
                break;
            }
        }
    }

    @Override
    public void exit() {
        DefaultHubManager hub = DefaultHubManager.getInstance();
        if (hub.getState() == DefaultHubManager.State.Stopped) {
            delegateExit();
        } else {
            hub.addPropertyChangeListener(new HubExitListener());
            hub.stop();
        }
    }

    private void delegateExit() {
        for (LifecycleManager lm : Lookup.getDefault().lookupAll(LifecycleManager.class)) {
            if (lm != LifecycleManagerImpl.this) {
                LOG.log(Level.FINE, "Delegating exit() to {0}", lm);
                lm.exit();
                break;
            }
        }
    }

    @Override
    public void markForRestart() throws UnsupportedOperationException {
        boolean delegated = false;
        for (LifecycleManager lm : Lookup.getDefault().lookupAll(LifecycleManager.class)) {
            if (lm != this) {
                delegated = true;
                LOG.log(Level.FINE, "Delegating markForRestart() to {0}", lm);
                lm.markForRestart();
                break;
            }
        }
        if (!delegated) {
            throw new UnsupportedOperationException();

        }
    }

    private class HubExitListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (pce.getNewValue() == DefaultHubManager.State.Stopped) {
                delegateExit();
            } else if (pce.getNewValue() == DefaultHubManager.State.Running) {
                // if switched back to running a shutdown task was cancelled.
                DefaultHubManager.getInstance().removePropertyChangeListener(this);
            }
        }
    }
}
