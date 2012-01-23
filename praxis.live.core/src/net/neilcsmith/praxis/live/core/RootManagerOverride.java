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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Call;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.PacketRouter;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.RootManagerService;
import net.neilcsmith.praxis.core.interfaces.ServiceManager;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.impl.AbstractAsyncControl;
import net.neilcsmith.praxis.impl.AbstractControl;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import net.neilcsmith.praxis.live.core.api.Task;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class RootManagerOverride extends AbstractSwingRoot {

    private final static Logger LOG = Logger.getLogger(RootManagerOverride.class.getName());
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

//    private class RemoveRootControl extends AbstractAsyncControl {
//
//        @Override
//        protected Call processInvoke(final Call call) throws Exception {
//
//            Object ret = DialogDisplayer.getDefault().notify(
//                    new NotifyDescriptor.Confirmation("Remove root " + call.getArgs().get(0).toString()));
//            if (ret == NotifyDescriptor.YES_OPTION) {
//                ControlAddress to = ControlAddress.create(getDefaultServiceAddress(), RootManagerService.REMOVE_ROOT);
//                return Call.createCall(to, getAddress(), call.getTimecode(), call.getArgs());
//            } else {
//                return Call.createErrorCall(call, CallArguments.EMPTY);
//            }
//
//        }
//
//        @Override
//        protected Call processResponse(Call call) throws Exception {
//            if (call.getType() == Call.Type.RETURN) {
//                // sucessfully added
//                Call active = getActiveCall();
//                knownRoots.remove(active.getArgs().get(0).toString());
//            }
//            return Call.createReturnCall(getActiveCall(), call.getArgs());
//        }
//
//        @Override
//        public ControlInfo getInfo() {
//            return RootManagerService.REMOVE_ROOT_INFO;
//        }
//    }
    private class RemoveRootControl extends AbstractControl {

        private Map<String, List<Call>> pending;
        private Map<Integer, String> forwarded;

        private RemoveRootControl() {

            pending = new HashMap<String, List<Call>>();
            forwarded = new HashMap<Integer, String>();
        }

        @Override
        public void call(Call call, PacketRouter router) throws Exception {
            switch (call.getType()) {
                case INVOKE:
                case INVOKE_QUIET:
                    processInvoke(call, router);
                    break;
                case RETURN:
                    processResponse(call, router, false);
                    break;
                default:
                    processResponse(call, router, true);
                    break;
            }
        }

        private void processInvoke(Call call, PacketRouter router) throws Exception {
            String rootID = call.getArgs().get(0).toString();
            List<Call> calls = pending.get(rootID);
            if (calls == null) {
                LOG.log(Level.FINE, "No pending calls found for root removal /{0}", rootID);
                List<Task> tasks = Utils.findRootDeletionTasks("Deleting /" + rootID,
                        Collections.singleton(rootID));
                if (tasks.isEmpty()) {
                    LOG.log(Level.FINE, "No tasks found for root removal /{0}", rootID);
                    Object ret = DialogDisplayer.getDefault().notify(
                            new NotifyDescriptor.Confirmation("Remove root " + call.getArgs().get(0).toString(),
                                    "Remove Root?",
                                    NotifyDescriptor.YES_NO_OPTION));
                    if (ret == NotifyDescriptor.YES_OPTION) {
                        forwardCall(rootID, call, router);
                    } else {
                        router.route(Call.createErrorCall(call, CallArguments.EMPTY));
                        return; // don't allow call to be added to pending
                    }

                } else {
                    if (tasks.size() > 1) {
                        LOG.log(Level.WARNING, "More than one deletion task for root /{0}\nOnly first task will be run", rootID);
                    }
                    LOG.log(Level.FINE, "Starting root deletion task");
                    Task task = tasks.get(0);
                    task.execute();
                    if (task.getState() == Task.State.RUNNING) {
                        LOG.log(Level.FINE, "Task still running - add PCL");
                        task.addPropertyChangeListener(new TaskListener(task, rootID));
                    } else if (task.getState() == Task.State.COMPLETED) {
                        LOG.log(Level.FINE, "Task completed synchronously - forwarding call");
                        forwardCall(rootID, call, router);
                    } else {
                        LOG.log(Level.FINE, "Synchronous task error");
                        router.route(Call.createErrorCall(call, CallArguments.EMPTY));
                        return; // don't allow call to be added to pending
                    }
                }
                calls = new ArrayList<Call>(1);
                calls.add(call);
                pending.put(rootID, calls);
            } else {
                LOG.log(Level.FINE, "Pending call found for root removal /{0}", rootID);
                calls.add(call);
            }


        }

        private void forwardCallFromTask(String rootID) throws Exception {
            LOG.log(Level.FINE, "Task Completed OK - forwarding call to remove /{0}", rootID);
            List<Call> inbound = pending.get(rootID);
            PacketRouter router = getPacketRouter();
            forwardCall(rootID, inbound.get(0), router);
        }

        private void forwardCall(String rootID, Call call, PacketRouter router) throws Exception {
            ControlAddress to = ControlAddress.create(getDefaultServiceAddress(), RootManagerService.REMOVE_ROOT);
            Call forward = Call.createCall(to, getAddress(), call.getTimecode(), call.getArgs());
            forwarded.put(forward.getMatchID(), rootID);
            router.route(forward);
        }

        private void processResponse(Call call, PacketRouter router, boolean error) {
            String rootID = forwarded.remove(call.getMatchID());
            List<Call> inbound = pending.remove(rootID);
            for (Call in : inbound) {
                Call response;
                if (error) {
                    response = Call.createErrorCall(in, call.getArgs());
                } else {
                    response = Call.createReturnCall(in, call.getArgs());
                }
                router.route(response);
            }
        }

        private void taskError(String rootID) {
            List<Call> inbound = pending.remove(rootID);
            PacketRouter router = getPacketRouter();
            for (Call in : inbound) {
                router.route(Call.createErrorCall(in, CallArguments.EMPTY));
            }
        }

        @Override
        public ControlInfo getInfo() {
            return RootManagerService.REMOVE_ROOT_INFO;
        }

        private class TaskListener implements PropertyChangeListener {

            private String rootID;
            private Task task;

            private TaskListener(Task task, String rootID) {
                this.task = task;
                this.rootID = rootID;
            }

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (task.getState() == Task.State.COMPLETED) {
                    try {
                        forwardCallFromTask(rootID);
                        return;
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                taskError(rootID);

            }
        }
    }
}
