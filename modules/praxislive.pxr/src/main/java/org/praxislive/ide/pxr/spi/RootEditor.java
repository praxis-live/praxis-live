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
package org.praxislive.ide.pxr.spi;

import java.util.List;
import java.util.Optional;
import javax.swing.Action;
import javax.swing.JComponent;
import org.praxislive.ide.model.RootProxy;
import org.openide.awt.UndoRedo;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;
import org.praxislive.ide.project.api.PraxisProject;

/**
 * An interface for services providing the ability to edit a root. The provided
 * editor will be installed within the root editor top component.
 */
public interface RootEditor {

    /**
     * Acquire the main editor component. This component will be installed in
     * the central pane of the root editor top component. This method should
     * always return the same instance.
     *
     * @return main editor component
     */
    public abstract JComponent getEditorComponent();

    /**
     * Hook called when the containing top component is shown. See
     * {@link TopComponent#componentShowing()}.
     * <p>
     * The default implementation does nothing.
     */
    public default void componentShowing() {
        // no op hook
    }

    /**
     * Hook called when the containing top component is activated. See
     * {@link TopComponent#componentActivated()}.
     * <p>
     * The default implementation does nothing.
     */
    public default void componentActivated() {
        // no op hook
    }

    /**
     * Hook called when the containing top component is deactivated. See
     * {@link TopComponent#componentDeactivated()}.
     * <p>
     * The default implementation does nothing.
     */
    public default void componentDeactivated() {
        // no op hook
    }

    /**
     * Hook called when the containing top component is hidden. See
     * {@link TopComponent#componentHidden()}.
     * <p>
     * The default implementation does nothing.
     */
    public default void componentHidden() {
        // no op hook
    }

    /**
     * Hook called to ensure any data in the editor is synced to the underlying
     * model. Will be called prior to saving, and any other time the model needs
     * to be up-to-date.
     * <p>
     * The default implementation does nothing.
     */
    public default void sync() {
        //no op hook
    }

    /**
     * Hook called when the editor component is being uninstalled and disposed
     * of.
     * <p>
     * The default implementation does nothing.
     */
    public default void dispose() {
        // no op hook
    }

    /**
     * Get a list of action to be added to the top component toolbar.
     * <p>
     * The default implementation returns an empty list.
     *
     * @return list of toolbar actions
     */
    public default List<Action> getActions() {
        return List.of();
    }

    /**
     * Get the editor lookup. This lookup will be merged into the top component
     * lookup.
     * <p>
     * This lookup should normally reflect the selected component node(s).
     * <p>
     * The default implementation returns an empty lookup.
     *
     * @return editor lookup
     */
    public default Lookup getLookup() {
        return Lookup.EMPTY;
    }

    /**
     * Get the undo-redo support.
     * <p>
     * The default implementation returns {@link UndoRedo#NONE}.
     *
     * @return undo redo
     */
    public default UndoRedo getUndoRedo() {
        return UndoRedo.NONE;
    }

    /**
     * An interface for providers of root editors. Providers should be
     * registered using {@link ServiceProvider}. The first provider to return an
     * editor will be selected. Use service provider position to order if
     * necessary.
     */
    public static interface Provider {

        /**
         * Create an editor for the provided root, if the root type, context,
         * etc. are supported by this provider.
         *
         * @param root root proxy
         * @param context root editor context
         * @return root editor or empty optional
         */
        public Optional<RootEditor> createEditor(RootProxy root, Context context);

    }

    /**
     * A context for the root editor, providing additional data and a connection
     * back to the containing editor top component.
     */
    public static interface Context {

        /**
         * Access the editor top component that the editor is installed in.
         *
         * @return top component container
         */
        public TopComponent container();

        /**
         * Access the file that backs the root, if available.
         *
         * @return file or empty optional
         */
        public Optional<FileObject> file();

        /**
         * Access the project that the root is part of, if available.
         *
         * @return project or empty optional
         */
        public Optional<PraxisProject> project();

    }

}
