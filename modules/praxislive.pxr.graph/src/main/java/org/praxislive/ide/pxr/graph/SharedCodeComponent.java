/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2021 Neil C Smith.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.io.OutputStreamWriter;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import org.netbeans.api.actions.Openable;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 */
@NbBundle.Messages({
    "ACT_NewSharedType=New Type...",
    "ACT_Open=Open",
    "ACT_Delete=Delete",
    "# {0} - Type name",
    "DLG_DeleteType=Delete shared type {0}?",
    "TTL_NewType=New Type",
    "DLG_EnterTypeName=Enter type name"
})
class SharedCodeComponent extends JPanel implements ExplorerManager.Provider {

    private final ExplorerManager em;
    private final FileView fileView;

    SharedCodeComponent(GraphEditor editor, FileObject parent) {
        setOpaque(false);
        em = new ExplorerManager();
        FolderNode rootNode = new FolderNode(
                DataFolder.findFolder(parent).getNodeDelegate(), true);
        em.setRootContext(rootNode);
        fileView = new FileView();
        fileView.setPreferredSize(new Dimension(200, 400));
        setLayout(new BorderLayout());
        add(fileView);
        Paint p = editor.getScene().getBackground();
        if (p instanceof Color) {
            setBackground((Color) p);
            fileView.updateBG((Color) p);
        }
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return em;
    }

    private static class FileView extends BeanTreeView {

        private void updateBG(Color bg) {
            super.setBackground(bg);
            tree.setBackground(bg);
        }

    }

    private static class FolderNode extends FilterNode {

        private final boolean root;
        private final NewFileAction newFileAction;

        public FolderNode(Node original, boolean root) {
            super(original, new FolderChildren(original));
            this.root = root;
            this.newFileAction = new NewFileAction(this);
        }

        @Override
        public Action getPreferredAction() {
            return null;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{newFileAction};
        }

        @Override
        public boolean canRename() {
            return !root;
        }

        
        
    }

    private static class FolderChildren extends FilterNode.Children {

        public FolderChildren(Node parent) {
            super(parent);
        }

        @Override
        protected Node[] createNodes(Node original) {
            if (original.isLeaf()) {
                FileObject file = original.getLookup().lookup(FileObject.class);
                if (file != null && file.hasExt("java")) {
                    return new Node[]{new FileNode(original)};
                } else {
                    return new Node[0];
                }
            } else {
                return new Node[]{new FolderNode(original, false)};
            }
        }

    }

    private static class FileNode extends FilterNode {

        private final OpenAction openAction;
        private final DeleteAction deleteAction;

        public FileNode(Node original) {
            super(original);
            openAction = new OpenAction(this);
            deleteAction = new DeleteAction(this);
        }

        @Override
        public Action getPreferredAction() {
            return openAction;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{openAction, deleteAction};
        }

        @Override
        public String getDisplayName() {
            String ret = super.getDisplayName();
            if (ret.endsWith(".java")) {
                return ret.substring(0, ret.lastIndexOf('.'));
            } else {
                return ret;
            }
        }

    }

    private static class OpenAction extends AbstractAction {

        private final Node node;

        private OpenAction(Node node) {
            super(Bundle.ACT_Open());
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Openable openable = node.getLookup().lookup(Openable.class);
            if (openable != null) {
                openable.open();
            }
        }

    }

    private static class DeleteAction extends AbstractAction {

        private final Node node;

        private DeleteAction(Node node) {
            super(Bundle.ACT_Delete());
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String name = node.getDisplayName();
            if (DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Confirmation(Bundle.DLG_DeleteType(name),
                            NotifyDescriptor.OK_CANCEL_OPTION))
                    == NotifyDescriptor.OK_OPTION) {
                try {
                    FileObject fob = node.getLookup().lookup(FileObject.class);
                    if (fob != null) {
                        fob.delete();
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

    }

    private static class NewFileAction extends AbstractAction {

        private final Node node;

        private NewFileAction(Node node) {
            super(Bundle.ACT_NewSharedType());
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine(
                        Bundle.DLG_EnterTypeName(), Bundle.TTL_NewType());
                if (DialogDisplayer.getDefault().notify(input) == NotifyDescriptor.OK_OPTION) {
                    String typeName = input.getInputText().trim();
                    if (typeName.isBlank() || typeName.contains("/")) {
                        return;
                    }
                    String source = "package SHARED;\n\n"
                            + "public class " + typeName + " {\n\n}";
                    String fileName = typeName + ".java";
                    FileObject folder = node.getLookup().lookup(FileObject.class);
                    FileObject file = FileUtil.createData(folder, fileName);
                    try (OutputStreamWriter writer = new OutputStreamWriter(file.getOutputStream())) {
                        writer.append(source);
                    }
                    DataObject dob = DataObject.find(file);
                    EditorCookie editor = dob.getLookup().lookup(EditorCookie.class);
                    editor.open();
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }

        }

    }

}
