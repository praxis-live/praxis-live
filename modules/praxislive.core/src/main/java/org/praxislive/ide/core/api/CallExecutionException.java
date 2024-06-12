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
 * Exception thrown when a call into the PraxisCORE system returns an error.
 */
public class CallExecutionException extends Exception {

    /**
     * Creates a new instance of <code>HubUnavailableException</code> without
     * detail message.
     */
    public CallExecutionException() {
    }

    /**
     * Constructs an instance of <code>HubUnavailableException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public CallExecutionException(String msg) {
        super(msg);
    }

    public CallExecutionException(Throwable cause) {
        super(cause);
    }

    public CallExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
