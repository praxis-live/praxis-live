/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.pxr;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.live.pxr.api.ComponentProxy;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
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

    PXRProxyNode(PXRComponentProxy component, PXRDataObject dob) {
        super(component instanceof PXRContainerProxy ?
            new ContainerChildren((PXRContainerProxy) component) : Children.LEAF,
                new ProxyLookup(Lookups.singleton(component), dob.getLookup()));
        this.component = component;
        component.addPropertyChangeListener(new ComponentPropListener());
    }

    @Override
    public String getDisplayName() {
        ComponentAddress address = component.getAddress();
        return address.getComponentID(address.getDepth() - 1) + " " + component.getType();
    }

    @Override
    public Action[] getActions(boolean context) {
        return component.getActions();
    }

    @Override
    protected Sheet createSheet() {
        // this gets called outside of EQ by propertysheet!
        if (EventQueue.isDispatchThread()) {
            return createSheetOnEQ();
        } else {
            final Sheet[] holder = new Sheet[1];
            try {
                EventQueue.invokeAndWait(new Runnable() {

                    @Override
                    public void run() {
                        holder[0] = createSheetOnEQ();
                    }
                });
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
                return super.createSheet();
            }
            return holder[0];
        }
    }

    private Sheet createSheetOnEQ() {
        Sheet sheet = Sheet.createDefault();
        Sheet.Set props = Sheet.createPropertiesSet();
        sheet.put(props);
        for (String id : component.getPropertyIDs()) {
            props.put(component.getProperty(id));
        }
        return sheet;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(component.getType().toString());
    }




    private class ComponentPropListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String property = evt.getPropertyName();
            if (!component.isIgnoredProperty(property)) {
                firePropertyChange(property, null, null);
            }
            
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
            if (ContainerProxy.PROP_CHILDREN.equals(evt.getPropertyName())) {
                setKeys(container.getChildIDs());
            }
        }

    }


}
