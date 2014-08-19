/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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

import java.awt.EventQueue;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.properties.PraxisProperty;
import net.neilcsmith.praxis.live.model.ComponentProxy;
import net.neilcsmith.praxis.live.model.ProxyException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXRProxyNode extends AbstractNode {

//    private final static Logger LOG = Logger.getLogger(PXRProxyNode.class.getName());
    private final PXRComponentProxy component;

    private Action[] actions;
    private Image icon;
    boolean ignore;

    PXRProxyNode(final PXRComponentProxy component) {
        this(component,
                component instanceof PXRContainerProxy
                ? new ContainerChildren((PXRContainerProxy) component) : Children.LEAF,
                new ProxyLookup(Lookups.singleton(component), component.getLookup()));

    }

    private PXRProxyNode(PXRComponentProxy component, Children children, Lookup lookup) {
        super(children, lookup);
        this.component = component;
        setName(component.getAddress().getID());
        refreshProperties();
        refreshActions();
    }

    final void refreshProperties() {
        setSheet(createSheetOnEQ());
    }

    final void refreshActions() {
        List<Action> lst = new ArrayList<>();
        List<Action> triggers = component.getTriggerActions();
        if (!triggers.isEmpty()) {
            lst.addAll(triggers);
            lst.add(null);
        }
        List<Action> prop = component.getPropertyActions();
        if (!prop.isEmpty()) {
            lst.addAll(prop);
            lst.add(null);
        }
        lst.add(component.getEditorAction());
        actions = lst.toArray(new Action[lst.size()]);
    }

    final void refreshChildren() {
        Children chs = getChildren();
        assert chs instanceof ContainerChildren;
        if (chs instanceof ContainerChildren) {
            ((ContainerChildren) chs).update();
        }
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Action[] getActions(boolean context) {
        return actions.clone();
    }

    @Override
    public Action getPreferredAction() {
        return component.getEditorAction();
    }

    @Override
    public boolean canDestroy() {
        return canDestroyImpl();
    }

    private boolean canDestroyImpl() {
//        assert EventQueue.isDispatchThread();
        return component.getParent() != null;
    }

    @Override
    public void destroy() throws IOException {
        if (EventQueue.isDispatchThread()) {
            destroyImpl();
        } else {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    destroyImpl();
                }
            });
        }
    }

    private void destroyImpl() {
        assert EventQueue.isDispatchThread();
        try {
            PXRContainerProxy container = component.getParent();
            container.removeChild(container.getChildID(component), null);
//            component.dispose(); //@TODO should be done from container callback?
        } catch (ProxyException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public Image getIcon(int type) {
        if (icon == null) {
            icon = Components.getIcon(component.getType());
        }
        return icon;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    private Sheet createSheetOnEQ() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set props = Sheet.createPropertiesSet();
        sheet.put(props);
        for (PraxisProperty<?> proxyProp : component.getProxyProperties()) {
            proxyProp.setHidden(true);
            props.put(proxyProp);
        }

        for (String id : component.getPropertyIDs()) {
            Node.Property<?> prop = component.getProperty(id);
            if (prop.canWrite() && prop instanceof BoundArgumentProperty) {
                BoundArgumentProperty bap = (BoundArgumentProperty) prop;
                if (bap.getArgumentType()
                        == PBoolean.class) {
                    prop = new BooleanPropertyWrapper(bap);
                }
            }
            props.put(prop);
        }
        return sheet;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(component.getType().toString());
    }

    void propertyChange(String property, Object oldValue, Object newValue) {
        firePropertyChange(property, oldValue, newValue);
    }

//    class ComponentPropListener implements PropertyChangeListener {
//
//        @Override
//        public void propertyChange(PropertyChangeEvent evt) {
//            String property = evt.getPropertyName();
////            if (!component.isProxiedProperty(property)) {
//                firePropertyChange(property, null, null);
////            }
//            
//        }
//
//    }
    static class ContainerChildren extends Children.Keys<String> {

        final PXRContainerProxy container;

        private ContainerChildren(PXRContainerProxy container) {
            this.container = container;
            setKeys(container.getChildIDs());
        }

        @Override
        protected Node[] createNodes(String key) {
            ComponentProxy component = container.getChild(key);
            if (component != null) {
                return new Node[]{component.getNodeDelegate()};
            } else {
                return new Node[0];
            }
        }

        private void update() {
            setKeys(container.getChildIDs());
        }

    }

}
