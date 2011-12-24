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
package net.neilcsmith.praxis.live.pxr.gui;

import java.awt.Dimension;
import net.neilcsmith.praxis.core.Lookup;
import java.awt.LayoutManager;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.IllegalRootStateException;
import net.neilcsmith.praxis.gui.Keys;
import net.neilcsmith.praxis.gui.ControlBinding.Adaptor;
import net.neilcsmith.praxis.gui.BindingContext;
import net.neilcsmith.praxis.gui.GuiContext;
import net.neilcsmith.praxis.impl.AbstractSwingRoot;
import net.neilcsmith.praxis.impl.InstanceLookup;
import net.neilcsmith.praxis.impl.RootState;

/**
 *
 * @author Neil C Smith
 */
public class DockableGuiRoot extends AbstractSwingRoot {
    
    private final static Map<String, DockableGuiRoot> REGISTRY = 
            new HashMap<String, DockableGuiRoot>();
    
    private JFrame frame;
//    private JScrollPane scrollPane;
    private JPanel container;
    private MigLayout layout;
    private LayoutChangeListener layoutListener;
    private Bindings bindings;
    private Context context;
    private Lookup lookup;
    private GuiEditor activeEditor;

    public DockableGuiRoot() {
//        bindingCache = new HashMap<ControlAddress, DefaultBindingControl>();
    }

    @Override
    protected void setup() {
        frame = new JFrame();
        frame.setTitle("PraxisLIVE: " + getAddress());
//        frame.setSize(150, 50);
        frame.setMinimumSize(new Dimension(150, 50));
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (activeEditor == null) {
                        setIdle();
                    }
                } catch (IllegalRootStateException ex) {
                    Logger.getLogger(DockableGuiRoot.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
//        frame.getContentPane().setLayout(new MigLayout("fill", "[fill, grow]"));
        layout = new MigLayout("", "[fill]");
        container = new JPanel(layout);
        container.addContainerListener(new ChildrenListener());
        container.putClientProperty(Keys.Address, getAddress());
        layoutListener = new LayoutChangeListener();
//        frame.getContentPane().add(new JScrollPane(container), "grow, push");
        frame.getContentPane().add(new JScrollPane(container));
        
        REGISTRY.put(getAddress().getRootID(), this);
        
    }

    @Override
    public Lookup getLookup() {
        if (lookup == null) {
            bindings = new Bindings();
            context = new Context();
            lookup = InstanceLookup.create(super.getLookup(), bindings, context);
        }
        return lookup;
    }

    @Override
    protected void starting() {
        super.starting();
        Utils.enableAll(container);
        if (activeEditor == null) {
            frame.pack();
            frame.setVisible(true);
        }
    }

    @Override
    protected void stopping() {
        super.stopping();
        Utils.disableAll(container);
        if (activeEditor == null) {
            frame.setVisible(false);
        }      
    }

    @Override
    protected void dispose() {
        super.dispose();
        if (activeEditor != null) {
            activeEditor.removeRootPanel(container);
            activeEditor = null;
        }
        frame.setVisible(false);
        frame.dispose();
        
        REGISTRY.values().remove(this);
    }

    void requestConnect(GuiEditor editor) {
        if (editor == activeEditor) {
            return;
        }
        if (activeEditor != null) {
            activeEditor.removeRootPanel(container);
            activeEditor = null;
        } else {
            frame.setVisible(false);
            container.getParent().remove(container);
            frame.getContentPane().removeAll();
        }
        activeEditor = editor;
        editor.addRootPanel(container);
    }

    void requestDisconnect(GuiEditor editor) {
        if (activeEditor == editor) {
            editor.removeRootPanel(container);
            activeEditor = null;
            frame.getContentPane().add(new JScrollPane(container));
            if (getState() == RootState.ACTIVE_RUNNING) {           
                frame.pack();
                frame.setVisible(true);
                frame.requestFocus();
                frame.toFront();
            }
        }
    }
    
    static DockableGuiRoot find(String id) {
        return REGISTRY.get(id);
    }

    private class Bindings extends BindingContext {

        @Override
        public void bind(ControlAddress address, Adaptor adaptor) {
            GuiHelper.getDefault().bind(address, adaptor);
        }

        @Override
        public void unbind(Adaptor adaptor) {
            GuiHelper.getDefault().unbind(adaptor);
        }
    }

    private class Context extends GuiContext {

        @Override
        public JComponent getContainer() {
            return container;
        }
    }

    private void updateLayout(JComponent child) {
        if (child != null) {
            layout.setComponentConstraints(child, child.getClientProperty(Keys.LayoutConstraint));
        }   
        container.revalidate();
        container.repaint();
    }

    private class ChildrenListener implements ContainerListener {

        public void componentAdded(ContainerEvent e) {
            if (e.getChild() instanceof JComponent) {
                JComponent child = (JComponent) e.getChild();
                child.addPropertyChangeListener(
                        Keys.LayoutConstraint, layoutListener);
                updateLayout(child);
            }
        }

        public void componentRemoved(ContainerEvent e) {
            if (e.getChild() instanceof JComponent) {
                ((JComponent) e.getChild()).removePropertyChangeListener(
                        Keys.LayoutConstraint, layoutListener);
            }
            updateLayout(null);
        }
    }

    private class LayoutChangeListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getSource() instanceof JComponent) {
                JComponent comp = (JComponent) evt.getSource();
                LayoutManager lm = container.getLayout();
                if (lm instanceof MigLayout) {
                    ((MigLayout) lm).setComponentConstraints(comp, evt.getNewValue());
                    container.revalidate();
                }
            }
        }
    }
}
