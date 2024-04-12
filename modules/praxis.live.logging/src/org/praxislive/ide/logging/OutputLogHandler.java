/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.openide.util.Lookup;
import org.praxislive.core.Value;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.types.PError;
import org.praxislive.ide.core.spi.LogHandler;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.FoldHandle;
import org.openide.windows.IOFolding;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;
import org.praxislive.core.services.LogLevel;

/**
 *
 */
public class OutputLogHandler implements LogHandler {

    private final Map<String, InputOutput> ioTabs;
    
    private OutputLogHandler() {
        ioTabs = new HashMap<String, InputOutput>();
    }
    
    @Override
    public void log(
            ComponentAddress source, 
            long time,
            LogLevel level,
            Value arg) {
        if (!getLevel().isLoggable(level)) {
            return;
        }
        String rootID = source.rootID();
        InputOutput ioTab = findIOTab(rootID);
        OutputWriter writer;
        if (level == LogLevel.ERROR) {
            ioTab.select();
            writer = ioTab.getErr();
        } else {
            writer = ioTab.getOut();
        }
        writer.print(level.name());
        writer.print(" : ");
        writer.println(source.toString());
        if (arg instanceof PError) {
            writePError(ioTab, writer, (PError) arg);
        } else {
            writer.println(arg.toString());
        }
    }

    @Override
    public LogLevel getLevel() {
        return LogLevel.INFO;
    }

    @Override
    public void close() {
        for (InputOutput ioTab : ioTabs.values()) {
            ioTab.closeInputOutput();
        }
        ioTabs.clear();
    }
    
    private InputOutput findIOTab(String rootID) {
        InputOutput ioTab = ioTabs.get(rootID);
        if (ioTab == null) {
            ioTab = IOProvider.getDefault().getIO(rootID, false);
            ioTabs.put(rootID, ioTab);
        }
        return ioTab;
    }
    
    private void writePError(InputOutput ioTab, OutputWriter writer, PError err) {
        writer.print(err.exceptionType().getSimpleName());
        writer.print(" - ");
        writer.print(err.message());
        writer.println();
        Exception ex = err.exception().orElse(null);
        if (ex != null && IOFolding.isSupported(ioTab)) {
            FoldHandle fold = IOFolding.startFold(ioTab, false);
            ex.printStackTrace(writer);
            writer.println();
            fold.finish();
        }
    }
    
    @ServiceProvider(service = LogHandler.Provider.class)
    public static class Provider implements LogHandler.Provider {

        @Override
        public Optional<LogHandler> createLogHandler(Lookup context) {
            return Optional.of(new OutputLogHandler());
        }
        
    }
    
}
