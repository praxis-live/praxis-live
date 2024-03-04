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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */

package org.praxislive.ide.core.api;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.praxislive.core.Value;


/**
 *
 */
public interface Callback {

    public void onReturn(List<Value> args);

    public void onError(List<Value> args);

    public static Callback create(Consumer<Result> callback) {
        return new Result.ResultCallback(callback);
    }
    
    public static final class Result {
        
        private final boolean error;
        private final List<Value> args;
        
        private Result(boolean error, List<Value> args) {
            this.error = error;
            this.args = args;
        }
        
        public boolean isError() {
            return error;
        }
        
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
