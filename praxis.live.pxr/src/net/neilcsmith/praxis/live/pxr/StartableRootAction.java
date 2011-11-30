/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JToggleButton;
import net.neilcsmith.praxis.core.Argument;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.InterfaceDefinition;
import net.neilcsmith.praxis.core.interfaces.StartableInterface;
import net.neilcsmith.praxis.core.types.PBoolean;
import net.neilcsmith.praxis.gui.ControlBinding.SyncRate;
import net.neilcsmith.praxis.live.core.api.HubUnavailableException;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.util.ArgumentPropertyAdaptor;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class StartableRootAction extends AbstractAction
        implements ContextAwareAction, Presenter.Toolbar, LookupListener {
    
    private final static String RESOURCE_DIR = "net/neilcsmith/praxis/live/pxr/resources/";
    
    private Lookup.Result<PXRRootContext> result;
    private RootProxy root;
    private JToggleButton button;
    private boolean running;
    private ArgumentPropertyAdaptor.ReadOnly runningAdaptor;
    
    
    StartableRootAction() {
        this(Utilities.actionsGlobalContext());
    }
    
    StartableRootAction(Lookup context) {
        super("", ImageUtilities.loadImageIcon(RESOURCE_DIR + "play24.png", true));
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
                to = ControlAddress.create(root.getAddress(), StartableInterface.START);
            } else {
                to = ControlAddress.create(root.getAddress(), StartableInterface.STOP);
            }
            PXRHelper.getDefault().send(to, CallArguments.EMPTY, null);
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
        PXRHelper.getDefault().unbind(runningAdaptor);
        runningAdaptor = null;
    }
    
    private void setup(RootProxy root) {
        this.root = root;
        if (!isStartable(root)) {
            return;
        }
        setEnabled(true);
        runningAdaptor = new ArgumentPropertyAdaptor.ReadOnly(this, 
                StartableInterface.IS_RUNNING, false, SyncRate.Low);
        runningAdaptor.addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                try {
                    PBoolean selected = PBoolean.coerce((Argument)pce.getNewValue());
                    putValue(SELECTED_KEY, selected.value());
                } catch (Exception ex) {
                    // no op?
                }             
            }
        });
        PXRHelper.getDefault().bind(
                ControlAddress.create(root.getAddress(), StartableInterface.IS_RUNNING),
                runningAdaptor);
    }
    

    
    private boolean isStartable(RootProxy root) {
        for (InterfaceDefinition i : root.getInfo().getInterfaces()) {
            if (StartableInterface.INSTANCE.equals(i)) {
                return true;
            }
        }
        return false;
    }
    
}
