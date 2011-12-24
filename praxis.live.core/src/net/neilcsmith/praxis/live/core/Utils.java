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
package net.neilcsmith.praxis.live.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.core.api.RootLifecycleHandler;
import net.neilcsmith.praxis.live.core.api.Task;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class Utils {
    
    
    private Utils() {}
    
    static List<Task> findRootDeletionTasks(Set<String> roots) {
        List<Task> tasks = new ArrayList<Task>();
        for (RootLifecycleHandler handler :
                Lookup.getDefault().lookupAll(RootLifecycleHandler.class)) {
            Task task = handler.getDeletionTask(roots);
            if (task != null) {
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    static Component[] findExtensions() {
        Collection<? extends ExtensionProvider> providers =
                Lookup.getDefault().lookupAll(ExtensionProvider.class);
        List<Component> list = new ArrayList<Component>(providers.size());
        for (ExtensionProvider provider : providers) {
            list.add(provider.getExtensionComponent());
        }
        return list.toArray(new Component[list.size()]);
    }
    
}
