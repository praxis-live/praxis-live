/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr.osc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.core.api.Callback;
import net.neilcsmith.praxis.live.pxr.api.ContainerProxy;
import net.neilcsmith.praxis.live.pxr.api.ProxyException;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.actions.DeleteAction;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.OutlineView;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith
 */
public class OSCEditor extends RootEditor {
    
    private final static String RESOURCE_DIR = "net/neilcsmith/praxis/live/pxr/osc/resources/";
    private RootProxy root;
    private JComponent editorComponent;
    private ExplorerManager em;
    private Lookup lookup;
    private Action[] actions;
    private OutlineView view;

    OSCEditor(RootProxy root) {
        this.root = root;

        editorComponent = new EditorPanel();
        em = new ExplorerManager();
        em.setRootContext(root.getNodeDelegate());
        editorComponent.add(initView());
        ActionMap am = editorComponent.getActionMap();
        initActions(am);
        lookup = ExplorerUtils.createLookup(em, am);
    }

    private OutlineView initView() {
        view = new OutlineView("Component");
        view.addPropertyColumn("address", "Praxis Control Address");
        view.addPropertyColumn("osc-address", "OSC Address");
        view.setPopupAllowed(false);
        return view;
    }

    private void initActions(ActionMap actionMap) {
        actions = new Action[2];
        actions[0] = new AddAction();
        DeleteAction del = DeleteAction.get(DeleteAction.class);
        actions[1] = del;
        actionMap.put(del.getActionMapKey(), ExplorerUtils.actionDelete(em, true));
    }

    @Override
    public JComponent getEditorComponent() {
        return editorComponent;
    }

    @Override
    public Action[] getActions() {
        return actions.clone();
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    private void addBinding() {
        ComponentType type = ComponentType.create("osc:input");
        ContainerProxy container = (ContainerProxy) root;
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                "ID:", "Enter an ID for " + type);
        dlg.setInputText(getFreeID(container, type));
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            final String id = dlg.getInputText();
            try {
                container.addChild(id, type, new Callback() {

                    @Override
                    public void onReturn(CallArguments args) {
                        // nothing wait for sync
                    }

                    @Override
                    public void onError(CallArguments args) {
                        DialogDisplayer.getDefault().notifyLater(new NotifyDescriptor.Message("Error creating component", NotifyDescriptor.ERROR_MESSAGE));
                    }
                });
            } catch (ProxyException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private String getFreeID(ContainerProxy container, ComponentType type) {
        String base = type.toString();
        base = base.substring(base.lastIndexOf(":") + 1);
        for (int i = 1; i < 100; i++) {
            if (container.getChild(base + i) == null) {
                return base + i;
            }
        }
        return "";
    }

    private class EditorPanel extends JPanel implements ExplorerManager.Provider {

        private EditorPanel() {
            setLayout(new BorderLayout());
        }

        @Override
        public ExplorerManager getExplorerManager() {
            return em;
        }

        @Override
        public void addNotify() {
            super.addNotify();
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    Component table = view.getViewport().getView();
                    KeyEvent ke = new KeyEvent(table,
                            KeyEvent.KEY_PRESSED,
                            System.currentTimeMillis(),
                            KeyEvent.CTRL_DOWN_MASK,
                            KeyEvent.VK_PLUS,
                            '+');
                    table.dispatchEvent(ke);
                    view.setCorner(JScrollPane.UPPER_RIGHT_CORNER, null);
                }
            });

        }
    }

    private class AddAction extends AbstractAction {

        private AddAction() {
            super("Add", ImageUtilities.loadImageIcon(RESOURCE_DIR + "add.png", true));
            putValue(SHORT_DESCRIPTION, "Add control binding");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            addBinding();
        }
    }
    
}
