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
package net.neilcsmith.praxis.live.core;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import net.neilcsmith.praxis.core.Call;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.RootManagerService;
import net.neilcsmith.praxis.core.interfaces.ServiceManager;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.impl.AbstractAsyncControl;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class RootManagerOverride extends AbstractSwingRoot {

    private ComponentAddress defaultService;
    private Set<String> knownRoots;

    RootManagerOverride() {
        super(EnumSet.of(Caps.Component));
        registerControl(RootManagerService.ADD_ROOT, new AddRootControl());
        registerControl(RootManagerService.REMOVE_ROOT, new RemoveRootControl());
        registerControl(RootManagerService.ROOTS, new RootsControl());
        registerInterface(RootManagerService.INSTANCE);
        knownRoots = new LinkedHashSet<String>();
    }
    
    Set<String> getKnownUserRoots() {
        return knownRoots;
    }

    private ComponentAddress getDefaultServiceAddress() throws ServiceUnavailableException {
        if (defaultService == null) {
            ServiceManager manager = getLookup().get(ServiceManager.class);
            if (manager == null) {
                throw new ServiceUnavailableException();
            }
            ComponentAddress[] services = manager.findAllServices(RootManagerService.INSTANCE);
            defaultService = services[services.length - 1];
        }
        return defaultService;
    }

    private class AddRootControl extends AbstractAsyncControl {

        @Override
        protected Call processInvoke(Call call) throws Exception {
            ControlAddress to = ControlAddress.create(getDefaultServiceAddress(), RootManagerService.ADD_ROOT);
            return Call.createCall(to, getAddress(), call.getTimecode(), call.getArgs());
        }

        @Override
        protected Call processResponse(Call call) throws Exception {
            if (call.getType() == Call.Type.RETURN) {
                // sucessfully added
                Call active = getActiveCall();
                knownRoots.add(active.getArgs().get(0).toString());
            }
            return Call.createReturnCall(getActiveCall(), call.getArgs());
        }

        @Override
        public ControlInfo getInfo() {
            return RootManagerService.ADD_ROOT_INFO;
        }
    }

    private class RootsControl extends AbstractAsyncControl {

        @Override
        protected Call processInvoke(Call call) throws Exception {
            ControlAddress to = ControlAddress.create(getDefaultServiceAddress(), RootManagerService.ROOTS);
            return Call.createCall(to, getAddress(), call.getTimecode(), call.getArgs());
        }

        @Override
        protected Call processResponse(Call call) throws Exception {
            return Call.createReturnCall(getActiveCall(), call.getArgs());
        }

        @Override
        public ControlInfo getInfo() {
            return RootManagerService.ROOTS_INFO;
        }
    }

    private class RemoveRootControl extends AbstractAsyncControl {

        @Override
        protected Call processInvoke(final Call call) throws Exception {

            Object ret = DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Confirmation("Remove root " + call.getArgs().get(0).toString()));
            if (ret == NotifyDescriptor.YES_OPTION) {
                ControlAddress to = ControlAddress.create(getDefaultServiceAddress(), RootManagerService.REMOVE_ROOT);
                return Call.createCall(to, getAddress(), call.getTimecode(), call.getArgs());
            } else {
                return Call.createErrorCall(call, CallArguments.EMPTY);
            }

        }

        @Override
        protected Call processResponse(Call call) throws Exception {
            if (call.getType() == Call.Type.RETURN) {
                // sucessfully added
                Call active = getActiveCall();
                knownRoots.remove(active.getArgs().get(0).toString());
            }
            return Call.createReturnCall(getActiveCall(), call.getArgs());
        }

        @Override
        public ControlInfo getInfo() {
            return RootManagerService.REMOVE_ROOT_INFO;
        }
    }
}
