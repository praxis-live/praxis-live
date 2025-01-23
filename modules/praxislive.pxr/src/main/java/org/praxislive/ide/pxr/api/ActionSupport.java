/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2025 Neil C Smith.
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
package org.praxislive.ide.pxr.api;

import java.util.List;
import org.praxislive.ide.pxr.spi.RootEditor;
import javax.swing.Action;
import org.openide.awt.ActionID;
import org.openide.awt.Actions;
import org.openide.explorer.ExplorerManager;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.pxr.ActionBridge;
import org.openide.filesystems.FileObject;
import org.praxislive.core.ComponentType;
import org.praxislive.core.Connection;
import org.praxislive.ide.core.api.Disposable;
import org.praxislive.ide.pxr.AddChildAction;
import org.praxislive.ide.pxr.ExposeControlsAction;
import org.praxislive.ide.pxr.RootConfigAction;
import org.praxislive.ide.pxr.StartableRootAction;
import org.praxislive.ide.pxr.spi.ModelTransform;

/**
 *
 * Support class for Actions in PXR editors.
 *
 */
public final class ActionSupport {

    /**
     * Action category for all PXR action. For use with {@link ActionID},
     * {@link Actions#forID(java.lang.String, java.lang.String)}, etc.
     */
    public static final String CATEGORY = ActionBridge.CATEGORY;

    /**
     * Action ID for the Add Child action. For use with {@link ActionID},
     * {@link Actions#forID(java.lang.String, java.lang.String)}, etc.
     */
    public static final String ADD_CHILD = AddChildAction.ID;

    /**
     * Action ID for the Expose Controls action. For use with {@link ActionID},
     * {@link Actions#forID(java.lang.String, java.lang.String)}, etc.
     */
    public static final String EXPOSE_CONTROLS = ExposeControlsAction.ID;

    /**
     * Action ID for the Root configuration action. For use with {@link ActionID},
     * {@link Actions#forID(java.lang.String, java.lang.String)}, etc.
     */
    public static final String ROOT_CONFIG = RootConfigAction.ID;

    /**
     * Action ID for the Root start action. For use with {@link ActionID},
     * {@link Actions#forID(java.lang.String, java.lang.String)}, etc.
     */
    public static final String ROOT_START = StartableRootAction.ID;

    private ActionSupport() {
    }

    /**
     * Create a task to add a component to the container. The task will show a
     * dialog for the user to enter the component ID. The task result on success
     * is the ID of the added component.
     *
     * @param editor active root editor
     * @param container container to add child to
     * @param type component type of child
     * @return child creation task
     */
    public static Task.WithResult<String> createAddChildTask(RootEditor editor,
            ContainerProxy container,
            ComponentType type) {
        return ActionBridge.getDefault().createAddChildTask(container, type);
    }

    /**
     * Create a copy action for the provided editor and explorer. The action
     * calls through to
     * {@link #createCopyTask(org.praxislive.ide.pxr.spi.RootEditor, org.praxislive.ide.model.ContainerProxy, java.util.Set)}.
     * The action may be used in an ActionMap linked to the global copy action
     * key, or standalone in popups. The action implements {@link Disposable}
     * and it is recommended to pass to
     * {@link Disposable#dispose(java.lang.Object)} rather than relying on GC to
     * clear listeners.
     *
     * @param editor root editor
     * @param explorer explorer manager
     * @return copy action
     */
    public static Action createCopyAction(RootEditor editor, ExplorerManager explorer) {
        return ActionBridge.getDefault().createCopyAction(editor, explorer);
    }

    /**
     * Create a task to copy the given children of the container to the
     * clipboard. The task will call in to the hub to serialize the components,
     * so will complete asynchronously.
     *
     * @param editor active root editor
     * @param container container of children to copy
     * @param children set of child IDs to copy
     * @return copy task
     */
    public static Task createCopyTask(RootEditor editor, ContainerProxy container, List<String> children) {
        return ActionBridge.getDefault().createCopyTask(container,
                children,
                editor.getLookup().lookup(ModelTransform.Copy.class));
    }

    /**
     * Create a delete action for the provided editor and explorer. The action
     * calls through to
     * {@link #createDeleteTask(org.praxislive.ide.pxr.spi.RootEditor, org.praxislive.ide.model.ContainerProxy, java.util.List, java.util.List)}.
     * The action may be used in an ActionMap linked to the global delete action
     * key, or standalone in popups. The action implements {@link Disposable}
     * and it is recommended to pass to
     * {@link Disposable#dispose(java.lang.Object)} rather than relying on GC to
     * clear listeners.
     * <p>
     * The connections list passed to the task will always be empty as the
     * explorer manager has no notion of selected connections. A root editor
     * needing to handle connection deletions separately should use a custom
     * action that calls the deletion task.
     *
     * @param editor root editor
     * @param explorer explorer manager
     * @return delete action
     */
    public static Action createDeleteAction(RootEditor editor, ExplorerManager explorer) {
        return ActionBridge.getDefault().createDeleteAction(editor, explorer);
    }

