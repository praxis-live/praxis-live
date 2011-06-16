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

package net.neilcsmith.praxis.live.project;

import org.netbeans.spi.project.ActionProvider;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class PraxisProjectActions implements ActionProvider {
    
    private DefaultPraxisProject project;

    public PraxisProjectActions(DefaultPraxisProject project) {
        this.project = project;
    }


    @Override
    public String[] getSupportedActions() {
        return new String[] {
            ActionProvider.COMMAND_RUN,
        };
    }

    @Override
    public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
        if (ActionProvider.COMMAND_RUN.equals(command)) {
            FileHandlerIterator itr = FileHandlerIterator.createBuildAndRunIterator(project);
            itr.start();
        }
    }

    @Override
    public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
        return true;
    }

}
