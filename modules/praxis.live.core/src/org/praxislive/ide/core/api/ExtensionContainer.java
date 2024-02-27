/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.core.api;

import java.util.List;
import java.util.stream.Collectors;
import org.praxislive.core.Component;
import org.praxislive.core.VetoException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Info;
import org.praxislive.ide.core.spi.ExtensionProvider;

/**
 *
 */
public final class ExtensionContainer extends AbstractIDERoot {
    
    private static final ComponentInfo INFO = Info.component().build();
    private static final String EXT_PREFIX = "_ext_";
    
    private final List<Component> extensions;

    private ExtensionContainer(List<Component> extensions) {
        this.extensions = extensions;
    }

    @Override
    protected void starting() {
        installExtensions();
    }

    @Override
    protected void stopping() {
        uninstallExtensions();
    }

    private void installExtensions() {
        for (Component ext : extensions) {
            String id = EXT_PREFIX + Integer.toHexString(System.identityHashCode(ext));
            try {
                addChild(id, ext);
            } catch (VetoException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void uninstallExtensions() {
        String[] ids = children().toArray(String[]::new);
        for (String id : ids) {
            removeChild(id);
        }
    }


    @Override
    public ComponentInfo getInfo() {
        return INFO;
    }
    
    public List<Component> extensions() {
        return extensions;
    }

    public static ExtensionContainer create(Lookup context) {
        var exts = Lookup.getDefault().lookupAll(ExtensionProvider.class)
                .stream()
                .flatMap(p -> p.createExtension(context).stream())
                .collect(Collectors.toList());
        return new ExtensionContainer(List.copyOf(exts));
    }
    

}
