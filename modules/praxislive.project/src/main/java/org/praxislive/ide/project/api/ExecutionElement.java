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
package org.praxislive.ide.project.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.openide.filesystems.FileObject;
import org.praxislive.core.syntax.Token;
import org.praxislive.core.syntax.Tokenizer;

/**
 * An execution element of a project.
 */
public abstract class ExecutionElement {

    private ExecutionElement() {

    }

    /**
     * Create an execution element wrapping a single line of Pcl script.
     *
     * @param script script line
     * @return execution element
     */
    public static ExecutionElement.Line forLine(String script) {
        var tokens = new ArrayList<Token>();
        var itr = new Tokenizer(script).iterator();
        while (itr.hasNext()) {
            Token t = itr.next();
            if (t.getType() == Token.Type.EOL) {
                if (itr.hasNext()) {
                    throw new IllegalArgumentException("Script contains more than one line");
                }
                break;
            }
            tokens.add(t);
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Empty line");
        }
        if (tokens.get(0).getType() != Token.Type.PLAIN) {
            throw new IllegalArgumentException("First token isn't plain");
        }
        return new Line(script, List.copyOf(tokens));
    }

    /**
     * Create an execution element referencing a file.
     *
     * @param file element file
     * @return execution element
     */
    public static ExecutionElement forFile(FileObject file) {
        return new File(file);
    }

    /**
     * An execution element referencing a file.
     */
    public static final class File extends ExecutionElement {

        private final FileObject file;

        private File(FileObject file) {
            this.file = file;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.file);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final File other = (File) obj;
            if (!Objects.equals(this.file, other.file)) {
                return false;
            }
            return true;
        }

        /**
         * The reference file.
         *
         * @return file
         */
        public FileObject file() {
            return file;
        }

    }

    /**
     * An execution element wrapping a single line of Pcl script.
     */
    public static final class Line extends ExecutionElement {

        private final String line;
        private final List<Token> tokens;

        private Line(String line, List<Token> tokens) {
            this.line = line;
            this.tokens = tokens;
        }

        /**
         * The script line as text.
         *
         * @return line
         */
        public String line() {
            return line;
        }

        /**
         * The line as a series of script tokens.
         *
         * @return line tokens
         */
        public List<Token> tokens() {
            return tokens;
        }

    }

}
