/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.code;

import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.spi.java.hints.unused.UsedDetector;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = UsedDetector.Factory.class)
public class UsedDetectorImpl implements UsedDetector.Factory {

    public UsedDetectorImpl() {
    }

    @Override
    public UsedDetector create(CompilationInfo info) {
        boolean used = PathRegistry.getDefault().findInfo(info.getFileObject()) != null;
        return ((e, tp) -> used);
    }

}
