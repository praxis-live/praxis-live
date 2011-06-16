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

package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.BorderLayout;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.spi.navigator.NavigatorLookupHint;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
// @TODO never have a null Bridge?
public class GraphNavigator implements NavigatorPanel {

    public final static NavigatorLookupHint HINT = new NavigatorLookupHint() {

        @Override
        public String getContentType() {
            return "x-praxis-root/graph";
        }
    };

    private JPanel container;
    private Lookup.Result<Bridge> result;
    private Listener listener;
    private InstanceContent content;
    private AbstractLookup lookup;
    private Bridge bridge;

    public GraphNavigator() {
        container = new JPanel();
        container.setLayout(new BorderLayout());
        listener = new Listener();
        content = new InstanceContent();
        content.add(HINT);
        lookup = new AbstractLookup(content);
    }

    @Override
    public String getDisplayName() {
        return "Navigator";
    }

    @Override
    public String getDisplayHint() {
        return getDisplayName();
    }

    @Override
    public JComponent getComponent() {
        return container;
    }

    @Override
    public void panelActivated(Lookup context) {
//        result = context.lookupResult(Bridge.class);
        result = Utilities.actionsGlobalContext().lookupResult(Bridge.class);
        result.addLookupListener(listener);
        configure();
    }

    @Override
    public void panelDeactivated() {
        container.removeAll();
        result.removeLookupListener(listener);
        result = null;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private void configure() {
        Collection<? extends Bridge> instances = result.allInstances();
        if (instances.isEmpty()) {
            container.removeAll();
            if (bridge != null) {
                content.remove(bridge);
                bridge = null;
            }
        } else {
            Bridge br = instances.iterator().next();
            if (br == bridge) {
                return;
            }
            container.removeAll();
            if (bridge != null) {
                content.remove(bridge);
            }
            container.add(br.getNavigatorComponent());
            content.add(br);
            bridge = br;
        }
        container.revalidate();
    }

    private class Listener implements LookupListener {

        @Override
        public void resultChanged(LookupEvent ev) {
            configure();
        }

    }

    public static interface Bridge {

        public JComponent getNavigatorComponent();

    }



}
