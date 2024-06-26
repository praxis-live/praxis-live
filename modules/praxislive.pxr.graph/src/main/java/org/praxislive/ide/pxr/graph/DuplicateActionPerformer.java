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
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.praxislive.ide.core.api.Callback;
import javax.swing.AbstractAction;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.model.ContainerProxy;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.praxislive.core.Value;
import org.praxislive.ide.core.api.SerialTasks;

/**
 *
 */
class DuplicateActionPerformer extends AbstractAction implements Callback {
    
    private final static Logger LOG = Logger.getLogger(DuplicateActionPerformer.class.getName());
    private final GraphEditor editor;
    private final ExplorerManager em;
    
    DuplicateActionPerformer(GraphEditor editor, ExplorerManager em) {
        super("Duplicate");
        this.editor = editor;
        this.em = em;
        em.addPropertyChangeListener(e -> refresh());
        refresh();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        LOG.finest("Duplicate action invoked");
        assert EventQueue.isDispatchThread();
        List<ComponentProxy> cmps = new ArrayList<>();
        ContainerProxy container = findComponentsAndParent(cmps);
        if (container == null) {
            LOG.finest("No container found");
            return;
        }
        Set<String> childIDs = cmps.stream()
                .map(cmp -> cmp.getAddress().componentID())
                .collect(Collectors.toSet());
        Task task = new SerialTasks(
                editor.getActionSupport().createCopyTask(container, childIDs),
                editor.getActionSupport().createPasteTask(container)
        );
        task.execute();
    }
    
    private void refresh() {
        boolean enabled = findComponentsAndParent(null) != null;
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
    
    @Override
    public void onReturn(List<Value> args) {
        editor.syncGraph(true, true);
    }

    @Override
    public void onError(List<Value> args) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message("Error duplicating.",
                        NotifyDescriptor.ERROR_MESSAGE));
        editor.syncGraph(true);
    }
    
}
