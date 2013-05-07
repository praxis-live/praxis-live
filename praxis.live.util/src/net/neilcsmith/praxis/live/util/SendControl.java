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
package net.neilcsmith.praxis.live.util;

import java.util.HashMap;
import java.util.Map;
import net.neilcsmith.praxis.core.Call;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.InterfaceDefinition;
import net.neilcsmith.praxis.core.PacketRouter;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.impl.AbstractControl;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.core.api.HubUnavailableException;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class SendControl extends AbstractControl {

    private Map<Integer, Callback> pending;
    private PacketRouter router;

    public SendControl() {
        pending = new HashMap<Integer, Callback>();
    }

    public void send(ControlAddress to, CallArguments args, Callback callback)
            throws HubUnavailableException {
        ControlAddress from = getAddress();
        boolean quiet = callback == null;
        Call call;
        if (quiet) {
            call = Call.createQuietCall(to, from, System.nanoTime(), args);
        } else {
            call = Call.createCall(to, from, System.nanoTime(), args);
        }
        getPacketRouter().route(call);
        if (!quiet) {
            pending.put(call.getID(), callback);
        }
    }

    public void send(InterfaceDefinition service, String control,
            CallArguments args, Callback callback)
            throws HubUnavailableException, ServiceUnavailableException {
        ControlAddress to = ControlAddress.create(findService(service), control);
        send(to, args, callback);
    }

    private PacketRouter getPacketRouter() throws HubUnavailableException {
        if (router == null) {
            router = getLookup().get(PacketRouter.class);
            if (router == null) {
                throw new HubUnavailableException("No PacketRouter available");
            }
        }
        return router;
    }

    @Override
    public void hierarchyChanged() {
        super.hierarchyChanged();
        router = null;
        for (Callback callback : pending.values()) {
            callback.onError(CallArguments.EMPTY);
        }
        pending.clear();
    }



    @Override
    public void call(Call call, PacketRouter router) throws Exception {
        switch (call.getType()) {
            case RETURN:
                handleResponse(call, false);
                break;
            case ERROR:
                handleResponse(call, true);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private void handleResponse(Call call, boolean error) {
        Callback callback = pending.remove(call.getMatchID());
        if (callback != null) {
            if (error) {
                callback.onError(call.getArgs());
            } else {
                callback.onReturn(call.getArgs());
            }
        }
    }

    @Override
    public ControlInfo getInfo() {
        return null;
    }
}
