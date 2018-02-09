/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2015 Neil C Smith.
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
package org.praxislive.ide.core;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.Value;
import org.praxislive.core.Call;
import org.praxislive.core.CallArguments;
import org.praxislive.core.ComponentAddress;
import org.praxislive.core.Control;
import org.praxislive.core.PacketRouter;
import org.praxislive.core.ControlInfo;
import org.praxislive.impl.AbstractSwingRoot;
import org.praxislive.ide.core.api.LogHandler;
import org.praxislive.logging.LogLevel;
import org.praxislive.logging.LogService;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class Logging extends AbstractSwingRoot {

    private final LogHandler[] handlers;

    Logging(List<LogHandler> handlers) {
        super(EnumSet.noneOf(Caps.class));
        if (handlers.isEmpty()) {
            this.handlers = new LogHandler[]{new FallbackHandler()};
        } else {
            this.handlers = handlers.toArray(new LogHandler[handlers.size()]);
        }
        registerControl(LogService.LOG, new LogControl());
        registerInterface(LogService.class);
    }

    @Override
    protected void dispose() {
        super.dispose();
        for (LogHandler handler : handlers) {
            handler.close();
        }
    }

    private void dispatch(ComponentAddress src, long time, LogLevel level, Value arg) {
        for (LogHandler handler : handlers) {
            handler.log(src, time, level, arg);
        }
    }

    private class LogControl implements Control {

        @Override
        public void call(Call call, PacketRouter router) throws Exception {
            Call.Type type = call.getType();
            long time = call.getTimecode();
            if (type == Call.Type.INVOKE || type == Call.Type.INVOKE_QUIET) {
                ComponentAddress src = call.getFromAddress().getComponentAddress();
                CallArguments args = call.getArgs();
                for (int i = 1; i < args.getSize(); i += 2) {
                    LogLevel level = LogLevel.valueOf(args.get(i - 1).toString());
                    dispatch(src, time, level, args.get(i));
                }
            }
            if (type == Call.Type.INVOKE) {
                router.route(Call.createReturnCall(call, CallArguments.EMPTY));
            }
        }

        @Override
        public ControlInfo getInfo() {
            return LogService.LOG_INFO;
        }

    }

    private static class FallbackHandler extends LogHandler {

        @Override
        public void log(ComponentAddress source, long time, LogLevel level, Value arg) {
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
            Logger.getAnonymousLogger().log(jlevel, "{0} : {1} : {2}", new Object[]{level, source, arg});
        }

    }

}
