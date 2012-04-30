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
package net.neilcsmith.praxis.live.pxr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class RootConfigAction extends AbstractAction
        implements ContextAwareAction, LookupListener {
    
    private final static String RESOURCE_DIR = "net/neilcsmith/praxis/live/pxr/resources/";
    
    private Lookup.Result<PXRRootContext> result;
    private RootProxy root;
   
    RootConfigAction() {
        this(Utilities.actionsGlobalContext());
    }
    
    RootConfigAction(Lookup context) {
        super("", ImageUtilities.loadImageIcon(RESOURCE_DIR + "properties.png", true));
        this.result = context.lookupResult(PXRRootContext.class);
        this.result.addLookupListener(this);
        setEnabled(false);
        resultChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (root != null) {
            Action action = root.getNodeDelegate().getPreferredAction();
            action.actionPerformed(ae);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new RootConfigAction(actionContext);
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
        setEnabled(false);
    }
    
    private void setup(RootProxy root) {
        this.root = root;
        setEnabled(true);
    }
    
    
}
