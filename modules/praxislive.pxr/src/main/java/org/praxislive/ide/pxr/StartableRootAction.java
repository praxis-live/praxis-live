/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToggleButton;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.protocols.StartableProtocol;
import org.praxislive.core.types.PBoolean;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;

/**
 *
 */
@ActionID(category = "PXR", id = "org.praxislive.ide.pxr.StartableRootAction")
@ActionRegistration(
        displayName = "#CTL_StartableRootAction",
        lazy = false
)
@Messages("CTL_StartableRootAction=Start")
public class StartableRootAction extends AbstractAction
        implements ContextAwareAction, Presenter.Toolbar {

    private final static String RESOURCE_DIR = "org/praxislive/ide/pxr/resources/";

    private final PropertyChangeListener baseListener;
    private final Lookup.Result<ActionEditorContext> result;
    
    private PXRRootProxy root;
    private PropertyChangeListener rootListener;
    private JToggleButton button;

    public StartableRootAction() {
        this(Utilities.actionsGlobalContext());
    }

    private StartableRootAction(Lookup context) {
        super("", ImageUtilities.loadImageIcon(RESOURCE_DIR + "play.png", true));
        this.result = context.lookupResult(ActionEditorContext.class);
        this.result.addLookupListener(this::resultChanged);
        this.baseListener = this::propertyChange;
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
        }
        return button;
    }

    private void resultChanged(LookupEvent ev) {
        if (root != null) {
            reset();
        }
        Collection<? extends ActionEditorContext> roots = result.allInstances();
        if (roots.isEmpty()) {
            return;
        }
        setup(roots.iterator().next().root());
    }

    private void propertyChange(PropertyChangeEvent pce) {
        String prop = pce.getPropertyName();
        if (prop == null || StartableProtocol.IS_RUNNING.equals(prop)) {
            update();
        }
    }

    private void update() {
        PBoolean active = Optional.ofNullable(root)
                .map(r -> r.getProperty(StartableProtocol.IS_RUNNING))
                .map(BoundArgumentProperty::getValue)
                .flatMap(PBoolean::from)
                .orElse(PBoolean.FALSE);
        putValue(SELECTED_KEY, active.value());
    }

    private void reset() {
        putValue(SELECTED_KEY, Boolean.FALSE);
        setEnabled(false);
        if (root != null) {
            if (rootListener != null) {
                root.removePropertyChangeListener(rootListener);
            }
            root = null;
            rootListener = null;
        }
    }

    private void setup(PXRRootProxy root) {
        this.root = root;
        if (!isStartable(root)) {
            return;
        }
        setEnabled(true);
        update();
        rootListener = WeakListeners.propertyChange(baseListener, root);
        root.addPropertyChangeListener(rootListener);
    }

    private boolean isStartable(PXRRootProxy root) {
        return root.getInfo().hasProtocol(StartableProtocol.class);
    }

}
