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
package org.praxislive.ide.properties;

/**
 * Utility methods for quoting values in Pcl script.
 */
public class SyntaxUtils {

    private SyntaxUtils() {
    }

    /**
     * Escape the provided text into a single token. Will attempt to escape
     * without surrounding quotes if possible. The returned text will include
     * the surrounding quotes if necessary.
     *
     * @param input value
     * @return safe token text for input
     */
    public static String escape(String input) {
        String res = doPlain(input);
        if (res == null) {
            res = doQuoted(input);
        }
        return res;
    }

    /**
     * Escape the provided text into a single token. This method always
     * surrounds the value in quotes, which are included in the returned text.
     *
     * @param input value
     * @return safe quoted token text for input
     */
    public static String escapeQuoted(String input) {
        String res = doQuoted(input);
        return res;
    }

    @Deprecated
    public static String escapeBraced(String input) {
        return doBraced(input);
    }

    private static String doPlain(String input) {
        int len = input.length();
        if (len == 0 || len > 128) {
            return null;
        }
        if (input.startsWith(".")) {
            // script executor would change this into address
            return null;
        }
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '.' || c == '-' || c == '_') {
                continue;
            } else {
                return null;
            }
        }
        return input;
    }

    private static String doQuoted(String input) {
        int len = input.length();
        if (len == 0) { // || len > 128) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(len * 2);
        sb.append("\"");
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            switch (c) {
                case '{':
                case '}':
                case '[':
                case ']':
//                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;
//                case '\t':
//                    sb.append("\\t");
//                    break;
//                case '\n':
//                    sb.append("\\n");
//                    break;
//                case '\r':
//                    sb.append("\\r");
//                    break;
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private static String doBraced(String input) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '{':
                case '}':
                case '\\':
                    sb.append('\\');
                default:
                    sb.append(c);
            }
        }
        sb.append('}');
        return sb.toString();
    }
}
