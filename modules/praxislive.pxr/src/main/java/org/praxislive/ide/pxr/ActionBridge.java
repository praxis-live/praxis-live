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

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import org.praxislive.core.ComponentAddress;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.core.api.AbstractTask;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.core.api.CallExecutionException;
import org.praxislive.ide.pxr.spi.ModelTransform;
import org.praxislive.ide.pxr.wizard.PXGExportWizard;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;
import org.praxislive.project.ParseException;

/**
 *
 */
public class ActionBridge {

    private final static ActionBridge INSTANCE = new ActionBridge();

    private final static RequestProcessor RP = new RequestProcessor(ActionBridge.class);

    private ActionBridge() {
        // non instantiable
    }

    @SuppressWarnings("deprecation")
    public Task createCopyTask(ContainerProxy container,
            Set<String> children,
            ModelTransform.Copy copyTransform) {
        return createCopyTask(container, children, null, copyTransform, null);
    }

    @Deprecated
    public Task createCopyTask(ContainerProxy container,
            Set<String> children,
            Runnable preWriteTask,
            ModelTransform.Copy copyTransform,
            Runnable postWriteTask) {
        SubGraphTransferable empty = new SubGraphTransferable("");
        getClipboard().setContents(empty, empty);
        return new CopyTask(Objects.requireNonNull(container),
                Objects.requireNonNull(children),
                preWriteTask,
                copyTransform,
                postWriteTask);
    }

    public Task createPasteTask(ContainerProxy container, ModelTransform.Paste pasteTransform) {
        return new PasteTask(Objects.requireNonNull(container), pasteTransform);
    }

    @SuppressWarnings("deprecation")
    public Task createExportTask(ContainerProxy container,
            Set<String> children,
            ModelTransform.Export exportTransform) {
        return createExportTask(container, children, null, exportTransform, null);
    }

    @Deprecated
    public Task createExportTask(ContainerProxy container,
            Set<String> children,
            Runnable preWriteTask,
            ModelTransform.Export exportTransform,
            Runnable postWriteTask) {
        return new ExportTask(Objects.requireNonNull(container),
                Objects.requireNonNull(children),
                preWriteTask,
                exportTransform,
                postWriteTask);
    }

    public Task createImportTask(ContainerProxy container,
            FileObject file,
            ModelTransform.Import importTransform) {
        return new ImportTask(Objects.requireNonNull(container),
                Objects.requireNonNull(file),
                importTransform);
    }

    private static class CopyTask extends AbstractTask {

        private final ContainerProxy container;
        private final Set<String> children;
        private final Runnable preWriteTask;
        private final ModelTransform.Copy copyTransform;
        private final Runnable postWriteTask;

        CopyTask(ContainerProxy container,
                Set<String> children,
                Runnable preWriteTask,
                ModelTransform.Copy copyTransform,
                Runnable postWriteTask) {
            this.container = container;
            this.children = children;
            this.preWriteTask = preWriteTask;
            this.copyTransform = copyTransform == null ? m -> m : copyTransform;
            this.postWriteTask = postWriteTask;
        }

