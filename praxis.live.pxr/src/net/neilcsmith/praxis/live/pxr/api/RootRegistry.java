/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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

package net.neilcsmith.praxis.live.pxr.api;

import java.beans.PropertyChangeListener;
import net.neilcsmith.praxis.live.pxr.DefaultRootRegistry;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class RootRegistry {

    public final static String PROP_ROOTS = "roots";

    public abstract void addPropertyChangeListener(PropertyChangeListener listener);

    public abstract void removePropertyChangeListener(PropertyChangeListener listener);

    public abstract RootProxy[] getRoots();

    public RootProxy getRootByID(String id) {
        for (RootProxy root : getRoots()) {
            if (root.getAddress().getRootID().equals(id)) {
                return root;
            }
        }
        return null;
    }
    
    public RootProxy findRootForFile(FileObject file) {
        for (RootProxy root : getRoots()) {
            if (root.getSourceFile().equals(file)) {
                return root;
            }
        }
        return null;
    }

    public static RootRegistry getDefault() {
        return DefaultRootRegistry.getDefault();
    }

}
