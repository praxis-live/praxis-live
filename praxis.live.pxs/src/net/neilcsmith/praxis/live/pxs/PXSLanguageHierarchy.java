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

import java.util.Collection;
import java.util.EnumSet;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PXSLanguageHierarchy extends LanguageHierarchy<PXSTokenID>{

    @Override
    protected Collection<PXSTokenID> createTokenIds() {
        return EnumSet.allOf(PXSTokenID.class);
    }

    @Override
    protected Lexer<PXSTokenID> createLexer(LexerRestartInfo<PXSTokenID> info) {
        return new PXSLexer(info);
    }

    @Override
    protected String mimeType() {
        return "text/x-praxis-script";
    }

}
