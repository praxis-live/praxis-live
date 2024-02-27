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

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author João Vicente Reis
 */
public class GlslLanguageHierarchy extends LanguageHierarchy<GlslTokenId> {

    @Override
    protected synchronized Collection<GlslTokenId> createTokenIds() {
        return Arrays.asList(GlslTokenId.values());
    }

    @Override
    protected synchronized Lexer<GlslTokenId> createLexer(LexerRestartInfo<GlslTokenId> info) {
        return new GlslLexer(info);
    }

    @Override
    protected String mimeType() {
        return "text/x-glsl";
    }
}
