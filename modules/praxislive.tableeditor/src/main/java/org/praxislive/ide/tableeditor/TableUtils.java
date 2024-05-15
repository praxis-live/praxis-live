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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.tableeditor;

import java.util.ArrayList;
import java.util.List;
import org.praxislive.core.ValueFormatException;
import org.praxislive.core.syntax.Token;
import org.praxislive.core.syntax.Tokenizer;

/**
 *
 */
class TableUtils {

    static PraxisTableModels parse(String data) throws ValueFormatException {
        PraxisTableModels models = new PraxisTableModels();
        List<Object> row = new ArrayList<>();
        PraxisTableModel model = null;
        int maxColumn = 0;
        int column = 0;
        Tokenizer tk = new Tokenizer(data);
        for (Token t : tk) {
            Token.Type type = t.getType();
            switch (type) {
                case PLAIN:
                case QUOTED:
                case BRACED:
                    if (maxColumn > 0 && column >= maxColumn) {
                        throw new ValueFormatException();
                    }
                    row.add(type == Token.Type.PLAIN
                            ? parsePlainToken(t.getText())
                            : t.getText());
                    column++;
                    break;
                case EOL:
                    if (column > 0) {
                        // inside table
                        if (model == null) {
                            model = new PraxisTableModel();
                            model.setColumnCount(column);
                            maxColumn = column;
                        } else {
                            // pad to end of row
                            while (column < maxColumn) {
                                row.add(null);
                                column++;
                            }
                        }
                        model.addRow(row.toArray());
                        row.clear();
                        column = 0;
                    } else if (model != null) {
                        // end of table
                        models.add(model);
                        model = null;
                        maxColumn = 0;
                    }
                    break;

                case COMMENT:
                    break;
                default:
                    throw new ValueFormatException();
            }
        }
        if (model != null) {
            models.add(model);
        }
        return models;
    }

    static String write(PraxisTableModels data) {
        StringBuilder sb = new StringBuilder();
        write(data, sb);
        return sb.toString();
    }

    static void write(PraxisTableModels data, StringBuilder sb) {
        boolean first = true;
        for (PraxisTableModel table : data) {
            if (!first) {
                sb.append('\n');
            }
            write(table, sb);
            first = false;
        }
    }

    static void write(PraxisTableModel data, StringBuilder sb) {
        write(data, sb, 0, 0, data.getRowCount(), data.getColumnCount());
    }

    static void write(PraxisTableModel data, StringBuilder sb,
            int rowOffset, int columnOffset, int rows, int columns) {
        for (int r = 0; r < rows; r++) {
            if (isEmpty(data, rowOffset + r, columnOffset, columnOffset + columns)) {
                sb.append('.');
                sb.append('\n');
            } else {
                boolean first = true;
                for (int c = 0; c < columns; c++) {
                    Object obj = data.getValueAt(rowOffset + r, columnOffset + c);
                    if (!first) {
                        sb.append(' ');
                    }
                    sb.append(writeValue(obj));
                    first = false;
                }
                sb.append('\n');
            }
        }
    }

    private static String parsePlainToken(String token) {
        if (".".equals(token)) {
            return null;
        } else {
            return token;
        }
    }

    private static String writeValue(Object value) {
        if (value == null) {
            return ".";
        }
        return escape(value.toString());
    }

    private static boolean isEmpty(PraxisTableModel data, int row, int start, int end) {
        for (int i = start; i < end; i++) {
            if (data.getValueAt(row, i) != null) {
                return false;
            }
        }
        return true;
    }

    private static String escape(String input) {
        String res = escapePlain(input);
        if (res == null) {
            res = escapeQuoted(input);
        }
        return res;
    }

    private static String escapePlain(String input) {
        int len = input.length();
        if (len == 0) {
            return null;
        }
        if (input.startsWith(".") || input.startsWith("#")) {
            return null;
        }
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c)
                    || c == '.' || c == '-' || c == '_' || c == '#') {
                continue;
            } else {
                return null;
            }
        }
        return input;
    }

    private static String escapeQuoted(String input) {
        int len = input.length();
        if (len == 0) {
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
                    sb.append('\\');
                    sb.append(c);
                    break;
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

}
