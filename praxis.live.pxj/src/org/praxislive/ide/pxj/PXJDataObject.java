/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2022 Neil C Smith.
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
package org.praxislive.ide.pxj;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.actions.Savable;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.cookies.EditorCookie;
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
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.ide.code.api.ClassBodyWrapper;
import org.praxislive.ide.code.api.DynamicPaths;
import org.praxislive.ide.code.api.SharedCodeInfo;
import org.praxislive.ide.model.HubProxy;
import org.praxislive.ide.model.RootProxy;

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
        iconBase = "org/praxislive/ide/pxj/resources/pxj16.png",
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
    )
})
public class PXJDataObject extends MultiDataObject {

    static final String PXJ_DOB_KEY = "PXJ_DOB";
    static final String CONTROL_ADDRESS_KEY = "controlAddress";
    static final String PROJECT_KEY = "project";
    private static final String ARGUMENT_INFO_KEY = "argumentInfo";
    private static final String BASE_CLASS_KEY = "base-class";
    private static final String BASE_IMPORTS_KEY = "base-imports";
    private static final String NEW_LINE = "\n";

    private final FileObject pxjFile;
    private final String baseClassName;
    private final List<String> baseImports;
    private final ControlAddress controlAddress;
    private final PraxisProject project;

    private FileObject javaProxy;
    private DynamicPaths.Key pathsKey;
    private String defaultImports;
    private String classDeclaration;
    private final String classEnding;
    private String superClassName;
    private boolean inRefresh;

