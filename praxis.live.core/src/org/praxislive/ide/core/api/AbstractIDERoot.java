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
package org.praxislive.ide.core.api;

import java.awt.EventQueue;
import javax.swing.Timer;
import org.praxislive.base.AbstractRootContainer;
import org.praxislive.base.BindingContextControl;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.Lookup;

/**
 *
 */
public abstract class AbstractIDERoot extends AbstractRootContainer {
    
    
    private BindingContextControl bindings;
    private Lookup lookup;

    @Override
    public Lookup getLookup() {
        return lookup == null ? super.getLookup() : lookup;
    }
    
    @Override
    protected final void activating() {
        bindings = new BindingContextControl(ControlAddress.of(getAddress(), "_bindings"),
                getExecutionContext(),
                getRouter());
        registerControl("_bindings", bindings);
        lookup = Lookup.of(super.getLookup(), bindings);
        var delegate = new SwingDelegate();
        attachDelegate(delegate);
        delegate.start();
    }

    private class SwingDelegate extends Delegate {

        private Timer timer;

        private void start() {
            EventQueue.invokeLater(() -> {
                setRunning();
                timer = new Timer(50, e -> update());
                timer.start();
            });
        }

        private void update() {
            boolean ok = doUpdate(getRootHub().getClock().getTime());
            if (!ok) {
                timer.stop();
                detachDelegate(this);
            }
        }

    }
    
}
