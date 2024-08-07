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

import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.pxr.api.ActionSupport;
import org.praxislive.ide.pxr.spi.RootEditor;

/**
 * Context object providing access to current editor, root and explored context
 * for internal actions.
 */
public class ActionEditorContext {

    static final String CONTEXT = "context";
    static final String SELECTION = "selection";

    private final PropertyChangeSupport pcs;
    private final RootEditor editor;
    private final PXRRootProxy root;
    private final Node palette;
    private final BiConsumer<PXRContainerProxy, List<PXRComponentProxy>> selector;

    private PXRContainerProxy context;
    private List<PXRComponentProxy> selection;

    ActionEditorContext(RootEditor editor, PXRRootProxy root, Node palette,
            BiConsumer<PXRContainerProxy, List<PXRComponentProxy>> selector) {
        this.editor = Objects.requireNonNull(editor);
        this.root = Objects.requireNonNull(root);
        this.palette = Objects.requireNonNull(palette);
        this.selector = Objects.requireNonNull(selector);
        this.pcs = new PropertyChangeSupport(this);
        this.context = root;
        this.selection = List.of();
    }

    RootEditor editor() {
        return editor;
    }

    PXRRootProxy root() {
        return root;
    }

    PXRContainerProxy context() {
        return context;
    }

    List<PXRComponentProxy> selection() {
        return selection;
    }

    Node palette() {
        return palette;
    }

    void select(PXRContainerProxy container, List<PXRComponentProxy> selection) {
        selector.accept(container, selection);
    }

    void updateSelection(PXRContainerProxy context, List<PXRComponentProxy> selection) {
        boolean contextChanged;
        if (context != this.context) {
            if (context.getRoot() != root) {
                throw new IllegalArgumentException();
            }
            contextChanged = true;
        } else {
            contextChanged = false;
        }
        List<PXRComponentProxy> oldSelection = selection;
        this.selection = List.copyOf(selection);
        if (contextChanged) {
            PXRContainerProxy oldContext = this.context;
            this.context = context;
            pcs.firePropertyChange(CONTEXT, oldContext, context);
        }
        pcs.firePropertyChange(SELECTION, oldSelection, selection);
    }

    void acceptComponentType(ComponentType type) {
        PXRContainerProxy container = context();
        Task.WithResult<String> task = ActionSupport.createAddChildTask(
                editor(), container, type);
        Task.WithResult.compute(task)
                .thenAccept(childID -> select(container, List.of(container.getChild(childID))));
    }

    void acceptImport(FileObject fo) {
        ActionSupport.createImportTask(editor(), context(), fo).execute();
    }

}
