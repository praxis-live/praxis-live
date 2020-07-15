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

import java.awt.EventQueue;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.praxislive.core.types.PBoolean;
import org.praxislive.ide.properties.PraxisProperty;
import org.praxislive.ide.model.ComponentProxy;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.ControlInfo;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.praxislive.ide.components.api.Components;
import org.praxislive.ide.components.api.Icons;

/**
 *
 */
class PXRProxyNode extends AbstractNode {

//    private final static Logger LOG = Logger.getLogger(PXRProxyNode.class.getName());
    private final PXRComponentProxy component;
    private final Action preferredAction;

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
        this.preferredAction = new PreferredAction();
        setName(component.getAddress().componentID());
        refreshProperties();
        refreshActions();
    }

    final void refreshProperties() {
        setSheet(createSheetOnEQ());
    }

    final void refreshActions() {
        List<Action> lst = new ArrayList<>();
        lst.add(component.getEditorAction());
        List<Action> triggers = component.getTriggerActions();
        if (!triggers.isEmpty()) {
            lst.add(null);
            lst.addAll(triggers);
        }
        List<Action> prop = component.getPropertyActions();
        if (!prop.isEmpty()) {
            lst.add(null);
            lst.addAll(prop);
        }
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
        return preferredAction;
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
        PXRContainerProxy container = component.getParent();
        container.removeChild(container.getChildID(component), null);
    }

    @Override
    public Image getIcon(int type) {
        if (icon == null) {
            var cmps = component.getRoot().getProject().getLookup().lookup(Components.class);
            if (cmps != null) {
                icon = cmps.getIcon(component.getType());
            } else {
                icon = Icons.getIcon(component.getType());
            }
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

        for (String input : inputControls()) {
            props.put(new InputPropertyWrapper(component, input));
        }

        for (Action action : component.getTriggerActions()) {
            props.put(new ActionPropertyWrapper(action));
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

    private List<String> inputControls() {
        ComponentInfo info = component.getInfo();
        List<String> inputs = new ArrayList<>();
        for (String id : info.controls()) {
            ControlInfo ci = info.controlInfo(id);
            if (ci.controlType() == ControlInfo.Type.Function
                    && ci.properties().getString("input-port", "").equals(id)) {
                inputs.add(id);
            }
        }
        return inputs;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(component.getType().toString());
    }

    void propertyChange(String property, Object oldValue, Object newValue) {
        firePropertyChange(property, oldValue, newValue);
    }

    private class PreferredAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if ((e.getModifiers() & ActionEvent.SHIFT_MASK) != 0) {
                Action codeAction = component.getCodeEditAction();
                if (codeAction != null) {
                    codeAction.actionPerformed(e);
                    return;
                }
            }
            component.getEditorAction().actionPerformed(e);
        }

    }

    static class ContainerChildren extends Children.Keys<String> {

        final PXRContainerProxy container;

        private ContainerChildren(PXRContainerProxy container) {
            this.container = container;
            update();
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
            setKeys(container.children().toArray(String[]::new));
        }

    }

}
