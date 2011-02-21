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

package net.neilcsmith.praxis.live.core;

import java.util.EnumSet;
import net.neilcsmith.praxis.impl.AbstractRoot.Caps;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class ExtensionContainer extends AbstractSwingRoot {

     ExtensionContainer() {
         super(EnumSet.of(Caps.Component, Caps.Container));
     }


}
