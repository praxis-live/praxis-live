/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
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

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.IllegalRootStateException;
import net.neilcsmith.praxis.core.VetoException;
import net.neilcsmith.praxis.hub.DefaultHub;
import net.neilcsmith.praxis.hub.TaskServiceImpl;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.core.api.HubManager;
import net.neilcsmith.praxis.script.impl.ScriptServiceImpl;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class DefaultHubManager extends HubManager {

    private static final String EXT_PREFIX = "_ext_";

    private DefaultHub hub;
    private ExtensionContainer container;

    @Override
    public synchronized void start() throws StateException {
        if (hub != null) {
            throw new StateException();
        }
        Component[] extensions = findExtensions();
        container = new ExtensionContainer();
        for (Component ext : extensions) {
            String extID = EXT_PREFIX + Integer.toHexString(ext.hashCode());
            try {
                container.addChild(extID, ext);
            } catch (VetoException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        hub = new DefaultHub(container,
                new ScriptServiceImpl(),
                new TaskServiceImpl());
        try {
            hub.activate();
        } catch (IllegalRootStateException ex) {
            Exceptions.printStackTrace(ex);
            hub.shutdown();
        }
    }

    @Override
    public synchronized void stop() throws StateException {
        if (hub == null) {
            throw new StateException();
        }
        if (!EventQueue.isDispatchThread()) {
            try {
                EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        container.removeAll();
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            container.removeAll();
        }
        hub.shutdown();
        hub = null;

    }

    @Override
    public synchronized void restart() throws StateException {
        stop();
        start();
    }

    @Override
    public synchronized boolean isRunning() {
        return hub != null;
    }
    
    private Component[] findExtensions() {
        Collection<? extends ExtensionProvider> providers = 
                Lookup.getDefault().lookupAll(ExtensionProvider.class);
        List<Component> list = new ArrayList<Component>(providers.size());
        for (ExtensionProvider provider : providers) {
            list.add(provider.getExtensionComponent());
        }
        return list.toArray(new Component[list.size()]);
        // @TODO add own monitor component???
    }

    private class ExtensionContainer extends AbstractSwingRoot {

        ExtensionContainer() {
            super(EnumSet.of(Caps.Component, Caps.Container));
        }

        private void removeAll() {
            for (String id : getChildIDs()) {
                removeChild(id);
            }
        }
    }
}
