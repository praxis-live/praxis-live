/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.EditorKit;
import javax.swing.text.StyledDocument;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.spi.editor.guards.GuardedEditorSupport;
import org.netbeans.spi.editor.guards.GuardedSectionsFactory;
import org.netbeans.spi.editor.guards.GuardedSectionsProvider;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.CookieSet;
import org.openide.text.DataEditorSupport;

/**
 *
 * @author Neil C Smith
 */
class PXJavaEditorSupport extends DataEditorSupport implements
        OpenCookie, EditCookie, EditorCookie.Observable, LineCookie {
    
    private final static Logger LOG = Logger.getLogger(PXJavaEditorSupport.class.getName());

    private final PXJavaDataObject dob;
    private final CookieSet cookies;
    private final SaveImpl saveCookie;

    PXJavaEditorSupport(PXJavaDataObject dob, CookieSet cookies) {
        super(dob, new Env(dob));
        this.dob = dob;
        this.cookies = cookies;
        saveCookie = new SaveImpl();
    }

    @Override
    protected boolean notifyModified() {
        if (!super.notifyModified()) {
            return false;
        }
        addSaveCookie();
        return true;
    }

    @Override
    protected void notifyUnmodified() {
        super.notifyUnmodified();
        removeSaveCookie();
    }

    private void addSaveCookie() {
        if (dob.getCookie(SaveCookie.class) == null) {
            cookies.add(saveCookie);
            dob.setModified(true);
        }
    }

    private void removeSaveCookie() {
        SaveCookie cookie = dob.getCookie(SaveCookie.class);
        if (cookie == saveCookie) {
            cookies.remove(saveCookie);
            dob.setModified(false);
        }
    }

    @Override
    protected void notifyClosed() {
        LOG.fine("notifyClosed() called on PXJavaEditorSupport");
        super.notifyClosed();
        env.unmarkModified(); // needed to remove file lock
        Object pxj = dob.getPrimaryFile().getAttribute(PXJDataObject.PXJ_DOB_KEY);
        if (pxj instanceof PXJDataObject) {
            LOG.fine("disposing proxy on PXJDataObject");
            ((PXJDataObject) pxj).disposeProxy();
        }
    }

    
    
    private class SaveImpl implements SaveCookie {

        @Override
        public void save() throws IOException {
            saveDocument();
//            dob.setModified(false);
        }

    }

    private GuardedEditor guardedEditor;
    private GuardedSectionsProvider guardedProvider;

    @Override
    protected void loadFromStreamToKit(StyledDocument doc, InputStream stream, EditorKit kit) throws IOException, BadLocationException {
        
        LOG.fine("Loading PXJ Java proxy with guarded sections");

        FileObject file = dob.getPrimaryFile();

        if (guardedEditor == null) {
            guardedEditor = new GuardedEditor();
            GuardedSectionsFactory factory = GuardedSectionsFactory.find("text/x-java");
            if (factory != null) {
                LOG.fine("Found GuardedSectionsFactory");
                guardedProvider = factory.create(guardedEditor);
            }
        }

        if (guardedProvider != null) {
            LOG.fine("Loading file through GuardedSectionsProvider");
            guardedEditor.doc = doc;
            Charset c = FileEncodingQuery.getEncoding(file);
            Reader reader = guardedProvider.createGuardedReader(stream, c);
            try {
                kit.read(reader, doc, 0);
            } finally {
                reader.close();
            }
        } else {
            super.loadFromStreamToKit(doc, stream, kit);
        }

    }

    @Override
    protected void saveFromKitToStream(StyledDocument doc, EditorKit kit, OutputStream stream) throws IOException, BadLocationException {
        if (guardedProvider != null) {
            Charset c = FileEncodingQuery.getEncoding(this.getDataObject().getPrimaryFile());
            Writer writer = guardedProvider.createGuardedWriter(stream, c);
            try {
                kit.write(writer, doc, 0, doc.getLength());
            } finally {
                writer.close();
            }
        } else {
            super.saveFromKitToStream(doc, kit, stream);
        }
    }

    private static class GuardedEditor implements GuardedEditorSupport {

        private StyledDocument doc;

        @Override
        public StyledDocument getDocument() {
            return doc;
        }

    }

    private static class Env extends DataEditorSupport.Env {

        private Env(DataObject obj) {
            super(obj);
        }

        @Override
        protected FileObject getFile() {
            return getDataObject().getPrimaryFile();
        }

        @Override
        protected FileLock takeLock() throws IOException {
            return getFile().lock();
        }

    }
}
