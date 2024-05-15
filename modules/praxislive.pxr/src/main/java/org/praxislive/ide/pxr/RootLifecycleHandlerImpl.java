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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.netbeans.spi.project.ProjectServiceProvider;
import org.praxislive.ide.project.spi.RootLifecycleHandler;
import org.praxislive.ide.core.api.Task;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.PraxisProject;

/**
 *
 */
@ProjectServiceProvider(projectType = PraxisProject.TYPE,
        service = RootLifecycleHandler.class)
public class RootLifecycleHandlerImpl implements RootLifecycleHandler {

    private final PraxisProject project;

    public RootLifecycleHandlerImpl(Lookup lookup) {
        this.project = Objects.requireNonNull(lookup.lookup(PraxisProject.class));
    }

    @Override
    public Optional<Task> getDeletionTask(String description, Set<String> rootIDs) {
        var reg = project.getLookup().lookup(PXRRootRegistry.class);
        if (reg == null) {
            return Optional.empty();
        }
        Set<PXRRootProxy> roots = rootIDs.stream()
                .map(rootID -> reg.getRootByID(rootID))
                .filter(root -> (root != null))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!roots.isEmpty()) {
            return Optional.of(new DeletionSaveTask(description, roots));
        } else {
            return Optional.empty();
        }
    }

    private static class DeletionSaveTask implements Task, PropertyChangeListener {

        private Set<PXRRootProxy> roots;
        private State state;
        private PropertyChangeSupport pcs;
        private SaveTask delegate;
        private String description;

        private DeletionSaveTask(String description, Set<PXRRootProxy> roots) {
            this.description = description;
            this.roots = roots;
            this.state = State.NEW;
            pcs = new PropertyChangeSupport(this);
        }

        @Override
        public State execute() {
            if (state != State.NEW) {
                throw new IllegalStateException();
            }
            updateState(State.RUNNING);
            try {
                roots.forEach(r -> r.send("stop", List.of(),
                        Callback.create(res -> {
                        })));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(buildDialogMessage(), description);
            Object ret = DialogDisplayer.getDefault().notify(nd);
            if (ret == NotifyDescriptor.YES_OPTION) {
                // save
                var dobs = roots.stream()
                        .map(PXRRootProxy::getSource)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                delegate = SaveTask.createSaveTask(dobs);
                delegate.addPropertyChangeListener(this);
                return delegate.execute();
            } else if (ret == NotifyDescriptor.NO_OPTION) {
                // don't save - pass completed so operation can continue
                updateState(State.COMPLETED);
                return state;
            } else {
                // pass cancelled back
                updateState(State.CANCELLED);
                return state;
            }

        }

        private String buildDialogMessage() {
            return roots.stream()
                    .map(r -> r.getSource().getName())
                    .collect(Collectors.joining(" /", "Save changes to " + "/", "?"));
        }

        private void updateState(State state) {
            State old = this.state;
            this.state = state;
            pcs.firePropertyChange(PROP_STATE, old, state);
        }

        @Override
        public State getState() {
            return state;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            pcs.addPropertyChangeListener(listener);
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            pcs.removePropertyChangeListener(listener);
        }

        @Override
        public boolean cancel() {
            if (delegate != null) {
                return delegate.cancel();
            } else {
                return false;
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (delegate.getState() != this.state) {
                updateState(delegate.getState());
            }
        }

    }

}
