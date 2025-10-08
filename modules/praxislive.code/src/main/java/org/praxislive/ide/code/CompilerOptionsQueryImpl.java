/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.code;

import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.spi.java.queries.CompilerOptionsQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = CompilerOptionsQueryImplementation.class)
public class CompilerOptionsQueryImpl implements CompilerOptionsQueryImplementation {

    private final Result result;

    public CompilerOptionsQueryImpl() {
        this.result = new ResultImpl();
    }

    @Override
    public Result getOptions(FileObject file) {
        PathRegistry.Info info = PathRegistry.getDefault().findInfo(file);
        if (info != null) {
            return result;
        } else {
            return null;
        }
    }

    static class ResultImpl extends Result {

        private final List<String> options;

        private ResultImpl() {
            this.options = List.of("-Xlint");
        }

        @Override
        public List<? extends String> getArguments() {
            return options;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
        }

    }

}
