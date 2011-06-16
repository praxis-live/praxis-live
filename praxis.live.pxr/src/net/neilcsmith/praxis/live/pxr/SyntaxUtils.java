/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class SyntaxUtils {

    private SyntaxUtils() {
    }

    public static String escape(String input) {
        String res = doPlain(input);
        if (res == null) {
            res = doQuoted(input);
        }
        if (res == null) {
            res = doBraced(input);
        }
        return res;
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

    public static String escapeQuoted(String input) {
        String res = doQuoted(input);
        if (res == null) {
            res = doBraced(input);
        }
        return res;
    }

    private static String doQuoted(String input) {
        int len = input.length();
        if (len == 0 || len > 128) {
            return null;
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
                    return null;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
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

    public static String escapeBraced(String input) {
        return doBraced(input);
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
