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
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.praxislive.core.ComponentAddress;
import org.praxislive.ide.core.api.Task;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.core.api.AbstractTask;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakListeners;
import org.praxislive.core.ComponentType;
import org.praxislive.core.Connection;
import org.praxislive.ide.core.api.CallExecutionException;
import org.praxislive.ide.core.api.Disposable;
import org.praxislive.ide.model.ComponentProxy;
import org.praxislive.ide.pxr.api.ActionSupport;
import org.praxislive.ide.pxr.api.EditorUtils;
import org.praxislive.ide.pxr.spi.ModelTransform;
import org.praxislive.ide.pxr.spi.RootEditor;
import org.praxislive.project.GraphElement;
import org.praxislive.project.GraphModel;
import org.praxislive.project.ParseException;

/**
 *
 */
@Messages({
    "# {0} - component type",
    "TTL_AddChild=Enter an ID for {0}",
    "LBL_AddChild=ID:",
    "LBL_CopyAction=Copy",
    "LBL_PasteAction=Paste",
    "LBL_DeleteAction=Delete",
    "LBL_DuplicateAction=Duplicate",
    "LBL_ExportAction=Export...",
    "TTL_DeleteTask=Confirm deletion",
    "# {0} - component ID",
    "LBL_DeleteTaskSingle=Delete {0}?",
    "# {0} - component count",
    "LBL_DeleteTaskMultiple=Delete {0} components?"
})
public class ActionBridge {

    public static final String CATEGORY = "PXR";

    private final static ActionBridge INSTANCE = new ActionBridge();

    private final static RequestProcessor RP = new RequestProcessor(ActionBridge.class);

    private ActionBridge() {
    }

    public Task.WithResult<String> createAddChildTask(ContainerProxy container,
            ComponentType type) {
        return new AddChildTask(Objects.requireNonNull(container), Objects.requireNonNull(type));
    }

    public Action createCopyAction(RootEditor editor, ExplorerManager explorer) {
        return new CopyActionPerformer(Objects.requireNonNull(editor), Objects.requireNonNull(explorer));
    }

    public Task createCopyTask(ContainerProxy container,
            List<String> children,
            ModelTransform.Copy copyTransform) {
        SubGraphTransferable empty = new SubGraphTransferable("");
        getClipboard().setContents(empty, empty);
        return new CopyTask(Objects.requireNonNull(container),
                List.copyOf(children),
                copyTransform);
    }

    public Action createDeleteAction(RootEditor editor, ExplorerManager explorer) {
        return new DeleteActionPerformer(Objects.requireNonNull(editor), Objects.requireNonNull(explorer));
    }

    public Task createDeleteTask(ContainerProxy container, List<String> children, List<Connection> connections) {
        return new DeleteTask(Objects.requireNonNull(container),
                List.copyOf(children), List.copyOf(connections));
    }

    public Action createDuplicateAction(RootEditor editor, ExplorerManager explorer) {
        return new DuplicateActionPerformer(Objects.requireNonNull(editor), Objects.requireNonNull(explorer));
    }

    public Action createExportAction(RootEditor editor, ExplorerManager explorer) {
        return new ExportActionPerformer(Objects.requireNonNull(editor), Objects.requireNonNull(explorer));
    }

    public Action createPasteAction(RootEditor editor, ExplorerManager explorer) {
        return new PasteActionPerformer(Objects.requireNonNull(editor), Objects.requireNonNull(explorer));
    }

    public Task.WithResult<List<String>> createPasteTask(ContainerProxy container, ModelTransform.Paste pasteTransform) {
        return new PasteTask(Objects.requireNonNull(container), pasteTransform);
    }

    public Task createExportTask(ContainerProxy container,
            List<String> children,
            ModelTransform.Export exportTransform) {
        return new ExportTask(Objects.requireNonNull(container),
                List.copyOf(children),
                exportTransform);
    }

    public Task createImportTask(ContainerProxy container,
            FileObject file,
            ModelTransform.Import importTransform) {
        return new ImportTask(Objects.requireNonNull(container),
                Objects.requireNonNull(file),
                importTransform);
    }

    private static class AddChildTask extends AbstractTask.WithResult<String> {

        private final ContainerProxy container;
        private final ComponentType type;

        private AddChildTask(ContainerProxy container, ComponentType type) {
            this.container = container;
            this.type = type;
        }