    public PXJDataObject(FileObject f, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(f, loader);
        this.pxjFile = f;
        Object attr = f.getAttribute(ARGUMENT_INFO_KEY);
        if (attr instanceof ArgumentInfo) {
            ArgumentInfo info = (ArgumentInfo) attr;
            baseClassName = Optional.ofNullable(info.properties().get(BASE_CLASS_KEY))
                    .map(Value::toString)
                    .orElse("java.lang.Object");
            baseImports = Optional.ofNullable(info.properties().get(BASE_IMPORTS_KEY))
                    .flatMap(PArray::from)
                    .orElse(PArray.EMPTY)
                    .stream()
                    .map(Value::toString)
                    .collect(Collectors.toUnmodifiableList());
        } else {
            baseClassName = "java.lang.Object";
            baseImports = List.of();
        }
        attr = f.getAttribute(CONTROL_ADDRESS_KEY);
        controlAddress = attr instanceof ControlAddress ? (ControlAddress) attr : null;
        attr = f.getAttribute(PROJECT_KEY);
        project = attr instanceof PraxisProject ? (PraxisProject) attr : null;
        getCookieSet().add(new Open());
        classEnding = constructClassEnding();
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    private void openProxy() {
        try {
            if (javaProxy == null) {
                javaProxy = constructProxy();
                pathsKey = DynamicPaths.getDefault().register(project,
                        javaProxy.getFileSystem().getRoot(),
                        findSharedInfo());
            }
            DataObject dob = DataObject.find(javaProxy);
            Openable openable = dob.getLookup().lookup(Openable.class);
            if (openable != null) {
                openable.open();
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private SharedCodeInfo findSharedInfo() {
        HubProxy hub = project.getLookup().lookup(HubProxy.class);
        if (hub != null) {
            RootProxy root = hub.getRoot(controlAddress.component().rootID());
            if (root != null) {
                return root.getLookup().lookup(SharedCodeInfo.class);
            }
        }
        return null;
    }

    @Override
    protected void dispose() {
        super.dispose();
        disposeProxy();
    }

    void disposeProxy() {
        if (javaProxy != null && !inRefresh) {
            try {
                pathsKey.unregister();
                pathsKey = null;
                FileObject file = javaProxy;
                javaProxy = null; // make sure listener ignored
                closeEditors(file);
                file.delete();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void closeEditors(FileObject file) {
        try {
            var dob = DataObject.find(file);
            var savable = dob.getLookup().lookup(Savable.class);
            if (savable != null) {
                savable.save();
            }
            var editor = dob.getLookup().lookup(EditorCookie.class);
            if (editor != null) {
                editor.close();
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void refreshDataFromProxy() {
        try {
            if (javaProxy == null) {
                return;
            }
            String data = javaProxy.asText();
            if (superClassName != null) {
                data = "extends " + superClassName + ";" + NEW_LINE + data;
            }
            if (defaultImports != null && !defaultImports.isEmpty()) {
                data = data.replace(defaultImports, "");
            }
            if (classDeclaration != null) {
                data = data.replace(classDeclaration, "");
            }
            int lastFold = data.lastIndexOf("}");
            if (lastFold > 0) {
                data = data.substring(0, lastFold);
            }
            try ( OutputStreamWriter writer = new OutputStreamWriter(pxjFile.getOutputStream())) {
                writer.append(data);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    private FileObject constructProxy() throws Exception {

        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject f = fs.getRoot().createData(pxjFile.getName(), "java");
        try ( OutputStreamWriter writer = new OutputStreamWriter(f.getOutputStream())) {
            writer.append(constructProxyContent(pxjFile.asText(), false));
        }
        f.setAttribute(PXJ_DOB_KEY, this);
        f.setAttribute(PROJECT_KEY, project);
        f.setAttribute(CONTROL_ADDRESS_KEY, controlAddress);
        f.addFileChangeListener(new ProxyListener());
        return f;
    }

    private String constructProxyContent(String source, boolean ignoreExtends) {
        defaultImports = null;
        classDeclaration = null;

        return ClassBodyWrapper.create()
                .className(pxjFile.getName())
                .extendsType(superClassName == null ? baseClassName : superClassName)
                .defaultImports(baseImports)
                .ignoreExtends(ignoreExtends)
                .filter(new ClassBodyWrapper.Filter() {
                    @Override
                    public void writeDefaultImports(StringBuilder sb, List<String> imports) {
                        defaultImports = constructDefaultImports(imports);
                        sb.append(defaultImports);
                    }

                    @Override
                    public void writeClassDeclaration(StringBuilder sb, String className, String extendedType, List<String> implementedTypes) {
                        if (extendedType.equals(baseClassName)) {
                            superClassName = null;
                        } else {
                            superClassName = extendedType;
                        }
                        sb.append(NEW_LINE);
                        classDeclaration = constructClassDeclaration(className, extendedType);
                        sb.append(classDeclaration);
                    }

                    @Override
                    public void writeClassEnding(StringBuilder sb) {
                        sb.append(classEnding);
                    }

                })
                .wrap(source);

    }

    private void refreshProxy() {
        if (javaProxy == null) {
            assert false;
            return;
        }
        inRefresh = true;
        closeEditors(javaProxy);
        try ( OutputStreamWriter writer = new OutputStreamWriter(javaProxy.getOutputStream())) {
            writer.append(constructProxyContent(pxjFile.asText(), true));
            EventQueue.invokeLater(this::openProxy);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            inRefresh = false;
        }
    }

    void setBaseType(String baseType) {
        String type = baseType == null ? baseClassName : baseType.strip();
        if (type.isBlank() || type.equals(baseClassName)) {
            if (superClassName != null) {
                superClassName = null;
                refreshProxy();
            }
        } else if (!type.equals(superClassName)) {
            superClassName = type;
            refreshProxy();
        }
    }

    String getBaseType() {
        return superClassName;
    }

    private static String constructDefaultImports(List<String> imports) {
        if (!imports.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Default Imports\">");
            sb.append("//GEN-BEGIN:imports");
            imports.forEach(i -> sb.append(NEW_LINE)
                    .append("import ")
                    .append(i)
                    .append(";")
            );
            sb.append("//</editor-fold> --//GEN-END:imports").append(NEW_LINE);
            return sb.toString();
        } else {
            return "";
        }
    }

    private static String constructClassDeclaration(String className, String superclass) {
        StringBuilder sb = new StringBuilder();
        sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Declaration\">");
        sb.append("//GEN-BEGIN:classdec").append(NEW_LINE);
        sb.append("class ");
        sb.append(className);
        sb.append(" extends ");
        sb.append(superclass);
        // @TODO support interfaces
        sb.append(" {").append(NEW_LINE);
        sb.append("//</editor-fold> --//GEN-END:classdec").append(NEW_LINE);
        return sb.toString();
    }

    private static String constructClassEnding() {
        return "}//GEN-BEGIN:classend" + NEW_LINE + "//GEN-END:classend";
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
