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
package org.praxislive.ide.pxr;

import java.awt.EventQueue;
import java.awt.Image;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.text.DataEditorSupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.praxislive.ide.components.api.Icons;
import org.praxislive.project.GraphModel;

@NbBundle.Messages({
    "LBL_Root_Template_Loader=Root Template Files"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_Root_Template_Loader",
        mimeType = TemplateDataObject.MIME_TYPE,
        extension = {"pxx"})
@DataObject.Registration(
        mimeType = TemplateDataObject.MIME_TYPE,
        iconBase = "org/praxislive/ide/pxr/resources/pxr16.png",
        displayName = "#LBL_Root_Template_Loader",
        position = 301)
@ActionReferences({
    @ActionReference(
            path = TemplateDataObject.ACTION_PATH,
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200),
    @ActionReference(
            path = TemplateDataObject.ACTION_PATH,
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600),
    @ActionReference(
            path = TemplateDataObject.ACTION_PATH,
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200),
    @ActionReference(
            path = TemplateDataObject.ACTION_PATH,
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300),
    @ActionReference(
            path = TemplateDataObject.ACTION_PATH,
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400)
})
public class TemplateDataObject extends MultiDataObject {

    static final String MIME_TYPE = "text/x-praxis-root-template";
    static final String ACTION_PATH = "Loaders/text/" + MIME_TYPE + "/Actions";

    private final static RequestProcessor RP = new RequestProcessor();

    String rootID;
    private Image icon;
    private DataNodeImpl node;

    public TemplateDataObject(FileObject file, MultiFileLoader loader) throws DataObjectExistsException {
        super(file, loader);
        CookieSet cookies = getCookieSet();
        cookies.add((Node.Cookie) DataEditorSupport.create(this, getPrimaryEntry(), cookies));
        cookies.add(new PXRWizardIterator());
        icon = Icons.defaultIcon();
        updateIconAndType();
        file.addFileChangeListener(new FileChangeAdapter() {
            @Override
            public void fileChanged(FileEvent fe) {
                updateIconAndType();
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

    private void updateIconAndType() {
        FileObject file = getPrimaryFile();
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

    private static class DataNodeImpl extends DataNode {

        TemplateDataObject dob;

        private DataNodeImpl(TemplateDataObject dob, Lookup lookup) {
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
