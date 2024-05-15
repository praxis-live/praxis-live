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

import java.util.Optional;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.PraxisProject;

/**
 * A handler for line elements.
 */
public interface LineHandler extends ElementHandler {

    /**
     * Optional hook for line handlers to rewrite the script line when the
     * project file is written.
     *
     * @param line existing line
     * @return rewritten or existing line
     */
    public default String rewrite(String line) {
        return line;
    }

    /**
     * A provider of line handlers. Instances should be registered for global
     * lookup.
     */
    public static interface Provider {

        /**
         * Create a line handler, if possible, for the provided project,
         * execution level and line element.
         *
         * @param project executing project
         * @param level execution level
         * @param line line execution element
         * @return line handler if this provider can handle
         */
        public Optional<LineHandler> createHandler(PraxisProject project,
                ExecutionLevel level, ExecutionElement.Line line);

    }
}
