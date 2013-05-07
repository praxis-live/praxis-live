/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ArgumentFormatException;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class PXRProxyNode extends AbstractNode {

    private final static Logger LOG = Logger.getLogger(PXRProxyNode.class.getName());

    private PXRComponentProxy component;
    private Image icon;
    
    boolean ignore;

    PXRProxyNode(PXRComponentProxy component, PXRDataObject dob) {
        super(component instanceof PXRContainerProxy ?
            new ContainerChildren((PXRContainerProxy) component) : Children.LEAF,
                new ProxyLookup(Lookups.singleton(component), dob.getLookup()));
        this.component = component;
        setName(component.getAddress().getID());
        component.addPropertyChangeListener(new ComponentPropListener());
        refreshProperties();
    }
    
    final void refreshProperties() {
        setSheet(createSheetOnEQ());
    }

    @Override
    public String getDisplayName() {
        return component.getAddress().getID();
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> triggers = component.getTriggerActions();
        Action[] actions;
        if (triggers.isEmpty()) {
            actions = new Action[]{component.getEditorAction()};
        } else {
            int size = triggers.size() + 2;
            actions = triggers.toArray(new Action[size]);
            actions[size - 1] = component.getEditorAction();
            return actions;
        }
        return actions;
    }

    @Override
    public Action getPreferredAction() {
        return component.getEditorAction();
    }

    @Override
    public boolean canDestroy() {
//        if (EventQueue.isDispatchThread()) {
            return canDestroyImpl();
//        } else {
//            try {
//                final boolean[] res = new boolean[0];
//                EventQueue.invokeAndWait(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        res[0] = canDestroyImpl();
//                    }
//                });
//                return res[0];
//            } catch (Exception ex) {
//                Exceptions.printStackTrace(ex);
//            }
//        }
//        return false;
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
            component.dispose(); //@TODO should be done from container callback?
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
    
    
    
//    @Override
//    protected Sheet createSheet() {
//        // this gets called outside of EQ by propertysheet!
//        if (EventQueue.isDispatchThread()) {
//            return createSheetOnEQ();
//        } else {
////            final Sheet[] holder = new Sheet[1];
////            try {
////                EventQueue.invokeAndWait(new Runnable() {
////
////                    @Override
////                    public void run() {
////                        holder[0] = createSheetOnEQ();
////                    }
////                });
////            } catch (Exception ex) {
////                Exceptions.printStackTrace(ex);
////                return super.createSheet();
////            }
////            return holder[0];
//            EventQueue.invokeLater(new Runnable() {
//
//                @Override
//                public void run() {
//                    setSheet(createSheetOnEQ());
//                }
//            });
//            return super.createSheet();
//        }
//    }

    private Sheet createSheetOnEQ() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set props = Sheet.createPropertiesSet();
        sheet.put(props);
        for (String id : component.getPropertyIDs()) {
            Node.Property<?> prop = component.getProperty(id);
            if (prop.canWrite() && prop instanceof BoundArgumentProperty) {
                BoundArgumentProperty bap = (BoundArgumentProperty) prop;
                if (bap.getArgumentType()
                        == PBoolean.class) {
                    prop = new BooleanWrapper(bap);
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




    class ComponentPropListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String property = evt.getPropertyName();
            if (!component.isIgnoredProperty(property)) {
                firePropertyChange(property, null, null);
            }
            
        }

    }

    private static class BooleanWrapper extends Node.Property<Boolean> {

        private final BoundArgumentProperty wrapped;
        
        private BooleanWrapper(BoundArgumentProperty wrapped) {
            super(Boolean.class);
            this.wrapped = wrapped;
        }
        
        @Override
        public boolean canRead() {
            return wrapped.canRead();
        }

        @Override
        public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
            try {
                return PBoolean.coerce(wrapped.getValue()).value();
            } catch (ArgumentFormatException ex) {
                return false;
            }
        }

        @Override
        public boolean canWrite() {
            return wrapped.canWrite();
        }

        @Override
        public void setValue(Boolean val) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            wrapped.setValue(val ? PBoolean.TRUE : PBoolean.FALSE);
        }

        @Override
        public String getName() {
            return wrapped.getName();
        }

        @Override
        public boolean supportsDefaultValue() {
            return wrapped.supportsDefaultValue();
        }

        @Override
        public void restoreDefaultValue() {
            wrapped.restoreDefaultValue();
        }

        @Override
        public String getHtmlDisplayName() {
            return wrapped.getHtmlDisplayName();
        }

        @Override
        public String getDisplayName() {
            return wrapped.getDisplayName();
        }
        
        
        
    }

    private static class ContainerChildren extends Children.Keys<String>
            implements PropertyChangeListener {

        PXRContainerProxy container;

        private ContainerChildren(PXRContainerProxy container) {
            this.container = container;
            setKeys(container.getChildIDs());
            container.addPropertyChangeListener(this);
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

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (container.ignore) {
                return;
            }
            if (ContainerProxy.PROP_CHILDREN.equals(evt.getPropertyName())) {
                setKeys(container.getChildIDs());
            }
        }

    }


}
