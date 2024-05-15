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

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.praxislive.core.Value;

/**
 * Handle result of a Call into the PraxisCORE system.
 */
public interface Callback {

    /**
     * Handle call return arguments.
     *
     * @param args call arguments
     */
    public void onReturn(List<Value> args);

    /**
     * Handle call error arguments.
     *
     * @param args call arguments
     */
    public void onError(List<Value> args);

    /**
     * Create a callback that delegates to the provided result consumer.
     *
     * @param callback result consumer
     * @return new callback
     */
    public static Callback create(Consumer<Result> callback) {
        return new Result.ResultCallback(callback);
    }

    /**
     * A callback result.
     */
    public static final class Result {

        private final boolean error;
        private final List<Value> args;

        private Result(boolean error, List<Value> args) {
            this.error = error;
            this.args = args;
        }

        /**
         * Query whether the call generated an error.
         *
         * @return true on error
         */
        public boolean isError() {
            return error;
        }

        /**
         * Call arguments.
         *
         * @return arguments
         */
        public List<Value> args() {
            return args;
        }

        private static class ResultCallback implements Callback {

            private final Consumer<Result> callback;

            private ResultCallback(Consumer<Result> callback) {
                this.callback = Objects.requireNonNull(callback);
            }

            @Override
            public void onReturn(List<Value> args) {
                callback.accept(new Result(false, List.copyOf(args)));
            }

            @Override
            public void onError(List<Value> args) {
                callback.accept(new Result(true, List.copyOf(args)));
            }

        }

    }

}
