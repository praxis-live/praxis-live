/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.core.spi;

import java.util.Optional;
import org.openide.util.Lookup;
import org.praxislive.core.Component;

/**
 * A provider of an extension component to be installed and allow for
 * communication with the PraxisCORE system. Extensions should only be executed
 * in the Swing event thread.
 */
public interface ExtensionProvider {

    /**
     * Create an extension component for the provided context.
     *
     * @param context usage context, such as project lookup
     * @return extension component if can be created for provided context
     */
    public Optional<Component> createExtension(Lookup context);

}
