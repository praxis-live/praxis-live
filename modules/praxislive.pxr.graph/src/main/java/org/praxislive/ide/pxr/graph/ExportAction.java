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
package org.praxislive.ide.pxr.graph;

import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;

/**
 *
 */
class ExportAction extends AbstractAction {
    
    private final static Logger LOG = Logger.getLogger(ExportAction.class.getName());
    private final GraphEditor editor;
    private final ExplorerManager em;
    
    ExportAction(GraphEditor editor, ExplorerManager em) {
        super("Export ...");
        this.editor = editor;
        this.em = em;
        em.addPropertyChangeListener(e -> refresh());
        refresh();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LOG.finest("Export action invoked");
        assert EventQueue.isDispatchThread();
        List<ComponentProxy> cmps = new ArrayList<>();
        ContainerProxy container = findComponentsAndParent(cmps);
        if (container == null) {
            LOG.finest("No container found");
            return;
        }
        Point offset = Utils.findOffset(cmps);
        LOG.log(Level.FINEST, "Found offset : {0}", offset);
        Set<String> childIDs = cmps.stream()
                .map(cmp -> cmp.getAddress().componentID())
                .collect(Collectors.toSet());
        Task exportTask = editor.getActionSupport().createExportTask(
                container,
                childIDs
        );
        exportTask.execute();
    }
    
    private void refresh() {
        boolean enabled = findComponentsAndParent(null) != null;
        LOG.log(Level.FINEST, "Refreshing Export action : enabled {0}", enabled);
        setEnabled(enabled);
    }

    // move into editor?
    private ContainerProxy findComponentsAndParent(List<ComponentProxy> components) {
        Node[] nodes = em.getSelectedNodes();
        ContainerProxy parent = null;
        for (Node node : nodes) {
            ComponentProxy cmp = node.getLookup().lookup(ComponentProxy.class);
            if (cmp != null) {
                ContainerProxy p = cmp.getParent();
                if (p == null || cmp == editor.getContainer()) {
                    parent = null;
                    break;
                }
                if (parent == null) {
                    parent = p;
                } else if (parent != p) {
                    parent = null;
                    break;
                }
                if (components != null) {
                    components.add(cmp);
                }
                
            }
        }
        return parent;
    }
    
}
