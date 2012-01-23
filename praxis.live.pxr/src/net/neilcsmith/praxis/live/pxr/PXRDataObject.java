/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr;

import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Collections;
import javax.swing.SwingUtilities;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.api.ComponentIconProvider;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.core.api.Task.State;
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

public class PXRDataObject extends MultiDataObject {

    public final static String KEY_ATTR_ROOT_TYPE = "rootType";
    private final static RequestProcessor RP = new RequestProcessor();
    private Image icon;
    private DataNodeImpl node;
    private ComponentType type;

    public PXRDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        CookieSet cookies = getCookieSet();
        cookies.add(new EditorSupport());
        cookies.add(new SaveSupport());
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
        if (type != null) {
            icon = Components.getIcon(type);
        } else {
            icon = null;
        }
        if (node != null) {
            node.updateType();
        }
    }

    ComponentType getType() {
        return type;
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
                ComponentType type = ComponentType.valueOf(attr.toString());
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
    }

    private class SaveSupport implements SaveCookie, PropertyChangeListener {

        private SaveTask task;

        @Override
        public void save() throws IOException {
//            RootProxy root = RootRegistry.getDefault().findRootForFile(getPrimaryFile());
//            if (root instanceof PXRRootProxy) {
//                PXRWriter.write(PXRDataObject.this, (PXRRootProxy) root);
//            } else {
//                NotifyDescriptor err = new NotifyDescriptor.Message("Unable to save file " + getName(), NotifyDescriptor.ERROR_MESSAGE);
//                DialogDisplayer.getDefault().notify(err);
//            }
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
