/*
 * Copyright (C) 2013 João Vicente Reis
 * Copyright (C) 2016 Neil C Smith
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
package br.pcgl.netbeans.glsl;

import br.pcgl.netbeans.glsl.lexer.GlslTokenId;
import org.netbeans.api.lexer.Language;
import org.netbeans.modules.csl.spi.DefaultLanguageConfig;
import org.netbeans.modules.csl.spi.LanguageRegistration;

/**
 *
 * @author João Vicente Reis
 */
@LanguageRegistration(mimeType = "text/x-glsl")
public class GlslLanguage extends DefaultLanguageConfig {

    @Override
    public Language<GlslTokenId> getLexerLanguage() {
        return GlslTokenId.getLanguage();
    }

    @Override
    public String getDisplayName() {
        return "GLSL";
    }

    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

}
