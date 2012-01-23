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
package net.neilcsmith.praxis.live.pxr;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import net.miginfocom.swing.MigLayout;
import net.neilcsmith.praxis.core.info.ComponentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.gui.ControlBinding;
import net.neilcsmith.praxis.live.pxr.api.PraxisProperty;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.explorer.propertysheet.PropertyPanel;
import org.openide.explorer.propertysheet.PropertySheet;
import org.openide.nodes.Node;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class PXRComponentEditor {
    
    private final static Logger LOG = Logger.getLogger(PXRComponentEditor.class.getName());
    
    private PXRComponentProxy component;
    private Dialog dialog;
    private JComponent editor;
    private Listener listener;
    private Map<String, PropertyPanel> propPanels;
    private List<ControlBinding.Adaptor> adaptors;
    

    PXRComponentEditor(PXRComponentProxy component) {
        this.component = component;
    }
    
    void show() {
        if (dialog != null) {
            dialog.setVisible(true);
            dialog.toFront();
            return;
        }
        if (editor == null) {
            initEditor();
        }
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
        component.addPropertyChangeListener(listener);
        dialog.setVisible(true);
        
    }
    
    private void initEditor() {
        // properties       
        PropertySheet propertyPanel = new PropertySheet();
        propertyPanel.setNodes(new Node[]{component.getNodeDelegate()});
        propertyPanel.setDescriptionAreaVisible(false);
        propertyPanel.setPreferredSize(new Dimension(250,(component.getPropertyIDs().length * 20) + 20));
        
        // triggers
        List<Action> triggers = component.getTriggerActions();
        JPanel actionsPanel = null;
        if (triggers.size() > 0) {
            actionsPanel = new JPanel(new MigLayout("", ""));
            for (Action trigger : triggers) {
                JButton bt = new JButton(trigger);
                actionsPanel.add(bt, "sizegroup trigger");
            }
        }
        
        editor = new JPanel(new BorderLayout(5, 5));
        if (propertyPanel == null && actionsPanel == null) {
            JLabel lab = new JLabel("<no editable controls>");
            lab.setEnabled(false);
            editor.add(lab, BorderLayout.CENTER);
        } else {
            if (propertyPanel != null) {
                editor.add(new JScrollPane(propertyPanel), BorderLayout.CENTER);
                if (actionsPanel != null) {
                    editor.add(actionsPanel, BorderLayout.NORTH);
                }
            } else {
                editor.add(actionsPanel, BorderLayout.CENTER);
            }
        }
        
    }
    
    
    private class Listener extends WindowAdapter implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINE, "Syncing {0} on {1}", new Object[]{pce.getPropertyName(), component.getAddress()});
            }
        }

        @Override
        public void windowClosed(WindowEvent we) {
            dialog.removeWindowListener(this);
            component.removePropertyChangeListener(this);
            dialog = null;
        }
        
        
        
    }
    
}