    /**
     * Create a task to delete the given children and/or connections from the
     * given container. A confirmation dialog will be shown if the list of
     * children is not empty.
     *
     * @param editor active root editor
     * @param container container
     * @param children child IDs to delete
     * @param connections connections to delete
     * @return delete task
     */
    public static Task createDeleteTask(RootEditor editor, ContainerProxy container,
            List<String> children, List<Connection> connections) {
        return ActionBridge.getDefault().createDeleteTask(container, children, connections);
    }

    /**
     * Create a duplicate action for the provided editor and explorer. The
     * action may be used in an ActionMap linked to the global duplicate action
     * key, or standalone in popups. The action implements {@link Disposable}
     * and it is recommended to pass to
     * {@link Disposable#dispose(java.lang.Object)} rather than relying on GC to
     * clear listeners.
     *
     * @param editor root editor
     * @param explorer explorer manager
     * @return duplicate action
     */
    public static Action createDuplicateAction(RootEditor editor, ExplorerManager explorer) {
        return ActionBridge.getDefault().createDuplicateAction(editor, explorer);
    }

    /**
     * Create an export action for the provided editor and explorer. The action
     * calls through to
     * {@link #createExportTask(org.praxislive.ide.pxr.spi.RootEditor, org.praxislive.ide.model.ContainerProxy, java.util.List)}.
     * The action implements {@link Disposable} and it is recommended to pass to
     * {@link Disposable#dispose(java.lang.Object)} rather than relying on GC to
     * clear listeners.
     *
     * @param editor root editor
     * @param explorer explorer manager
     * @return export action
     */
    public static Action createExportAction(RootEditor editor, ExplorerManager explorer) {
        return ActionBridge.getDefault().createExportAction(editor, explorer);
    }

    /**
     * Create a task to export the given children of the container to a subgraph
     * file. The task will complete asynchronously, first calling in to the hub
     * to serialize the components, then writing to the file. A dialog will be
     * shown for the user to select the export file name and other properties.
     *
     * @param editor active root editor
     * @param container container of children to export
     * @param children child IDs to include in export
     * @return export task
     */
    public static Task createExportTask(RootEditor editor, ContainerProxy container, List<String> children) {
        return ActionBridge.getDefault().createExportTask(
                container,
                children,
                editor.getLookup().lookup(ModelTransform.Export.class));
    }

    /**
     * Create a task to import the provided subgraph file in to the provided
     * container. A dialog will be shown to allow the user to change the IDs of
     * components added to the container.
     *
     * @param editor active root editor
     * @param container container for import
     * @param file subgraph file
     * @return import task
     */
    public static Task createImportTask(RootEditor editor, ContainerProxy container, FileObject file) {
        return ActionBridge.getDefault().createImportTask(container,
                file,
                editor.getLookup().lookup(ModelTransform.Import.class));
    }

    /**
     * Create a paste action for the provided editor and explorer. The action
     * calls through to
     * {@link #createPasteTask(org.praxislive.ide.pxr.spi.RootEditor, org.praxislive.ide.model.ContainerProxy)}.
     * The action may be used in an ActionMap linked to the global paste action
     * key, or standalone in popups. The action implements {@link Disposable}
     * and it is recommended to pass to
     * {@link Disposable#dispose(java.lang.Object)} rather than relying on GC to
     * clear listeners.
     *
     * @param editor root editor
     * @param explorer explorer manager
     * @return paste action
     */
    public static Action createPasteAction(RootEditor editor, ExplorerManager explorer) {
        return ActionBridge.getDefault().createPasteAction(editor, explorer);
    }

    /**
     * Create a task to handle pasting the clipboard contents into the provided
     * container. The clipboard contents should match the format of subgraph
     * files, as produced by the copy task. A dialog will be shown to allow the
     * user to change the IDs of components added to the container. The result
     * of the task is a list of the IDs of direct children added to the
     * container.
     *
     * @param editor active root editor
     * @param container container for paste
     * @return paste task
     */
    public static Task.WithResult<List<String>> createPasteTask(RootEditor editor, ContainerProxy container) {
        return ActionBridge.getDefault().createPasteTask(container,
                editor.getLookup().lookup(ModelTransform.Paste.class));
    }

}
