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
import java.util.EnumSet;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.VetoException;
import net.neilcsmith.praxis.impl.AbstractRoot.Caps;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class ExtensionContainer extends AbstractSwingRoot {

    private static final String EXT_PREFIX = "_ext_";
    private Component[] extensions;

    ExtensionContainer(Component[] extensions) {
        super(EnumSet.noneOf(Caps.class));
        this.extensions = extensions.clone();
    }

    @Override
    protected void setup() {
        super.setup();
        installExtensionsImpl();
    }

    @Override
    protected void dispose() {
        super.dispose();
        uninstallExtensionsImpl();
    }

    void uninstallExtensions() {
        if (EventQueue.isDispatchThread()) {
            uninstallExtensionsImpl();
        } else {
            try {
                EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        uninstallExtensionsImpl();
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void installExtensionsImpl() {
        for (Component ext : extensions) {
            String id = EXT_PREFIX + Integer.toHexString(System.identityHashCode(ext));
            try {
                addChild(id, ext);
            } catch (VetoException ex) {
                Exceptions.printStackTrace(ex);
            }

        }
    }

    private void uninstallExtensionsImpl() {
        String[] ids = getChildIDs();
        for (String id : ids) {
            removeChild(id);
        }
    }
}
