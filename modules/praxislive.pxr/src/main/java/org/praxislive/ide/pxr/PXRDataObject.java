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
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import org.praxislive.ide.core.api.Task.State;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.OpenSupport;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.CloneableTopComponent;
import org.praxislive.core.ComponentAddress;
import org.praxislive.ide.components.api.Icons;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.ProjectProperties;
import org.praxislive.project.GraphModel;

@NbBundle.Messages({
    "LBL_Root_Loader=Root Files"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Root_Loader",
        mimeType = "text/x-praxis-root",
        extension = {"pxr"})
@DataObject.Registration(
        mimeType = "text/x-praxis-root",
        iconBase = "org/praxislive/ide/pxr/resources/pxr16.png",
        displayName = "#LBL_Root_Loader",
        position = 300)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600),
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200),
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id
            = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300),
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id = @ActionID(category = "PXR", id = "org.praxislive.ide.pxr.SaveAsTemplateAction"),
            position = 1350),
    @ActionReference(
            path = "Loaders/text/x-praxis-root/Actions",
            id
            = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400)
})
public class PXRDataObject extends MultiDataObject {

    public final static String KEY_ATTR_ROOT_TYPE = "rootType";
    private final static RequestProcessor RP = new RequestProcessor();

    private final EditorSupport editorSupport;
    private final SaveSupport saveSupport;

    private String rootID;
    private Image icon;
    private DataNodeImpl node;

    public PXRDataObject(FileObject file, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(file, loader);
        editorSupport = new EditorSupport();
        saveSupport = new SaveSupport();
        CookieSet cookies = getCookieSet();
        cookies.add(editorSupport);
        cookies.add(saveSupport);
        cookies.add(new PXRWizardIterator());
        String fileID = file.getName();
        this.rootID = ComponentAddress.isValidID(fileID) ? fileID : "root";
        this.icon = Icons.defaultIcon();
        RP.execute(() -> {
            try {
                String script = file.asText();
                GraphModel model = GraphModel.parse(file.getParent().toURI(), script);
                EventQueue.invokeLater(() -> {
                    icon = Icons.getIcon(model.root().type());
                    rootID = model.root().id();
                    if (node != null) {
                        node.updateIcon();
                    }
                });
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        });
    }

    @Override
    protected Node createNodeDelegate() {
        node = new DataNodeImpl(this, getLookup());
        return node;
    }

    @Override
    public Lookup getLookup() {
        return getCookieSet().getLookup();
    }

    void preSave() {
        editorSupport.syncEditors();
    }

    @Override
    public boolean isCopyAllowed() {
        return false;
    }

    @Override
    public boolean isMoveAllowed() {
        return false;
    }

    @Override
    public boolean isRenameAllowed() {
        return false;
    }

    @Override
    protected void handleDelete() throws IOException {
        Project owner = FileOwnerQuery.getOwner(getPrimaryFile());
        if (owner != null) {
            PXRRootProxy proxy = PXRRootRegistry.findRootForFile(getPrimaryFile());
            String id = proxy == null ? rootID : proxy.getID();
            EventQueue.invokeLater(() -> {
                var props = owner.getLookup().lookup(ProjectProperties.class);
                if (props != null) {
                    try {
                        props.removeLine(ExecutionLevel.RUN, "/" + id + ".start");
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
                if (proxy != null) {
                    proxy.getHelper().removeComponent(ComponentAddress.of("/" + id));
                    proxy.dispose();
                }
            });

        }
        super.handleDelete();
    }

    static String findRootID(DataObject dob) {
        if (dob instanceof PXRDataObject pxrDob) {
            return pxrDob.rootID;
        } else {
            return "root";
        }
    }

    private class EditorSupport extends OpenSupport implements OpenCookie, CloseCookie {

        EditorSupport() {
            super(getPrimaryEntry());
        }

        @Override
        protected CloneableTopComponent createCloneableTopComponent() {
            return new RootEditorTopComponent(PXRDataObject.this);
        }

        private void syncEditors() {
            if (allEditors.isEmpty()) {
                return;
            }
            Enumeration<CloneableTopComponent> editors = allEditors.getComponents();
            while (editors.hasMoreElements()) {
                RootEditorTopComponent rootEditor = (RootEditorTopComponent) editors.nextElement();
                rootEditor.syncEditor();
            }
        }

    }

    private class SaveSupport implements SaveCookie, PropertyChangeListener {

        private SaveTask task;

        @Override
        public void save() throws IOException {
            if (task != null) {
                return;
            }
            task = SaveTask.createSaveTask(Set.of(PXRDataObject.this));
            task.addPropertyChangeListener(this);
            task.execute();
        }

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (task.getState() != State.RUNNING) {
                task.removePropertyChangeListener(this);
                task = null;
            }
        }

        @Override
        public String toString() {
            return PXRDataObject.this.getName();
        }
    }

    private static class DataNodeImpl extends DataNode {

        PXRDataObject dob;

        private DataNodeImpl(PXRDataObject dob, Lookup lookup) {
            super(dob, Children.LEAF, lookup);
            this.dob = dob;
            // add property change listener to dob
        }

        private void updateIcon() {
            fireIconChange();
        }

        @Override
        public Image getIcon(int type) {
            return dob.icon;
        }
    }
}
