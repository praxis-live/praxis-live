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

import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenId;

/**
 *
 * @author João Vicente Reis
 */
public enum GlslTokenId implements TokenId {

    ERROR("error"),
    FLOAT_LITERAL("number"),
    INT_LITERAL("number"),
    OPERATOR("operator"),
    SLASH("operator"),
    SEMICOLON("operator"),
    LBRACE("operator"),
    RBRACE("operator"),
    LPAREN("operator"),
    RPAREN("operator"),
    SL_COMMENT("comment"),
    ML_COMMENT("comment"),
    ML_COMMENT_INCOMPLETE("comment"),
    WHITESPACE("whitespace"),
    IDENTIFIER("identifier"),
    KEYWORD("keyword"),
    BASIC_TYPE("basictype"),
    PREPROCESSOR("preprocessor"),
    BUILTIN_VAR("builtinvar"),
    BUILTIN_FUNC("builtinfunc"),
    STRING("string"),
    ;

    private final String primaryCategory;

    private GlslTokenId(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    @Override
    public String primaryCategory() {
        return primaryCategory;
    }

    public static Language<GlslTokenId> getLanguage() {
        return new GlslLanguageHierarchy().language();
    }
}
