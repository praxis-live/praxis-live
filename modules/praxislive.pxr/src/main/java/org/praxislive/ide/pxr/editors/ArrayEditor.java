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
package org.praxislive.ide.pxr.editors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.praxislive.core.Value;
import org.praxislive.core.syntax.Token;
import org.praxislive.core.syntax.Tokenizer;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PNumber;
import org.praxislive.core.types.PString;
import org.praxislive.ide.properties.EditorSupport;
import org.praxislive.ide.properties.SyntaxUtils;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.praxislive.ide.properties.PraxisProperty.SubCommandEditor;

/**
 *
 */
public class ArrayEditor extends EditorSupport
        implements SubCommandEditor, ExPropertyEditor {

    private String text;
    private PropertyEnv env;

    @Override
    public void setValue(Object value) {
        try {
            super.setValue(PArray.from((Value) value).get());
            text = null;
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        Iterator<Token> tokens = new Tokenizer(text).iterator();
        super.setValue(parseTokens(tokens, false));
        this.text = text;
    }

    @Override
    public String getAsText() {
        if (text != null) {
            return text;
        } else {
            return buildValueText(false);
        }
    }

    public String getDisplayName() {
        return "Array Editor";
    }

    @Override
    public void setFromCommand(String command) throws Exception {
        Iterator<Token> tokens = new Tokenizer(command).iterator();
        Token cmd = tokens.next();
        if (cmd.getType() != Token.Type.PLAIN || !"array".equals(cmd.getText())) {
            throw new IllegalArgumentException("Not array command");
        }
        super.setValue(parseTokens(tokens, false));
        text = null;
    }

    @Override
    public String[] getSupportedCommands() {
        return new String[]{"array"};
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public boolean supportsCustomEditor() {
        return env != null;
    }

    @Override
    public Component getCustomEditor() {
        return new ArrayCustomEditor(this, env);
    }

    @Override
    public String getPraxisInitializationString() {
        return buildValueText(true);
    }

    private PArray parseTokens(Iterator<Token> tokens, boolean allowEOL) {
        if (!tokens.hasNext()) {
            return PArray.EMPTY;
        }
        List<Value> args = new ArrayList<Value>();
        boolean EOLreached = false;
        while (tokens.hasNext()) {
            if (EOLreached && !allowEOL) {
                throw new IllegalArgumentException("Extra tokens found after EOL");
            }
            Token tk = tokens.next();
            switch (tk.getType()) {
                case PLAIN:
                    args.add(parsePlainToken(tk.getText()));
                    break;
                case QUOTED:
                case BRACED:
                    args.add(PString.of(tk.getText()));
                    break;
                case SUBCOMMAND:
                    // @TODO allow [array subcommands
                    throw new IllegalArgumentException("SubCommand token not allowed in array");
                case EOL:
                    EOLreached = true;
                    break;
            }
        }
        return PArray.of(args);
    }

    private Value parsePlainToken(String text) {
        // can't have empty plain token
        char c = text.charAt(0);
        if (c == '.') {
            throw new IllegalArgumentException("Can't parse relative address");
        }
        Value ret = null;
        if (Character.isDigit(c)) {
            try {
                ret = PNumber.parse(text);
            } catch (Exception ex) {
                //fall through}
            }
        }
        if (c == '-' && text.length() > 1 && Character.isDigit(text.charAt(1))) {
            try {
                ret = PNumber.parse(text);
            } catch (Exception ex) {
                // fall through
            }
        }
        if (ret == null) {
            return PString.of(text);
        } else {
            return ret;
        }
    }

    private String buildValueText(boolean asCommand) {
        try {
            PArray array = PArray.from((Value) getValue()).get();
            StringBuilder sb = new StringBuilder();
            if (asCommand) {
                sb.append("[array ");
            }
            Value arg;
            for (int i = 0, total = array.size(); i < total; i++) {
                arg = array.get(i);
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(SyntaxUtils.escape(arg.toString()));
            }
            if (asCommand) {
                sb.append("]");
            }
            return sb.toString();
        } catch (Exception ex) {
            return null;
        }
    }
}
