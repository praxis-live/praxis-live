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
package org.praxislive.ide.project.spi;

import java.util.List;
import org.praxislive.ide.core.api.Callback;

/**
 * A base handler for execution elements.
 */
public interface ElementHandler {

    /**
     * Process the element.
     *
     * @param callback response callback
     * @throws Exception
     */
    public void process(Callback callback) throws Exception;

    /**
     * List of any warning messages generated during process of this handler.
     * There may be warnings whether the process results in a complete or error
     * response callback.
     *
     * @return list of warnings
     */
    public default List<String> warnings() {
        return List.of();
    }

}
