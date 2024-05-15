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
package org.praxislive.ide.core.api;

/**
 * An object that may be disposed.
 * <p>
 * This interface now extends {@link AutoCloseable} for convenience. The default
 * implementation of {@code close()} calls through to {@code dispose()}.
 */
public interface Disposable extends AutoCloseable {

    @Override
    public default void close() throws Exception {
        dispose();
    }

    /**
     * Dispose of this object.
     */
    public void dispose();

}
