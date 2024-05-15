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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxs;

import java.util.List;
import java.util.Optional;
import org.praxislive.core.Component;
import org.praxislive.core.services.ScriptService;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.spi.ExtensionProvider;
import org.praxislive.ide.core.api.AbstractHelperComponent;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
public class PXSHelper extends AbstractHelperComponent {
    
    private PXSHelper(PraxisProject project) {
        
    }

    public void executeScript(String script) {
        try {
            send(ScriptService.class, ScriptService.EVAL,
                    List.of(PString.of(script)), null);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static class Provider implements ExtensionProvider {

        @Override
        public Optional<Component> createExtension(Lookup context) {
            return Optional.ofNullable(context.lookup(PraxisProject.class))
                    .map(PXSHelper::new);
        }

    }

}
