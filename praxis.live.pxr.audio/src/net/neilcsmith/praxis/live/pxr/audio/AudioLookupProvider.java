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

package net.neilcsmith.praxis.live.pxr.audio;

import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@LookupProvider.Registration(projectType="net-neilcsmith-praxis-live-project")
public class AudioLookupProvider implements LookupProvider {

    private final static PrivilegedTemplates templates = new PrivTemplatesImpl();

    @Override
    public Lookup createAdditionalLookup(Lookup baseContext) {
        return Lookups.singleton(templates);
    }

    private static class PrivTemplatesImpl implements PrivilegedTemplates {

        @Override
        public String[] getPrivilegedTemplates() {
            return new String[]{
                "Templates/Praxis/AudioTemplate.pxr"
            };
        }
    }
}
