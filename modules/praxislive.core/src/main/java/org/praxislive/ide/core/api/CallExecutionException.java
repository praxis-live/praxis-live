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

import org.praxislive.core.types.PError;

/**
 * Exception thrown when a call into the PraxisCORE system returns an error. The
 * Exception wraps the PError.
 */
public class CallExecutionException extends Exception {

    private static final String UNKNOWN_MESSAGE = "Unknown Error";
    private static final PError UNKNOWN = PError.of(UNKNOWN_MESSAGE);

    private final PError error;

    /**
     * Create a new instance wrapping the given {@link PError}.
     *
     * @param error wrapped error
     */
    public CallExecutionException(PError error) {
        // Don't want to throw an NPE when already in exception!
        this(error == null ? UNKNOWN : error, error == null ? UNKNOWN_MESSAGE : error.toString());
    }

    private CallExecutionException(PError error, String message) {
        super(message, error.exception().orElse(null));
        this.error = error;
    }

    /**
     * Access the wrapped {@link PError}.
     *
     * @return wrapped error
     */
    public PError error() {
        return error;
    }

}