        @Override
        protected void handleExecute() throws Exception {
            NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                    Bundle.LBL_AddChild(),
                    Bundle.TTL_AddChild(type)
            );
            dlg.setInputText(
                    EditorUtils.findFreeID(
                            container.children().collect(Collectors.toSet()),
                            EditorUtils.extractBaseID(type), true));
            Object retval = DialogDisplayer.getDefault().notify(dlg);
            if (retval == NotifyDescriptor.OK_OPTION) {
                String id = dlg.getInputText().strip();
                container.addChild(id, type)
                        .whenComplete((r, ex) -> {
                            if (ex != null) {
                                Exceptions.printStackTrace(ex);
                                updateState(State.ERROR);
                            } else {
                                complete(id);
                            }
                        });

            } else {
                updateState(State.CANCELLED);
            }
        }

    }

    private static class CopyTask extends AbstractTask {

        private final ContainerProxy container;
        private final List<String> children;
        private final ModelTransform.Copy copyTransform;

        CopyTask(ContainerProxy container,
                List<String> children,
                ModelTransform.Copy copyTransform) {
            this.container = container;
            this.children = children;
            this.copyTransform = copyTransform == null ? m -> m : copyTransform;
        }

        @Override
        protected void handleExecute() throws Exception {
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
                    });
        }

    }

    private static class DeleteTask extends AbstractTask {

        private final ContainerProxy container;
        private final List<String> children;
        private final List<Connection> connections;

        DeleteTask(ContainerProxy container,
                List<String> children,
                List<Connection> connections) {
            this.container = container;
            this.children = children;
            this.connections = connections;
        }

        @Override
        protected void handleExecute() throws Exception {
            if (!checkDeletion()) {
                updateState(State.CANCELLED);
                return;
            }
            Stream.concat(connections.stream().map(container::disconnect),
                    children.stream().map(container::removeChild))
                    .reduce(CompletableFuture.completedStage(null),
                            (s1, s2) -> s1.thenCombine(s2, (r1, r2) -> null))
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            Exceptions.printStackTrace(ex);
                            updateState(State.ERROR);
                        } else {
                            updateState(State.COMPLETED);
                        }
                    });
        }

        private boolean checkDeletion() {
            if (children.isEmpty()) {
                return true;
            }
            int count = children.size();
            String msg = count > 1
                    ? Bundle.LBL_DeleteTaskMultiple(count)
                    : Bundle.LBL_DeleteTaskSingle(children.get(0));
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(
                    msg, Bundle.TTL_DeleteTask(), NotifyDescriptor.YES_NO_OPTION);
            return NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(nd));
        }

    }

    private static class PasteTask extends AbstractTask.WithResult<List<String>> {

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
                List<String> children = model.root().children().keySet().stream().toList();
                helper.safeContextEval(projectDir, container.getAddress(),
                        model.writeToString())
                        .whenComplete((r, ex) -> {
                            if (ex != null) {
                                log = handleException(ex);
                                updateState(State.ERROR);
                            } else {
                                complete(children);
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
        private final List<String> children;
        private final ModelTransform.Export exportTransform;

        ExportTask(ContainerProxy container,
                List<String> children,
                ModelTransform.Export exportTransform) {
            this.container = container;
            this.children = children;
            this.exportTransform = exportTransform == null ? m -> m : exportTransform;
        }

        @Override
        protected void handleExecute() throws Exception {
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
                    });
                } else {
                    try {
                        Files.writeString(file.toPath(), export, StandardOpenOption.CREATE_NEW);
                        FileUtil.toFileObject(file.getParentFile()).refresh();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        EventQueue.invokeLater(() -> {
                            updateState(State.ERROR);
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
                        });
                        return;
                    }

                    EventQueue.invokeLater(() -> {
                        updateState(State.COMPLETED);
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

        private final String data;

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

    private static abstract class EMSensitiveAction extends AbstractAction implements Disposable {

        private final RootEditor editor;
        private final ExplorerManager explorer;
        private final PropertyChangeListener baseListener;
        private final PropertyChangeListener emListener;

        private ContainerProxy container;
        private List<ComponentProxy> selection;

        EMSensitiveAction(String name, RootEditor editor, ExplorerManager explorer) {
            super(name);
            this.editor = Objects.requireNonNull(editor);
            this.explorer = Objects.requireNonNull(explorer);
            this.baseListener = this::propertyChange;
            this.emListener = WeakListeners.propertyChange(baseListener, explorer);
            this.explorer.addPropertyChangeListener(emListener);
            propertyChange(null);
        }

        final ContainerProxy context() {
            return container;
        }

        final List<ComponentProxy> selection() {
            return selection;
        }

        final ContainerProxy findSharedContainer() {
            if (selection.isEmpty()) {
                return null;
            }
            ContainerProxy container = selection.get(0).getParent();
            if (container == null) {
                return null;
            }
            for (int i = 1; i < selection.size(); i++) {
                ContainerProxy parent = selection.get(i).getParent();
                if (parent == null || parent != container) {
                    return null;
                }
            }
            return container;
        }

        final RootEditor editor() {
            return editor;
        }

        final ExplorerManager explorer() {
            return explorer;
        }

        final Void handleException(Throwable t) {
            if (t instanceof CancellationException
                    || (t instanceof CompletionException ex
                    && ex.getCause() instanceof CancellationException)) {
                // do nothing
            } else {
                Exceptions.printStackTrace(t);
            }
            return null;
        }

        void refresh() {

        }

        @Override
        public void dispose() {
            this.explorer.removePropertyChangeListener(emListener);
            container = null;
            selection = List.of();
        }

        private void propertyChange(PropertyChangeEvent ev) {
            Node contextNode = explorer.getExploredContext();
            Node[] selectedNodes = explorer.getSelectedNodes();
            container = contextNode.getLookup().lookup(ContainerProxy.class);
            selection = Stream.of(selectedNodes)
                    .map(n -> n.getLookup().lookup(ComponentProxy.class))
                    .filter(c -> c != null)
                    .toList();
            refresh();
        }

    }

    private static final class CopyActionPerformer extends EMSensitiveAction {

        CopyActionPerformer(RootEditor editor, ExplorerManager explorer) {
            super(Bundle.LBL_CopyAction(), editor, explorer);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ContainerProxy container = findSharedContainer();
            List<String> children = selection().stream()
                    .map(ComponentProxy::getID)
                    .toList();
            if (container == null || children.isEmpty()) {
                // @TODO should not happen - throw / log?
                return;
            }
            Task.run(ActionSupport.createCopyTask(editor(), container, children))
                    .exceptionally(this::handleException);
        }

        @Override
        void refresh() {
            setEnabled(!selection().isEmpty() && findSharedContainer() != null);
        }

    }

    private static final class DeleteActionPerformer extends EMSensitiveAction {

        DeleteActionPerformer(RootEditor editor, ExplorerManager explorer) {
            super(Bundle.LBL_DeleteAction(), editor, explorer);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Global delete action not on EDT!
            if (!EventQueue.isDispatchThread()) {
                EventQueue.invokeLater(() -> actionPerformed(e));
                return;
            }
            ContainerProxy container = findSharedContainer();
            List<String> children = selection().stream()
                    .map(ComponentProxy::getID)
                    .toList();
            if (container == null || children.isEmpty()) {
                // @TODO should not happen - throw / log?
                return;
            }
            Task.run(ActionSupport.createDeleteTask(editor(), container, children, List.of()))
                    .exceptionally(this::handleException);
        }

        @Override
        void refresh() {
            setEnabled(!selection().isEmpty() && findSharedContainer() != null);
        }

    }

    private static final class DuplicateActionPerformer extends EMSensitiveAction {

        DuplicateActionPerformer(RootEditor editor, ExplorerManager explorer) {
            super(Bundle.LBL_DuplicateAction(), editor, explorer);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            RootEditor ed = editor();
            ContainerProxy container = findSharedContainer();
            List<String> children = selection().stream()
                    .map(ComponentProxy::getID)
                    .toList();
            if (container == null || children.isEmpty()) {
                // @TODO should not happen - throw / log?
                return;
            }
            Task.run(ActionSupport.createCopyTask(ed, container, children))
                    .thenCompose(v -> Task.run(ActionSupport.createPasteTask(ed, container)))
                    .exceptionally(this::handleException);
        }

        @Override
        void refresh() {
            setEnabled(!selection().isEmpty() && findSharedContainer() != null);
        }

    }

    private static final class ExportActionPerformer extends EMSensitiveAction {

        ExportActionPerformer(RootEditor editor, ExplorerManager explorer) {
            super(Bundle.LBL_ExportAction(), editor, explorer);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ContainerProxy container = findSharedContainer();
            List<String> children = selection().stream()
                    .map(ComponentProxy::getID)
                    .toList();
            if (container == null || children.isEmpty()) {
                // @TODO should not happen - throw / log?
                return;
            }
            Task.run(ActionSupport.createExportTask(editor(), container, children))
                    .exceptionally(this::handleException);
        }

        @Override
        void refresh() {
            setEnabled(!selection().isEmpty() && findSharedContainer() != null);
        }

    }

    private static final class PasteActionPerformer extends EMSensitiveAction {

        PasteActionPerformer(RootEditor editor, ExplorerManager explorer) {
            super(Bundle.LBL_PasteAction(), editor, explorer);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ContainerProxy container = context();
            Task.WithResult.compute(ActionSupport.createPasteTask(editor(), container))
                    .thenAccept(ids -> selectChildren(container, ids))
                    .exceptionally(this::handleException);
        }

        @Override
        void refresh() {
            setEnabled(context() != null /* && selection().isEmpty() */);
        }

        private void selectChildren(ContainerProxy container, List<String> children) {
            // @TODO - need task to sync on adding proxies, or action to select on tree changes
//            Node context = container.getNodeDelegate();
//            Node[] nodes = children.stream()
//                    .map(container::getChild)
//                    .map(ComponentProxy::getNodeDelegate)
//                    .toArray(Node[]::new);
//            try {
//                explorer().setExploredContextAndSelection(context, nodes);
//            } catch (PropertyVetoException ex) {
//                Exceptions.printStackTrace(ex);
//            }
        }

    }

}
