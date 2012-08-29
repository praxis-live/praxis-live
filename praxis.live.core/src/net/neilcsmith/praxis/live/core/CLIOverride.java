/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service=OptionProcessor.class, supersedes={"net.neilcsmith.praxis.player.CLIProcessor"})
public class CLIOverride extends OptionProcessor {

    private final static Option ALWAYS = Option.always();
    private final static Option FILE = Option.defaultArguments();
    
    @Override
    protected Set<Option> getOptions() {
        Set<Option> opts = new HashSet<Option>(3);
        opts.add(ALWAYS);
        opts.add(FILE);        
        return opts;
    }

    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        // no op for now
    }
    
}
