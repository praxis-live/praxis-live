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
package org.praxislive.ide.pxr;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.praxislive.ide.core.api.Syncable;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.Node;
import org.praxislive.base.Binding;

/**
 *
 */
class PXRComponentEditor {

    private final static Logger LOG = Logger.getLogger(PXRComponentEditor.class.getName());

    private PXRComponentProxy component;
    private Dialog dialog;
    private JComponent editor;
    private Listener listener;
    private Map<String, PropertyPanel> propPanels;
    private List<Binding.Adaptor> adaptors;

    PXRComponentEditor(PXRComponentProxy component) {
        this.component = component;
    }

    void show() {
        if (dialog != null) {
            dialog.setVisible(true);
            dialog.toFront();
            return;
        }
//        if (editor == null) {
        initEditor();
//        }
        if (listener == null) {
            listener = new Listener();
        }

        DialogDescriptor descriptor = new DialogDescriptor(
                editor,
                component.getAddress().toString(),
                false,
                new Object[]{"Close"},
                null,
                DialogDescriptor.DEFAULT_ALIGN,
                null,
                null,
                false);

        dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.addWindowListener(listener);
//        component.addPropertyChangeListener(listener);
        Syncable sync = component.getLookup().lookup(Syncable.class);
        assert sync != null;
        if (sync != null) {
            sync.addKey(this);
        }
        dialog.setVisible(true);

    }

    void dispose() {
        dispose(true);
    }

    private void dispose(boolean closeDialog) {
        if (dialog != null) {
            Syncable sync = component.getLookup().lookup(Syncable.class);
            assert sync != null;
            if (sync != null) {
                sync.removeKey(this);
            }
            if (closeDialog) {
                dialog.setVisible(false);
            }
            dialog = null;
        }
    }

    private void initEditor() {
        // properties       
        PropertySheet propertyPanel = new PropertySheet();
        propertyPanel.setNodes(new Node[]{component.getNodeDelegate()});
        propertyPanel.setDescriptionAreaVisible(false);
        propertyPanel.setPreferredSize(new Dimension(250, calculateHeight()));

        editor = new JPanel(new BorderLayout());
        editor.add(new JScrollPane(propertyPanel), BorderLayout.CENTER);
    }

    private int calculateHeight() {
        int rowHeight = UIManager.getInt("netbeans.ps.rowheight");
        if (rowHeight < 16) {
            rowHeight = 16;
        }
        rowHeight += 1; // padding
        int count = 0;
        count += component.getPropertyIDs().length;
        count += component.getTriggerActions().size();
        count = Math.max(count, 4); // height of at least 4 rows?
        return Math.min(count * rowHeight, 500); // max 500px high?

    }

    private class Listener extends WindowAdapter {

        @Override
        public void windowClosed(WindowEvent we) {
            dialog.removeWindowListener(this);
            dispose(false);
        }

    }

}
