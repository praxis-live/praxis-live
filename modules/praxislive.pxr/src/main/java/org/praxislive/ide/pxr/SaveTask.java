/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2026 Neil C Smith.
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

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.core.api.AbstractTask;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;

/**
 *
 */
abstract class SaveTask extends AbstractTask {

    private final static RequestProcessor RP = new RequestProcessor();

    private final static Logger LOG = Logger.getLogger(SaveTask.class.getName());
    private final static Map<PXRDataObject, Single> activeTasks = new HashMap<>();

    static SaveTask createSaveTask(Set<PXRDataObject> dobs) {
        if (dobs == null || dobs.isEmpty()) {
            return null;
        }
        if (dobs.size() == 1) {
            PXRDataObject dob = dobs.iterator().next();
            Single active = activeTasks.get(dob);
            if (active != null) {
                return new Compound(Collections.singleton(dob));
            } else {
                return new Single(dob);
            }
        } else {
            return new Compound(new HashSet<>(dobs));
        }
    }

    private static class Single extends SaveTask {

        private final PXRDataObject dob;
        private PXRRootProxy root;
        private ProgressHandle ph;

        private Single(PXRDataObject dob) {
            this.dob = dob;
        }

        @Override
        public void handleExecute() {
            dob.preSave();
            root = PXRRootRegistry.findRootForFile(dob.getPrimaryFile());
            activeTasks.put(dob, this);

            ph = ProgressHandle.createHandle("Saving " + root.getAddress(), this);
            ph.setInitialDelay(0);
            ph.start();
            ph.progress("Syncing.");

            root.getHelper().componentData(root.getAddress())
                    .thenApply(this::toModel)
                    .thenCompose(model -> handleSharedSources(model))
                    .thenAcceptAsync(this::saveToFile, RP)
                    .whenCompleteAsync((res, ex) -> {
                        assert EventQueue.isDispatchThread();
                        if (ex != null) {
                            Exceptions.printStackTrace(ex);
                            updateState(State.ERROR);
                        } else {
                            updateState(State.COMPLETED);
                        }
                        activeTasks.remove(dob);
                        ph.finish();
                    }, EventQueue::invokeLater);

        }

        private GraphModel toModel(PMap data) {
            return GraphModel.fromSerializedRoot(root.getAddress().rootID(), data)
                    .withContext(dob.getPrimaryFile().getParent().toURI());
        }

        private CompletionStage<GraphModel> handleSharedSources(GraphModel model) {
            if (model.root().properties().containsKey("shared-code")) {
                return CompletableFuture.supplyAsync(() -> {
                    return root.getProject().getProjectDirectory()
                            .getFileObject("code/" + root.getID());
                }, RP).thenComposeAsync(sourceFolder -> {
                    if (sourceFolder != null && sourceFolder.isFolder()) {
                        return sharedSourcesToDisk(model);
                    } else {
                        return CompletableFuture.completedStage(model);
                    }
                }, EventQueue::invokeLater);
            } else {
                return CompletableFuture.completedStage(model);
            }
        }

        private CompletionStage<GraphModel> sharedSourcesToDisk(GraphModel model) {
            return root.getHelper().execScript("sources-write {%s} \"code/%s\""
                    .formatted(model.root().properties().get("shared-code").value(),
                            root.getID()))
                    .thenApply(v -> model.withTransform(r -> {
                        r.transformProperties(props -> props.map(p -> {
                            if ("shared-code".equals(p.getKey())) {
                                GraphElement.Command cmd = GraphElement.command(
                                        "sources \"code/%s\"".formatted(root.getID()));
                                return Map.entry(p.getKey(),
                                        GraphElement.property(cmd, p.getValue().value()));
                            } else {
                                return p;
                            }
                        }).toList());
                    }));
        }

        private void saveToFile(GraphModel model) {
            try {
                Files.writeString(FileUtil.toPath(dob.getPrimaryFile()), model.writeToString());
            } catch (IOException ioex) {
                throw new UncheckedIOException(ioex);
            }

        }
    }

    private static class Compound extends SaveTask implements PropertyChangeListener {

        private final Set<Single> childTasks;
        private final Set<PXRDataObject> dobs;

        private Compound(Set<PXRDataObject> dobs) {
            this.dobs = dobs;
            childTasks = new HashSet<>(dobs.size());
        }

        @Override
        public void handleExecute() {
            dobs.forEach(this::initChildTask);
        }

        private void initChildTask(PXRDataObject dob) {
            Single child = activeTasks.get(dob);
            if (child == null) {
                child = new Single(dob);
            }
            childTasks.add(child);
            child.addPropertyChangeListener(this);
            if (child.getState() != State.RUNNING) {
                child.execute();
            }
        }

        @Override
        public boolean handleCancel() {
            for (Single child : childTasks) {
                child.cancel();
            }
            return true;
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            Single task = (Single) pce.getSource();
            if (this.getState() != State.RUNNING) {
                task.removePropertyChangeListener(this);
                return;
            }
            switch (task.getState()) {
                case ERROR, CANCELLED -> {
                    childTasks.clear();
                    updateState(task.getState());
                }
                case COMPLETED -> {
                    childTasks.remove(task);
                    if (childTasks.isEmpty()) {
                        updateState(State.COMPLETED);
                    }
                }
                default -> {
                    // nothing?
                }
            }
        }
    }
}
