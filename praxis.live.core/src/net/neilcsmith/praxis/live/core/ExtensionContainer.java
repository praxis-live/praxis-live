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
import java.util.HashMap;
import java.util.Map;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.Lookup;
import net.neilcsmith.praxis.core.VetoException;
import net.neilcsmith.praxis.gui.BindingContext;
import net.neilcsmith.praxis.gui.ControlBinding;
import net.neilcsmith.praxis.gui.ControlBinding.Adaptor;
import net.neilcsmith.praxis.gui.impl.DefaultBindingControl;
import net.neilcsmith.praxis.impl.AbstractRoot.Caps;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import net.neilcsmith.praxis.impl.InstanceLookup;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class ExtensionContainer extends AbstractSwingRoot {

    private static final String EXT_PREFIX = "_ext_";
    
    private Component[] extensions;
    private Map<ControlAddress, DefaultBindingControl> bindingCache;
    private Lookup lookup;
    private Bindings bindings;

    ExtensionContainer(Component[] extensions) {
        super(EnumSet.noneOf(Caps.class));
        this.extensions = extensions.clone();
        bindingCache = new HashMap<ControlAddress, DefaultBindingControl>();
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
//        clearBindings();
    }

    @Override
    public Lookup getLookup() {
        if (lookup == null) {
            lookup = InstanceLookup.create(super.getLookup(), new Bindings());
        }
        return lookup;
    }

    @Override
    public void hierarchyChanged() {
        super.hierarchyChanged();
        lookup = null;
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
        clearBindings();
    }

    private void clearBindings() {
        for (DefaultBindingControl binding : bindingCache.values()) {
            binding.unbindAll();
        }
    }

    private class Bindings extends BindingContext {

        @Override
        public void bind(ControlAddress address, Adaptor adaptor) {
            DefaultBindingControl binding = bindingCache.get(address);
            if (binding == null) {
                binding = new DefaultBindingControl(address);
                registerControl("_binding_" + Integer.toHexString(binding.hashCode()),
                        binding);
                bindingCache.put(address, binding);
            }
            binding.bind(adaptor);
        }

        @Override
        public void unbind(Adaptor adaptor) {
            ControlBinding cBinding = adaptor.getBinding();
            if (cBinding == null) {
                return;
            }
            DefaultBindingControl binding = bindingCache.get(cBinding.getAddress());
            if (binding != null) {
                binding.unbind(adaptor);
            }
        }
    }
}
