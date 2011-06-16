/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
