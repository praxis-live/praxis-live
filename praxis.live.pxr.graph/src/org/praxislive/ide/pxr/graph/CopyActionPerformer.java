/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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
 * @author Neil C Smith
 */
class CopyActionPerformer extends AbstractAction {
    
    private final static Logger LOG = Logger.getLogger(CopyActionPerformer.class.getName());
    private GraphEditor editor;
    private ExplorerManager em;
    
    CopyActionPerformer(GraphEditor editor, ExplorerManager em) {
        this.editor = editor;
        this.em = em;
        em.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                refresh();
            }
        });
        refresh();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LOG.finest("Copy action invoked");
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
                .map(cmp -> cmp.getAddress().getID())
                .collect(Collectors.toSet());
        Task copyTask = editor.getActionSupport().createCopyTask(
                container,
                childIDs,
                () -> Utils.offsetComponents(cmps, offset, false),
                () -> Utils.offsetComponents(cmps, offset, true)
        );
        copyTask.execute();
    }
    
    private void refresh() {
        boolean enabled = findComponentsAndParent(null) != null;
        LOG.log(Level.FINEST, "Refreshing Copy action : enabled {0}", enabled);
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
