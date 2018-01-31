/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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
package org.praxislive.ide.pxr;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class AttrUtils {
    
    private AttrUtils() {}
    
    static String unescape(String text) {
        if (!text.contains("\\")) {
            return text;
        }
        int len = text.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i=0; i < len; i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                c = text.charAt(i);
                switch (c) {
                    case 'n':
                        sb.append('\n');
                        continue;
                    case 't':
                        sb.append('\t');
                        continue;
                    case 'r':
                        continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    static String escape(String text) {
        int len = text.length();
        StringBuilder sb = new StringBuilder(len * 2);
        for (int i=0; i < len; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '{':
                case '}':
                case '[':
                case ']':
                case '\"':
                case '\\':
                    sb.append('\\').append(c);
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    break;
                default:
                    sb.append(c);
            }
        }
        
        // just in case, make sure newline isn't escaped
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\\') {
            sb.append(' ');
        }
        return sb.toString();
    }
    
}
