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

import java.awt.EventQueue;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.SwingUtilities;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.Components;
import org.praxislive.ide.core.api.Task.State;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.CloseCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.loaders.OpenSupport;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.openide.nodes.Children;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.windows.CloneableTopComponent;
import org.praxislive.core.ComponentAddress;
import org.praxislive.ide.components.api.Icons;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.project.api.ExecutionLevel;
import org.praxislive.ide.project.api.ProjectProperties;

public class PXRDataObject extends MultiDataObject {

    public final static String KEY_ATTR_ROOT_TYPE = "rootType";
    private final static RequestProcessor RP = new RequestProcessor();
    
    private final EditorSupport editorSupport;
    private final SaveSupport saveSupport;
    
    private Image icon;
    private DataNodeImpl node;
    private ComponentType type;

    public PXRDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        editorSupport = new EditorSupport();
        saveSupport = new SaveSupport();
        CookieSet cookies = getCookieSet();
        cookies.add(editorSupport);
        cookies.add(saveSupport);
        initType(pf);
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

    void setType(ComponentType type) {
        if (this.type == type) {
            return;
        }
        this.type = type;
        icon = null;
        if (type != null) {
            var project = FileOwnerQuery.getOwner(getPrimaryFile());
            if (project != null) {
                var components = project.getLookup().lookup(Components.class);
                if (components != null) {
                    icon = components.getIcon(type);
                }
            }
            if (icon == null) {
                icon = Icons.getIcon(type);
            }
        }
        if (node != null) {
            node.updateType();
        }
    }

    ComponentType getType() {
        return type;
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
            FileObject file = owner.getProjectDirectory();
            file = file.getFileObject("config");
            if (file != null) {
                file = file.getFileObject(getName() + "_autostart");
                if (file != null) {
                    file.delete();
                }
            }
            var proxy = PXRRootRegistry.findRootForFile(getPrimaryFile());
            var id = getName();
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
                    proxy.getHelper().removeComponent(
                            ComponentAddress.of("/" + id), Callback.create(r -> {}));
                    proxy.dispose();
                }
            });

        }
        super.handleDelete();
    }

//    private Image findIcon(ComponentType type) {
//        try {
//            for (ComponentIconProvider provider : Lookup.getDefault().lookupAll(ComponentIconProvider.class)) {
//                Image img = provider.getIcon(type);
//                if (img != null) {
//                    return img;
//                }
//            }
//        } catch (Exception ex) {
//            //fall through
//        }
//        return null;
//    }
    private void initType(final FileObject file) {
        Object attr = file.getAttribute(KEY_ATTR_ROOT_TYPE);
        if (attr instanceof String) {
            try {
                ComponentType type = ComponentType.of(attr.toString());
                setType(type);
                return;
            } catch (Exception ex) {
                // fall through
            }
        }

        // no type attribute found
        RP.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    String script = file.asText();
                    final PXRParser.RootElement root = PXRParser.parse(script);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            setType(root.type);
                            try {
                                file.setAttribute(KEY_ATTR_ROOT_TYPE, root.type.toString());
                            } catch (IOException ex) {
                                // do nothing
                            }
                        }
                    });
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });


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
            task = SaveTask.createSaveTask(Collections.singleton(PXRDataObject.this));
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

        private void updateType() {
            fireIconChange();
        }

        @Override
        public Image getIcon(int type) {
            Image ret = dob.icon;
            if (ret == null) {
                return super.getIcon(type);
            } else {
                return ret;
            }

        }
    }
}
