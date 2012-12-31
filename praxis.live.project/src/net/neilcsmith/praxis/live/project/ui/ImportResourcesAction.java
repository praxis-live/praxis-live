/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.project.ui;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

@ActionID(
    category = "Project",
id = "net.neilcsmith.praxis.live.project.ui.ImportResourcesAction")
@ActionRegistration(lazy = false,
displayName = "#CTL_ImportResourcesAction")
@ActionReference(path = "Loaders/folder/any/Actions", position = 250, separatorAfter = 275)
@Messages("CTL_ImportResourcesAction=Import...")
public final class ImportResourcesAction extends AbstractAction
        implements ContextAwareAction {

    private final static RequestProcessor RP = new RequestProcessor(ImportResourcesAction.class);

    public ImportResourcesAction() {
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        assert false;
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new ActionImpl(actionContext);
    }

    private static class ActionImpl extends AbstractAction {

        private PraxisProject project;
        private FileObject folder;

        private ActionImpl(Lookup context) {
            project = context.lookup(PraxisProject.class);
            if (project != null) {
                FileObject res = project.getProjectDirectory().getFileObject("resources");
                FileObject d = context.lookup(FileObject.class);
                if (res != null && d != null && d.isFolder()
                        && (res == d || FileUtil.isParentOf(res, d))) {
                    folder = d;
                }
            }
            if (folder == null) {
                setEnabled(false);
                putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
            }
            putValue(NAME, "Import...");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (folder == null) {
                return;
            }
            File[] files = new FileChooserBuilder(ImportResourcesAction.class)
                    .setTitle("Import File(s)")
                    .setFileHiding(true)
                    .setFilesOnly(true)
                    .setApproveText("OK")
                    .showMultiOpenDialog();

            if (files != null && files.length > 0) {
                RP.post(new FileCopyTask(files, folder));
            }

        }
    }

    private static class FileCopyTask implements Runnable, Cancellable {

        private final File[] files;
        private final FileObject folder;
        private volatile boolean cancelled;

        private FileCopyTask(File[] files, FileObject folder) {
            this.files = files;
            this.folder = folder;
        }

        @Override
        public void run() {
            ProgressHandle ph = ProgressHandleFactory.createHandle("Importing Files...", this);
            ph.start(files.length);
            int count = 0;
            for (File file : files) {
                try {
                    final FileObject src = FileUtil.toFileObject(file);
                    ph.progress("Copying " + src.getNameExt(), count++);
                    if (src.isFolder()) {
                        // shouldn't happen at the moment. How to do safely?
                        count++;
                        continue;
                    }
//                    final String name = src.isFolder() ? FileUtil.findFreeFolderName(folder, src.getNameExt())
//                            : FileUtil.findFreeFileName(folder, src.getName(), src.getExt());
                    final String name = FileUtil.findFreeFileName(folder, src.getName(), src.getExt());
                    FileUtil.runAtomicAction(new FileSystem.AtomicAction() {
                        @Override
                        public void run() throws IOException {
                            FileUtil.copyFile(src, folder, name);
                        }
                    });
                } catch (IOException iOException) {
                    Exceptions.printStackTrace(iOException);
                }
            }
            ph.finish();
        }

        @Override
        public boolean cancel() {
            return false;
        }
    }
}
