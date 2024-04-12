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
package org.praxislive.ide.core.api;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.openide.util.Lookup;
import org.praxislive.core.Value;
import org.praxislive.core.Call;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Control;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.Info;
import org.praxislive.core.services.LogLevel;
import org.praxislive.core.services.LogService;
import org.praxislive.ide.core.spi.LogHandler;

/**
 *
 */
public final class Logging extends AbstractIDERoot {
    
    private static final ComponentInfo INFO = Info.component()
            .control(LogService.LOG, LogService.LOG_INFO)
            .protocol(LogService.class)
            .build();

    private final List<LogHandler> handlers;

    private Logging(List<LogHandler> handlers) {
        if (handlers.isEmpty()) {
            this.handlers = List.of(new FallbackHandler());
        } else {
            this.handlers = handlers;
        }
        registerControl(LogService.LOG, new LogControl());
    }
    
    @Override
    public ComponentInfo getInfo() {
        return INFO;
    }
    
    public LogLevel getLogLevel() {
        LogLevel level = LogLevel.ERROR;
        for (LogHandler handler : handlers) {
            LogLevel l = handler.getLevel();
            if (!level.isLoggable(l)) {
                level = l;
            }
        }
        return level;
    }

    @Override
    protected void stopping() {
        handlers.forEach(LogHandler::close);
    }

    private void dispatch(ComponentAddress src, long time, LogLevel level, Value arg) {
        handlers.forEach(handler -> {
            handler.log(src, time, level, arg);
        });
    }

    public static Logging create(Lookup context) {
        var handlers = Lookup.getDefault().lookupAll(LogHandler.Provider.class)
                .stream()
                .flatMap(p -> p.createLogHandler(context).stream())
                .collect(Collectors.toList());
        return new Logging(List.copyOf(handlers));
    }

    
    private class LogControl implements Control {

        @Override
        public void call(Call call, PacketRouter router) throws Exception {
            if (call.isRequest()) {
                var src = call.from().component();
                var time = call.time();
                var args = call.args();
                for (int i = 1; i < args.size(); i += 2) {
                    LogLevel level = LogLevel.valueOf(args.get(i - 1).toString());
                    dispatch(src, time, level, args.get(i));
                }
            }
            if (call.isReplyRequired()) {
                router.route(call.reply());
            }
        }

    }

    private static class FallbackHandler implements LogHandler {

        @Override
        public void log(ComponentAddress source, long time, LogLevel level, Value message) {
            Level jlevel = Level.SEVERE;
            switch (level) {
                case WARNING:
                    jlevel = Level.WARNING;
                    break;
                case INFO:
                    jlevel = Level.INFO;
                    break;
                case DEBUG:
                    jlevel = Level.CONFIG;
                    break;
            }
            Logger.getAnonymousLogger().log(jlevel, "{0} : {1} : {2}", new Object[]{level, source, message});
        }

    }
    
}
