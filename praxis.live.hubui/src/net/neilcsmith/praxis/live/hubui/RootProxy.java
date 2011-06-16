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

package net.neilcsmith.praxis.live.hubui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.InterfaceDefinition;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.interfaces.ComponentInterface;
import net.neilcsmith.praxis.core.interfaces.RootManagerService;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.interfaces.StartableInterface;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.gui.ControlBinding;
import net.neilcsmith.praxis.live.core.api.HubUnavailableException;
import net.neilcsmith.praxis.live.util.ArgumentPropertyAdaptor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class RootProxy {

    private final static Logger LOG = Logger.getLogger(RootProxy.class.getName());
    private final static String RESOURCE_DIR = "net/neilcsmith/praxis/live/hubui/resources/";

    private String id;
    private ComponentAddress address;
    private ControlAddress infoAddress;
    private ArgumentPropertyAdaptor.ReadOnly infoAdaptor;
    private ControlAddress runningAddress;
    private ArgumentPropertyAdaptor.ReadOnly runningAdaptor;
    private boolean startable;
    private boolean running;
    private Delegate node;

    RootProxy(String id) {
        this.id = id;
        address = ComponentAddress.create("/" + id);
        infoAddress = ControlAddress.create(address, ComponentInterface.INFO);
        infoAdaptor = new ArgumentPropertyAdaptor.ReadOnly(null, ComponentInterface.INFO, true, ControlBinding.SyncRate.Low);
        infoAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateInfo();
            }
        });
        HubUIHelper.getDefault().bind(infoAddress, infoAdaptor);
        runningAddress = ControlAddress.create(address, StartableInterface.IS_RUNNING);
        updateInfo();
    }

    private void updateInfo() {
        LOG.info("RootProxy received Root Info");
        try {
            ComponentInfo info = ComponentInfo.coerce(infoAdaptor.getValue());
            for (InterfaceDefinition i : info.getInterfaces()) {
                LOG.info("Found Interface : " + i.toString());
                if (StartableInterface.INSTANCE.equals(i)) {
                    setStartable(true);
                    LOG.info("Found Startable");
                    return;
                }
            }
        } catch (ArgumentFormatException ex) {
            // fall through
        }
        setStartable(false);
        refreshNode();
    }

    private void updateRunning() {
        try {
            PBoolean running = PBoolean.coerce(runningAdaptor.getValue());
            this.running = running.value();
        } catch (ArgumentFormatException ex) {
            running = false;
        }
        refreshNode();
    }

    private void setStartable(boolean startable) {
        if (startable) {
            this.startable = true;
            if (runningAdaptor == null) {
                runningAdaptor = new ArgumentPropertyAdaptor.ReadOnly(null, "is-running", true, ControlBinding.SyncRate.Low);
                runningAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        updateRunning();
                    }
                });
            }
            HubUIHelper.getDefault().bind(runningAddress, runningAdaptor);
        } else {
            this.startable = false;
            if (runningAdaptor != null) {
                HubUIHelper.getDefault().unbind(runningAdaptor);
            }
        }
    }

    void dispose() {
        HubUIHelper.getDefault().unbind(infoAdaptor);
        if (runningAdaptor != null) {
            HubUIHelper.getDefault().unbind(runningAdaptor);
        }
    }

    public String getID() {
        return id;
    }

    public Node getNodeDelegate() {
        if (node == null) {
            node = new Delegate(this);
        }
        return node;
    }

    private void refreshNode() {
        if (node != null) {
            node.refresh();
        }
    }


    private static class Delegate extends AbstractNode {

        private RootProxy root;
        private StartableAction startAction;
        private StartableAction stopAction;
        private DeleteAction deleteAction;

        private Delegate(RootProxy root) {
            super(Children.LEAF, Lookups.singleton(root));
            this.root = root;
            startAction = new StartableAction("Start", true);
            stopAction = new StartableAction("Stop", false);
            deleteAction = new DeleteAction();
            refresh();
        }

        private void refresh() {
            if (root.startable) {
                if (root.running) {
                    setDisplayName(root.id + " [active]");
                    setIconBaseWithExtension(RESOURCE_DIR + "root_active.png");
                    startAction.setEnabled(false);
                    stopAction.setEnabled(true);
                } else {
                    setDisplayName(root.id + " [idle]");
                    setIconBaseWithExtension(RESOURCE_DIR + "root_idle.png");
                    stopAction.setEnabled(false);
                    startAction.setEnabled(true);
                }
            } else {
                setDisplayName(root.id);
                setIconBaseWithExtension(RESOURCE_DIR + "root.png");
                startAction.setEnabled(false);
                stopAction.setEnabled(false);
            }
        }

        @Override
        public Action[] getActions(boolean context) {
            if (root.id.startsWith(HubProxy.SYSTEM_PREFIX)) {
                return new Action[0];
            }
            if (root.startable) {
                return new Action[] {
                  startAction,
                  stopAction,
                  null,
                  deleteAction
                };
            } else {
                return new Action[] {deleteAction};
            }
        }




        private class StartableAction extends AbstractAction {

            private ControlAddress to;

            private StartableAction(String name, boolean start) {
                super(name);
                to = ControlAddress.create(root.address,
                        start ? StartableInterface.START : StartableInterface.STOP);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    HubUIHelper.getDefault().send(to, CallArguments.EMPTY, null);
                } catch (HubUnavailableException ex) {
                    Exceptions.printStackTrace(ex);
                }
                setEnabled(false);
            }
        }

        private class DeleteAction extends AbstractAction {

            private DeleteAction() {
                super("Delete");
            }


            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    HubUIHelper.getDefault().send(RootManagerService.INSTANCE,
                            RootManagerService.REMOVE_ROOT,
                            CallArguments.create(PString.valueOf(root.id)), null);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                setEnabled(false);
            }

            

        }



    }




}
