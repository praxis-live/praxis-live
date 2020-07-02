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
package org.praxislive.ide.pxr.gui;

import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.miginfocom.swing.MigLayout;
import org.openide.util.Exceptions;
import org.praxislive.core.ComponentInfo;
import org.praxislive.core.Info;
import org.praxislive.core.Lookup;
import org.praxislive.core.protocols.ComponentProtocol;
import org.praxislive.core.protocols.ContainerProtocol;
import org.praxislive.core.protocols.StartableProtocol;
import org.praxislive.gui.GuiContext;
import org.praxislive.gui.Keys;
import org.praxislive.ide.core.api.AbstractIDERoot;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
public class DockableGuiRoot extends AbstractIDERoot {
    
    private final static Map<String, DockableGuiRoot> REGISTRY = 
            new HashMap<String, DockableGuiRoot>();
    private final static ComponentInfo INFO = Info.component(cmp -> cmp
            .merge(ComponentProtocol.API_INFO)
            .merge(ContainerProtocol.API_INFO)
            .merge(StartableProtocol.API_INFO)
    );
    
    private final PraxisProject project;
    
    private JFrame frame;
//    private JScrollPane scrollPane;
    private JPanel container;
    private MigLayout layout;
    private LayoutChangeListener layoutListener;
    private Context context;
    private Lookup lookup;
    private GuiEditor activeEditor;

    public DockableGuiRoot(PraxisProject project) {
        this.project = Objects.requireNonNull(project);
    }

    @Override
    public ComponentInfo getInfo() {
        return INFO;
    }

    @Override
    protected void setup() {
        frame = new JFrame();
        frame.setTitle("PraxisLIVE : " + getAddress());
        frame.setMinimumSize(new Dimension(150, 50));
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (activeEditor == null) {
                        setIdle();
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
        layout = new MigLayout("", "[fill]");
        container = new JPanel(layout);
        container.addContainerListener(new ChildrenListener());
        container.putClientProperty(Keys.Address, getAddress());
        layoutListener = new LayoutChangeListener();
        frame.getContentPane().add(new JScrollPane(container));
        
        REGISTRY.put(computeID(project, getAddress().rootID()), this);
        
    }

    @Override
    public Lookup getLookup() {
        if (lookup == null) {
            context = new Context();
            lookup = Lookup.of(super.getLookup(), context);
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
        if (getState() != State.ACTIVE_RUNNING) {
            Utils.disableAll(container);
        }
    }

    void requestDisconnect(GuiEditor editor) {
        if (activeEditor == editor) {
            editor.removeRootPanel(container);
            activeEditor = null;
            frame.getContentPane().add(new JScrollPane(container));
            if (getState() == State.ACTIVE_RUNNING) {           
                frame.pack();
                frame.setVisible(true);
                frame.requestFocus();
                frame.toFront();
            }
        }
    }
    
    static DockableGuiRoot find(PraxisProject project, String id) {
        return REGISTRY.get(computeID(project, id));
    }
    
    private static String computeID(PraxisProject project, String rootID) {
        return project.getProjectDirectory().getPath() + "!" + rootID;
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

        @Override
        public void componentAdded(ContainerEvent e) {
            if (e.getChild() instanceof JComponent) {
                JComponent child = (JComponent) e.getChild();
                child.addPropertyChangeListener(
                        Keys.LayoutConstraint, layoutListener);
                updateLayout(child);
            }
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            if (e.getChild() instanceof JComponent) {
                ((JComponent) e.getChild()).removePropertyChangeListener(
                        Keys.LayoutConstraint, layoutListener);
            }
            updateLayout(null);
        }
    }

    private class LayoutChangeListener implements PropertyChangeListener {

        @Override
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
