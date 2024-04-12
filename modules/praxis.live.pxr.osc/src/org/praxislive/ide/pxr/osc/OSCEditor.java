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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.osc;

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
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.praxislive.ide.model.RootProxy;
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
 */
public class OSCEditor extends RootEditor {
    
    private final static String RESOURCE_DIR = "org/praxislive/ide/pxr/osc/resources/";
    
    private final RootProxy root;
    private final JComponent editorComponent;
    private final ExplorerManager em;
    private final Lookup lookup;
    
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
        ComponentType type = ComponentType.of("osc:input");
        ContainerProxy container = (ContainerProxy) root;
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                "ID:", "Enter an ID for " + type);
        dlg.setInputText(getFreeID(container, type));
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            final String id = dlg.getInputText();
            try {
                container.addChild(id, type,
                        Callback.create(r -> {
                            if (r.isError()) {
                                DialogDisplayer.getDefault().notifyLater(
                                        new NotifyDescriptor.Message("Error creating component",
                                                NotifyDescriptor.ERROR_MESSAGE));
                            }
                        }));
            } catch (Exception ex) {
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
