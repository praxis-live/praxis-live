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
import java.io.OutputStreamWriter;
import org.netbeans.api.actions.Openable;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@Messages({
    "LBL_PXJ_LOADER=Files of PXJ"
})
@MIMEResolver.ExtensionRegistration(
        displayName = "#LBL_PXJ_LOADER",
        mimeType = "text/x-praxis-java",
        extension = {"pxj"}
)
@DataObject.Registration(
        mimeType = "text/x-praxis-java",
        iconBase = "net/neilcsmith/praxis/live/pxj/resources/pxj16.png",
        displayName = "#LBL_PXJ_LOADER",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/text/x-praxis-java/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/text/x-praxis-java/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ), /*@ActionReference(
 path="Loaders/text/x-praxis-java/Actions", 
 id=@ActionID(category="System", id="org.openide.actions.RenameAction"),
 position=700,
 separatorAfter=800
 ),*/})
public class PXJDataObject extends MultiDataObject {

    final static String PXJ_DOB_KEY = "PXJ_DOB";

    private final FileObject pxjFile;
    private FileObject javaProxy;
    private String defaultImports;
    private String classDeclaration;
    private String classEnding;

    public PXJDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        this.pxjFile = pf;
        getCookieSet().add(new Open());
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    private void openProxy() {
        try {
            if (javaProxy == null) {
                javaProxy = constructProxy();
            }
            DataObject dob = DataObject.find(javaProxy);
            Openable openable = dob.getLookup().lookup(Openable.class);
            if (openable != null) {
                openable.open();
            }
        } catch (Exception ex) {

        }
    }

    @Override
    protected void dispose() {
        super.dispose();
        disposeProxy();
    }

    void disposeProxy() {
        if (javaProxy != null) {
            try {
                javaProxy.delete();
                javaProxy = null;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void refreshDataFromProxy() {
        try {
            if (javaProxy == null) {
                return;
            }
            String data = javaProxy.asText();
            if (defaultImports != null) {
                data = data.replace(defaultImports, "");
            }
            if (classDeclaration != null) {
                data = data.replace(classDeclaration, "");
            }
            if (classEnding != null) {
                data = data.replace(classEnding, "");
            }
            try (OutputStreamWriter writer = new OutputStreamWriter(pxjFile.getOutputStream())) {
                writer.append(data);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private FileObject constructProxy() throws Exception {

        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject f = fs.getRoot().createData(pxjFile.getName(), "java");
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(f.getOutputStream());
            writer.append(constructProxyContent());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        f.setAttribute(PXJ_DOB_KEY, this);
        f.addFileChangeListener(new ProxyListener());
        return f;
    }

    private String constructProxyContent() {
        try {
            String fileContents = pxjFile.asText();
            boolean video = fileContents.contains("draw()");
            ClassBodyContext context = video ? new VideoClassBodyContext()
                    : new CoreClassBodyContext();
            StringBuilder sb = new StringBuilder();
            sb.append(getDefaultImports(context));
            sb.append("\n");
            sb.append(getClassDeclaration(context));
            sb.append("\n");
            sb.append(fileContents);
            sb.append("\n");
            sb.append(getClassEnding(context));

            return sb.toString();
        } catch (IOException ex) {
            return "";
        }

    }

    private String getDefaultImports(ClassBodyContext context) {
        if (defaultImports == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Default Imports\">");
            sb.append("//GEN-BEGIN:imports\n");
            for (String imp : context.getDefaultImports()) {
                sb.append("import ");
                sb.append(imp);
                sb.append(";\n");
            }
            sb.append("//</editor-fold>//GEN-END:imports\n");

            defaultImports = sb.toString();
        }
        return defaultImports;
    }

    private String getClassDeclaration(ClassBodyContext context) {
        if (classDeclaration == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Declaration\">");
            sb.append("//GEN-BEGIN:classdec\n");
            sb.append("class ");
            sb.append(pxjFile.getName());
            sb.append(" extends ");
            sb.append(context.getExtendedClass().getName());
            sb.append(" {\n");
            sb.append("//</editor-fold>//GEN-END:classdec\n");
            classDeclaration = sb.toString();
        }
        return classDeclaration;
    }

    private String getClassEnding(ClassBodyContext context) {
        if (classEnding == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Ending\">");
            sb.append("//GEN-BEGIN:classend\n");
            sb.append("}\n");
            sb.append("//</editor-fold>//GEN-END:classend\n");
            classEnding = sb.toString();
        }
        return classEnding;
    }

    private class Open implements OpenCookie {

        @Override
        public void open() {
            openProxy();
        }

    }

    private class ProxyListener extends FileChangeAdapter {

        @Override
        public void fileChanged(FileEvent fe) {
            if (fe.getFile() == javaProxy) {
                refreshDataFromProxy();
            } else {
                fe.getFile().removeFileChangeListener(this);
            }
        }

    }

}
