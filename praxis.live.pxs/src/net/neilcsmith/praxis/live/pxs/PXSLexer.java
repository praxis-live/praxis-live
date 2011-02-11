/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxs;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXSLexer implements Lexer<PXSTokenID> {

    private LexerRestartInfo<PXSTokenID> info;
    private boolean first;

    PXSLexer(LexerRestartInfo<PXSTokenID> info) {
        this.info = info;
        if (info.state() instanceof Integer) {
            first = false;
        } else {
            first = true;
        }
    }

    @Override
    public Token<PXSTokenID> nextToken() {
        LexerInput input = info.input();
        int i = input.read();
        switch (i) {
            // end of input
            case LexerInput.EOF:
                return null;

            // blocks
            case '{':
            case '[':
            case ';':
                first = true;
                return info.tokenFactory().createToken(PXSTokenID.SEPARATOR);
            case '}':
            case ']':
                first = false;
                return info.tokenFactory().createToken(PXSTokenID.SEPARATOR);

            // whitespace
            case ' ':
            case '\t':
            case '\n':
            case '\r':
                first |= i == '\n' || i == '\r';
                do {
                    i = input.read();
                    first |= i == '\n' || i == '\r';
                } while (i == ' '
                        || i == '\t'
                        || i == '\n'
                        || i == '\r');
                if (i != LexerInput.EOF) {
                    input.backup(1);
                }
                return info.tokenFactory().createToken(PXSTokenID.WHITESPACE);

            // comments
            case '#':
                do {
                    i = input.read();
                } while (i != '\n'
                        && i != '\r'
                        && i != LexerInput.EOF);
                first = true;
                return info.tokenFactory().createToken(PXSTokenID.COMMENT);

            // numbers
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                do {
                    i = input.read();
                } while (i >= '0'
                        && i <= '9');
                if (i == '.') {
                    do {
                        i = input.read();
                    } while (i >= '0'
                            && i <= '9');
                }
                input.backup(1);
                first = false;
                return info.tokenFactory().createToken(PXSTokenID.NUMBER);

            // strings
            case '"':
                do {
                    i = input.read();
                    if (i == '\\') {
                        i = input.read();
                        i = input.read();
                    }
                } while (i != '"'
                        && i != '\n'
                        && i != '\r'
                        && i != LexerInput.EOF);
                first = false;
                return info.tokenFactory().createToken(PXSTokenID.STRING);
            default:
                do {
                    i = input.read();
                } while (!Character.isWhitespace(i) && i != ';');
                input.backup(1);
                if (first) {
                    return info.tokenFactory().createToken(PXSTokenID.COMMAND);
                } else {
                    return info.tokenFactory().createToken(PXSTokenID.LITERAL);
                }
        }
    }

    @Override
    public Object state() {
        return first ? null : Integer.valueOf(1);
    }

    @Override
    public void release() {
        // no op
    }
}
