/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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
package org.praxislive.ide.project.spi.ui;

import java.util.Optional;
import org.openide.nodes.Node;

/**
 * Service interface to allow for decoration of files and folders in the project
 * tree view. Implementations should be registered in the project lookup. The
 * first registered implementation to provide a decorated node takes precedence.
 */
public interface ProjectNodeDecorator {

    /**
     * Optionally provide a new node to replace the provided file/folder node.
     * This may be a FilterNode or other suitable node representation of the
     * underlying context. The first provider to return a non-empty response
     * takes precedence.
     *
     * @param node node to decorate
     * @return replacement node or empty optional
     */
    public Optional<Node> decorate(Node node);

}
