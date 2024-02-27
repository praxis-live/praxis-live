/*
 * Copyright (C) 2013 João Vicente Reis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.pcgl.netbeans.glsl.lexer;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * See http://bits.netbeans.org/dev/javadoc/org-netbeans-modules-lexer/overview-summary.html
 * and http://platform.netbeans.org/tutorials/nbm-javacc-lexer.html
 */
public class GlslLexer implements Lexer<GlslTokenId> {

    private LexerRestartInfo<GlslTokenId> info;
    private LexerInput input;

    GlslLexer(LexerRestartInfo<GlslTokenId> info) {
        this.info = info;
        this.input = info.input();
    }

    @Override
    @SuppressWarnings("fallthrough")
    public Token<GlslTokenId> nextToken() {
        while (true) {
            int ch = input.read();

            switch (ch) {
                case ';': return token(GlslTokenId.SEMICOLON);
                case '{': return token(GlslTokenId.LBRACE);
                case '}': return token(GlslTokenId.RBRACE);
                case '(': return token(GlslTokenId.LPAREN);
                case ')': return token(GlslTokenId.RPAREN);

                case '+':
                case '-':
                case '*':
                case '%':
                case '[':
                case ']':
                case ',':
                case '!':
                case '~':
                case '<':
                case '>':
                case '=':
                case '&':
                case '^':
                case '|':
                case '?':
                case ':':
                case '\\':
                    return token(GlslTokenId.OPERATOR);

                case '\'':
                case '\"':
                    int stringStart;
                    if (ch == '\"') { stringStart = '\"'; } else { stringStart = '\''; }

                    while (true) {
                        int lastChar = ch;
                        ch = input.read();

                        if (ch == stringStart && lastChar != '\\') {
                            break;
                        }

                        if (ch == LexerInput.EOF || ch == '\r' || ch == '\n') {
                            input.backup(1);
                            break;
                        }
                    }

                    return token(GlslTokenId.STRING);

                case '/':
                    switch (input.read()) {
                        case '/': // in single-line comment
                            while (true) {
                                switch (input.read()) {
                                    case '\r': input.consumeNewline();
                                    case '\n':
                                    case LexerInput.EOF:
                                        return token(GlslTokenId.SL_COMMENT);
                                }
                            }
                        case '*': // in multi-line comment
                            while (true) {
                                ch = input.read();
                                while (ch == '*') {
                                    ch = input.read();
                                    if (ch == '/') {
                                        return token(GlslTokenId.ML_COMMENT);
                                    } else if (ch == LexerInput.EOF) {
                                        return token(GlslTokenId.ML_COMMENT_INCOMPLETE);
                                    }
                                }
                                if (ch == LexerInput.EOF) {
                                    return token(GlslTokenId.ML_COMMENT_INCOMPLETE);
                                }
                            }
                    }
                    input.backup(1);
                    return token(GlslTokenId.SLASH);

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    return finishIntOrFloatLiteral(ch);

                case '.':
                    char nextChar = (char)input.read();
                    input.backup(1);

                    if (isDigit(nextChar)) {
                        return finishIntOrFloatLiteral(ch);
                    } else {
                        return token(GlslTokenId.OPERATOR);
                    }

                case LexerInput.EOF:
                    return null;

                default:
                    if (Character.isWhitespace((char)ch)) {
                        ch = input.read();
                        while (ch != LexerInput.EOF && Character.isWhitespace((char)ch)) {
                            ch = input.read();
                        }
                        input.backup(1);
                        return token(GlslTokenId.WHITESPACE);
                    }

                    if (Character.isLetter((char)ch) || (((char)ch) == '#') || (((char)ch) == '_')) { // identifier or keyword
                        while (true) {
                            char cch = (char)ch;
                            if (ch == LexerInput.EOF || ! (Character.isLetter(cch) || isDigit(cch) || (cch == '#') || (cch == '_')) ) {
                                input.backup(1); // backup the extra char (or EOF)

                                // Check for keywords!
                                String text = input.readText().toString();

                                GlslTokenId id = GlslKeywords.keywords1.get(text);

                                if (id == null) { id = GlslKeywords.keywords2.get(text); }
                                if (id == null) { id = GlslKeywords.keywords3.get(text); }
                                if (id == null) { id = GlslKeywords.keywords4.get(text); }
                                if (id == null) { id = GlslKeywords.keywords5.get(text); }
                                if (id == null) { id = GlslKeywords.keywords6.get(text); }

                                if (id == null) { id = GlslTokenId.IDENTIFIER; }

                                return token(id);
                            }
                            ch = input.read(); // read next char
                        }
                    }

                    return token(GlslTokenId.ERROR);
            }
        }
    }

    private boolean isDigit(char ch) {
        switch(ch) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return true;
            default:
                return false;
        }
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    private Token<GlslTokenId> finishIntOrFloatLiteral(int ch) {
        boolean floatLiteral = false;
        boolean inExponent = false;
        while (true) {
            switch (ch) {
                case '.':
                    if (floatLiteral) {
                        return token(GlslTokenId.FLOAT_LITERAL);
                    } else {
                        floatLiteral = true;
                    }
                    break;
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    break;
                case 'e': case 'E': // exponent part
                    if (inExponent) {
                        return token(GlslTokenId.FLOAT_LITERAL);
                    } else {
                        floatLiteral = true;
                        inExponent = true;
                    }
                    break;
                case 'f': case 'F':
                    if (floatLiteral) {
                        return token(GlslTokenId.FLOAT_LITERAL);
                    } else {
                        input.backup(1);
                        return token(GlslTokenId.INT_LITERAL);
                    }
                default:
                    input.backup(1);
                    return token(floatLiteral ? GlslTokenId.FLOAT_LITERAL
                            : GlslTokenId.INT_LITERAL);
            }
            ch = input.read();
        }
    }

    private Token<GlslTokenId> token(GlslTokenId id) {
        return info.tokenFactory().createToken(id);
    }

    @Override
    public Object state() {
        return null;
    }

    @Override
    public void release() {
    }
}
