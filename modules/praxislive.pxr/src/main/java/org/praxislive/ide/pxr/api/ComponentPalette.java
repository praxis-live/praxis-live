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
package org.praxislive.ide.pxr.api;

import org.netbeans.spi.palette.PaletteController;
import org.openide.nodes.Node;
import org.praxislive.ide.core.api.Disposable;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.palette.DefaultComponentPalette;

/**
 * Palette based on the {@code supported-types} property of the given container
 * context.
 * <p>
 * Add the {@link #controller()} to the editor lookup. Make sure to dispose when
 * no longer needed to free up listeners on the context.
 */
public final class ComponentPalette implements Disposable {

    private final DefaultComponentPalette delegate;

    private ComponentPalette(DefaultComponentPalette delegate) {
        this.delegate = delegate;
    }

    /**
     * Update the context to a different container.
     *
     * @param container container context or null
     */
    public void context(ContainerProxy container) {
        delegate.context(container);
    }

    /**
     * Query the current container context.
     *
     * @return current context
     */
    public ContainerProxy context() {
        return delegate.context();
    }

    /**
     * Access the palette controller.
     *
     * @return palette controller
     */
    public PaletteController controller() {
        return delegate.controller();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    /**
     * Access the root node of the palette.
     *
     * @return root node
     */
    public Node root() {
        return delegate.root();
    }

    /**
     * Create a new component palette with the provided initial context.
     *
     * @param container initial context
     * @return component palette
     */
    public static ComponentPalette create(ContainerProxy container) {
        return new ComponentPalette(DefaultComponentPalette.create(container));
    }

}
