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
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.syntax.Token;
import net.neilcsmith.praxis.core.syntax.Tokenizer;
import net.neilcsmith.praxis.core.types.PArray;
import net.neilcsmith.praxis.core.types.PNumber;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.pxr.SyntaxUtils;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ArrayEditor extends PraxisPropertyEditorSupport
        implements SubCommandEditor, ExPropertyEditor {

    private String text;
    private PropertyEnv env;

    @Override
    public void setValue(Object value) {
        try {
            super.setValue(PArray.coerce((Argument) value));
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
            return buildValueText();
        }
    }

    @Override
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
        return new StringCustomEditor(this, env);
    }

    @Override
    public String getPraxisInitializationString() {
        return super.getPraxisInitializationString();
    }



    private PArray parseTokens(Iterator<Token> tokens, boolean allowEOL) {
        if (!tokens.hasNext()) {
            return PArray.EMPTY;
        }
        List<Argument> args = new ArrayList<Argument>();
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
                    args.add(PString.valueOf(tk.getText()));
                    break;
                case SUBCOMMAND:
                    throw new IllegalArgumentException("SubCommand token not allowed in array");
                case EOL:
                    EOLreached = true;
                    break;
            }
        }
        return PArray.valueOf(args);
    }

    private Argument parsePlainToken(String text) {
        // can't have empty plain token
        char c = text.charAt(0);
        if (c == '.') {
            throw new IllegalArgumentException("Can't parse relative address");
        }
        Argument ret = null;
        if (Character.isDigit(c)) {
            try {
                ret = PNumber.valueOf(text);
            } catch (Exception ex) {
                //fall through}
            }
        }
        if (c == '-' && text.length() > 1 && Character.isDigit(text.charAt(1))) {
            try {
                ret = PNumber.valueOf(text);
            } catch (Exception ex) {
                // fall through
            }
        }
        if (ret == null) {
            return PString.valueOf(text);
        } else {
            return ret;
        }
    }

    private String buildValueText() {
        try {
            PArray array = PArray.coerce((Argument) getValue());
            StringBuilder sb = new StringBuilder();
            Argument arg;
            for (int i = 0, total = array.getSize(); i < total; i++) {
                arg = array.get(i);
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(SyntaxUtils.escape(arg.toString()));
            }
            return sb.toString();
        } catch (ArgumentFormatException ex) {
            return null;
        }
    }
}
