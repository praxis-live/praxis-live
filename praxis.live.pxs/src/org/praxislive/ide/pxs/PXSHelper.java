/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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
package org.praxislive.ide.pxs;

import org.praxislive.core.CallArguments;
import org.praxislive.core.Component;
import org.praxislive.core.services.ScriptService;
import org.praxislive.core.services.ServiceUnavailableException;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.spi.ExtensionProvider;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.core.api.AbstractHelperComponent;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service = ExtensionProvider.class)
public class PXSHelper implements ExtensionProvider {

    private final static ComponentImpl component = new ComponentImpl();

    @Override
    public Component getExtensionComponent() {
        return component;
    }

    public static void executeScript(String script) {
        component.executeScript(script);
    }

    private static class ComponentImpl extends AbstractHelperComponent {

        private void executeScript(String script) {
            try {
                send(ScriptService.class, ScriptService.EVAL, CallArguments.create(PString.valueOf(script)), null);
            } catch (HubUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ServiceUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
