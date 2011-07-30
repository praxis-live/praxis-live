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

import java.io.IOException;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
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
import org.openide.util.Lookup;
import org.openide.windows.CloneableTopComponent;

public class PXRDataObject extends MultiDataObject {

    public PXRDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        CookieSet cookies = getCookieSet();
        cookies.add(new EditorSupport());
        cookies.add(new SaveSupport());
    }

    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }

    @Override
    public Lookup getLookup() {
        return getCookieSet().getLookup();
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

    private class SaveSupport implements SaveCookie {

        @Override
        public void save() throws IOException {
            RootProxy root = RootRegistry.getDefault().findRootForFile(getPrimaryFile());
            if (root instanceof PXRRootProxy) {
                PXRWriter.write(PXRDataObject.this, (PXRRootProxy) root);
            } else {
                NotifyDescriptor err = new NotifyDescriptor.Message("Unable to save file " + getName(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(err);
            }
        }

    }

}
