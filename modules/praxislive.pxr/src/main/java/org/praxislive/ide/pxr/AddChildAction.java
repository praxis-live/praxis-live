/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.explorer.view.MenuView;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;
import org.openide.util.actions.Presenter;
import org.praxislive.core.ComponentType;

/**
 *
 */
@ActionID(category = "PXR", id = "org.praxislive.ide.pxr.AddChildAction")
@ActionRegistration(
        displayName = "#CTL_AddChildAction",
        lazy = false
)
@Messages("CTL_AddChildAction=Add...")
public class AddChildAction extends AbstractAction
        implements ContextAwareAction, Presenter.Popup, Presenter.Menu {

    private final Lookup.Result<ActionEditorContext> result;
    private final LookupListener listener;
    private final DynMenu menu;

    private ActionEditorContext editorContext;
    private MenuView.Menu paletteView;

    public AddChildAction() {
        this(Utilities.actionsGlobalContext());
    }

    private AddChildAction(Lookup context) {
        super(Bundle.CTL_AddChildAction());
        this.menu = new DynMenu();
        this.result = context.lookupResult(ActionEditorContext.class);
        listener = this::resultChanged;
        this.result.addLookupListener(
                WeakListeners.create(LookupListener.class, listener, result));
        setEnabled(false);
        resultChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // no op?
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        ActionEditorContext context = lkp.lookup(ActionEditorContext.class);
        if (context != null) {
            return new AddChildAction(lkp);
        } else {
            return this;
        }
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return menu;
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return menu;
    }

    private void resultChanged(LookupEvent ev) {
        editorContext = null;
        paletteView = null;
        Collection<? extends ActionEditorContext> contexts = result.allInstances();
        if (contexts.size() == 1) {
            editorContext = contexts.iterator().next();
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }

    private boolean acceptChoice(Node[] nodes) {
        if (nodes.length == 1) {
            ComponentType type = nodes[0].getLookup().lookup(ComponentType.class);
            if (type != null) {
                EventQueue.invokeLater(() -> editorContext.acceptComponentType(type));
                return true;
            }
            FileObject fo = nodes[0].getLookup().lookup(FileObject.class);
            if (fo != null) {
                EventQueue.invokeLater(() -> editorContext.acceptImport(fo));
                return true;
            }
        }
        return false;
    }

    private class DynMenu extends JMenuItem implements DynamicMenuContent {

        private DynMenu() {
            super(AddChildAction.this);
        }

        @Override
        public JComponent[] getMenuPresenters() {
            if (editorContext != null) {
                if (paletteView == null) {
                    paletteView = new MenuView.Menu(editorContext.palette(),
                            AddChildAction.this::acceptChoice);
                    paletteView.setIcon(null);
                    paletteView.setText(Bundle.CTL_AddChildAction());
                }
                return new JComponent[]{paletteView};
            } else {
                return new JComponent[]{this};
            }
        }

        @Override
        public JComponent[] synchMenuPresenters(JComponent[] jcs) {
            return getMenuPresenters();
        }

    }

}