        @Override
        protected void handleExecute() throws Exception {
            if (preWriteTask != null) {
                preWriteTask.run();
            }
            PXRHelper helper = findRootProxy(container).getHelper();
            CompletionStage<GraphModel> modelStage;
            if (children.size() == 1) {
                String childID = children.iterator().next();
                modelStage = helper.componentData(ComponentAddress.of(
                        container.getAddress(), childID))
                        .thenApply(data -> GraphModel.fromSerializedComponent(childID, data));
            } else {
                modelStage = helper.componentData(container.getAddress())
                        .thenApply(data -> GraphModel.fromSerializedSubgraph(data, children::contains));
            }
            modelStage.thenApply(copyTransform)
                    .thenAccept(model -> {
                        SubGraphTransferable trans = new SubGraphTransferable(model.writeToString());
                        getClipboard().setContents(trans, trans);
                    })
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            Exceptions.printStackTrace(ex);
                            updateState(State.ERROR);
                        } else {
                            updateState(State.COMPLETED);
                        }
                        if (postWriteTask != null) {
                            postWriteTask.run();
                        }
                    });
        }

    }

    private static class PasteTask extends AbstractTask {

        private final ContainerProxy container;
        private final ModelTransform.Paste pasteTransform;
        private List<String> log;

        PasteTask(ContainerProxy container, ModelTransform.Paste pasteTransform) {
            this.container = container;
            this.pasteTransform = pasteTransform == null ? m -> m : pasteTransform;
            this.log = List.of();
        }

        @Override
        protected void handleExecute() throws Exception {
            Clipboard clipboard = getClipboard();
            PXRRootProxy root = findRootProxy(container);
            PXRHelper helper = root.getHelper();
            URI projectDir = root.getProject().getProjectDirectory().toURI();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                String script = (String) clipboard.getData(DataFlavor.stringFlavor);
                if (script.strip().isEmpty()) {
                    updateState(State.COMPLETED);
                    return;
                }
                GraphModel model = GraphModel.parseSubgraph(projectDir, script);
                model = ImportRenameSupport.prepareForPaste(container, model);
                if (model == null) {
                    updateState(State.CANCELLED);
                    return;
                }
                model = pasteTransform.apply(model);
                helper.safeContextEval(projectDir, container.getAddress(),
                        model.writeToString())
                        .whenComplete((r, ex) -> {
                            if (ex != null) {
                                log = handleException(ex);
                                updateState(State.ERROR);
                            } else {
                                updateState(State.COMPLETED);
                            }
                        });
            } else {
                updateState(State.ERROR);
            }

        }

        @Override
        public List<String> log() {
            return log;
        }

    }

    private static class ExportTask extends AbstractTask {

        private final ContainerProxy container;
        private final Set<String> children;
        private final Runnable preWriteTask;
        private final ModelTransform.Export exportTransform;
        private final Runnable postWriteTask;

        ExportTask(ContainerProxy container,
                Set<String> children,
                Runnable preWriteTask,
                ModelTransform.Export exportTransform,
                Runnable postWriteTask) {
            this.container = container;
            this.children = children;
            this.preWriteTask = preWriteTask;
            this.exportTransform = exportTransform == null ? m -> m : exportTransform;
            this.postWriteTask = postWriteTask;
        }

        @Override
        protected void handleExecute() throws Exception {
            if (preWriteTask != null) {
                preWriteTask.run();
            }
            PXRHelper helper = findRootProxy(container).getHelper();
            CompletionStage<GraphModel> modelStage;
            if (children.size() == 1) {
                String childID = children.iterator().next();
                modelStage = helper.componentData(ComponentAddress.of(
                        container.getAddress(), childID))
                        .thenApply(data -> GraphModel.fromSerializedComponent(childID, data));
            } else {
                modelStage = helper.componentData(container.getAddress())
                        .thenApply(data -> GraphModel.fromSerializedSubgraph(data, children::contains));
            }
            modelStage.thenApply(exportTransform)
                    .thenAccept(model -> handleSave(model.writeToString()))
                    .exceptionally(ex -> {
                        updateState(State.ERROR);
                        return null;
                    });
        }

        private void handleSave(String export) {
            PXGExportWizard wizard = new PXGExportWizard();
            try {
                GraphElement.Root root = GraphModel.parseSubgraph(export).root();
                wizard.setSuggestedFileName(findSuggestedName(root));
                wizard.setSuggestedPaletteCategory(findPaletteCategory(root));
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }

            if (wizard.display() != WizardDescriptor.FINISH_OPTION) {
                updateState(State.CANCELLED);
                return;
            }

            File file = wizard.getExportFile();
            String paletteCategory = wizard.getPaletteCategory().replace(":", "_");

            RP.post(() -> {
                if (file.exists()) {
                    EventQueue.invokeLater(() -> {
                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message("File already exists.", NotifyDescriptor.ERROR_MESSAGE));
                        updateState(State.ERROR);
                        if (postWriteTask != null) {
                            postWriteTask.run();
                        }
                    });
                } else {
                    try {
                        Files.writeString(file.toPath(), export, StandardOpenOption.CREATE_NEW);
                        FileUtil.toFileObject(file.getParentFile()).refresh();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        EventQueue.invokeLater(() -> {
                            updateState(State.ERROR);
                            if (postWriteTask != null) {
                                postWriteTask.run();
                            }
                        });
                        return;
                    }
                    try {
                        if (!paletteCategory.isEmpty()) {
                            FileObject src = FileUtil.toFileObject(file);
                            FileObject dst = FileUtil.createFolder(
                                    FileUtil.getConfigRoot(), "PXR/Palette/" + paletteCategory);
                            FileUtil.copyFile(src, dst, src.getName());
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        EventQueue.invokeLater(() -> {
                            updateState(State.ERROR);
                            if (postWriteTask != null) {
                                postWriteTask.run();
                            }
                        });
                        return;
                    }

                    EventQueue.invokeLater(() -> {
                        updateState(State.COMPLETED);
                        if (postWriteTask != null) {
                            postWriteTask.run();
                        }
                    });

                }

            });
        }

        private String findSuggestedName(GraphElement.Root root) {
            if (root.children().size() == 1) {
                return root.children().firstEntry().getKey();
            } else {
                return "";
            }
        }

        private String findPaletteCategory(GraphElement.Root root) {
            String ret = "core:custom";
            for (GraphElement.Component cmp : root.children().sequencedValues()) {
                if (!cmp.children().isEmpty()) {
                    // container ??
                    return "";
                }
                String type = cmp.type().toString();
                if (type.startsWith("video:gl:")) {
                    // short circuit for GL
                    return "video:gl:custom";
                } else if (!type.startsWith("core")) {
                    String base = type.substring(0, type.indexOf(":"));
                    ret = base + ":custom";
                }
            }
            return ret;
        }

    }

    private static class ImportTask extends AbstractTask {

        private final ContainerProxy container;
        private final FileObject file;
        private final ModelTransform.Import importTransform;
        private List<String> log;

        ImportTask(ContainerProxy container, FileObject file, ModelTransform.Import importTransform) {
            this.container = container;
            this.file = file;
            this.importTransform = importTransform == null ? m -> m : importTransform;
            log = List.of();
        }

        @Override
        protected void handleExecute() throws Exception {
            PXRRootProxy root = findRootProxy(container);
            PXRHelper helper = root.getHelper();
            URI projectDir = root.getProject().getProjectDirectory().toURI();

            CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            return file.asText();
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    }, RP)
                    .thenComposeAsync(script -> {
                        try {
                            GraphModel model = GraphModel.parseSubgraph(projectDir, script);
                            model = ImportRenameSupport.prepareForImport(container, model);
                            if (model == null) {
                                updateState(State.CANCELLED);
                                return CompletableFuture.completedStage(null);
                            }
                            model = importTransform.apply(model);
                            return helper.safeContextEval(projectDir,
                                    container.getAddress(), model.writeToString());
                        } catch (ParseException pex) {
                            throw new RuntimeException(pex);
                        }
                    }, EventQueue::invokeLater)
                    .whenCompleteAsync((r, ex) -> {
                        if (ex != null) {
                            log = handleException(ex);
                            updateState(State.ERROR);
                        } else if (getState() == Task.State.RUNNING) {
                            updateState(State.COMPLETED);
                        }
                    }, EventQueue::invokeLater);
        }

        @Override
        public List<String> log() {
            return log;
        }
    }

    private static Clipboard getClipboard() {
        Clipboard c = Lookup.getDefault().lookup(Clipboard.class);
        if (c == null) {
            c = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return c;
    }

    private static PXRRootProxy findRootProxy(ContainerProxy container) {
        while (container != null) {
            if (container instanceof PXRRootProxy root) {
                return root;
            }
            container = container.getParent();
        }
        throw new IllegalStateException("No root proxy found");
    }

    private static List<String> handleException(Throwable ex) {
        if (ex instanceof CompletionException ce) {
            return handleException(ce.getCause());
        }
        if (ex instanceof CallExecutionException err) {
            return err.error().message().lines().toList();
        } else {
            return List.of(ex.toString());
        }
    }

    public static ActionBridge getDefault() {
        return INSTANCE;
    }

    private static class SubGraphTransferable implements Transferable, ClipboardOwner {

        private String data;

        private SubGraphTransferable(String data) {
            this.data = data;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{
                DataFlavor.stringFlavor
            };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            if (flavor.equals(DataFlavor.stringFlavor)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (isDataFlavorSupported(flavor)) {
                return data;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // no op
        }
    }

}
