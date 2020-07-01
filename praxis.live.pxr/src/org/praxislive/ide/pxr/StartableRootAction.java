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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToggleButton;
import org.praxislive.core.Value;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.protocols.StartableProtocol;
import org.praxislive.core.types.PBoolean;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.core.api.ValuePropertyAdaptor;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.praxislive.base.Binding;

/**
 *
 */
class StartableRootAction extends AbstractAction
        implements ContextAwareAction, Presenter.Toolbar, LookupListener {

    private final static String RESOURCE_DIR = "org/praxislive/ide/pxr/resources/";

    private Lookup.Result<PXRRootContext> result;
    private PXRRootProxy root;
    private JToggleButton button;
    private boolean running;
    private ValuePropertyAdaptor.ReadOnly runningAdaptor;

    StartableRootAction() {
        this(Utilities.actionsGlobalContext());
    }

    StartableRootAction(Lookup context) {
        super("", ImageUtilities.loadImageIcon(RESOURCE_DIR + "play.png", true));
        this.result = context.lookupResult(PXRRootContext.class);
        this.result.addLookupListener(this);
        putValue(SELECTED_KEY, Boolean.FALSE);
        setEnabled(false);
        resultChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            ControlAddress to;
            if (getValue(SELECTED_KEY) == Boolean.TRUE) {
                to = ControlAddress.of(root.getAddress(), StartableProtocol.START);
            } else {
                to = ControlAddress.of(root.getAddress(), StartableProtocol.STOP);
            }
            root.getHelper().send(to, List.of(), null);
        } catch (HubUnavailableException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new StartableRootAction(actionContext);
    }

    @Override
    public Component getToolbarPresenter() {
        if (button == null) {
            button = new JToggleButton(this);
//            button.setIcon(ImageUtilities.loadImageIcon(
//                    RESOURCE_DIR + "pxr-toggle-idle.png", true));
//            button.setSelectedIcon(ImageUtilities.loadImageIcon(
//                    RESOURCE_DIR + "pxr-toggle-active.png", true));
        }
        return button;
    }

    @Override
    public final void resultChanged(LookupEvent ev) {
        if (root != null) {
            reset();
        }
        Collection<? extends PXRRootContext> roots = result.allInstances();
        if (roots.isEmpty()) {
            return;
        }
        setup(roots.iterator().next().getRoot());
    }

    private void reset() {
        root = null;
        putValue(SELECTED_KEY, Boolean.FALSE);
        setEnabled(false);
        root.getHelper().unbind(ControlAddress.of(root.getAddress(),
                StartableProtocol.IS_RUNNING), runningAdaptor);
        runningAdaptor = null;
    }

    private void setup(PXRRootProxy root) {
        this.root = root;
        if (!isStartable(root)) {
            return;
        }
        setEnabled(true);
        runningAdaptor = new ValuePropertyAdaptor.ReadOnly(this,
                StartableProtocol.IS_RUNNING, false, Binding.SyncRate.Low);
        runningAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                PBoolean selected = PBoolean.from((Value) pce.getNewValue())
                        .orElse(PBoolean.FALSE);
                putValue(SELECTED_KEY, selected.value());
            }
        });
        root.getHelper().bind(ControlAddress.of(root.getAddress(),
                StartableProtocol.IS_RUNNING), runningAdaptor);
    }

    private boolean isStartable(PXRRootProxy root) {
        return root.getInfo().hasProtocol(StartableProtocol.class);
    }

}
