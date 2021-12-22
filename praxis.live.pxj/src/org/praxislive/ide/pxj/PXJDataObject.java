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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    ), /*@ActionReference(
 path="Loaders/text/x-praxis-java/Actions", 
 id=@ActionID(category="System", id="org.openide.actions.RenameAction"),
 position=700,
 separatorAfter=800
 ),*/})
public class PXJDataObject extends MultiDataObject {

    private static final Logger LOG = Logger.getLogger(PXJDataObject.class.getName());

    static final String PXJ_DOB_KEY = "PXJ_DOB";
    static final String CONTROL_ADDRESS_KEY = "controlAddress";
    static final String PROJECT_KEY = "project";
    private static final String ARGUMENT_INFO_KEY = "argumentInfo";
    private static final String BASE_CLASS_KEY = "base-class";
    private static final String BASE_IMPORTS_KEY = "base-imports";

    private final FileObject pxjFile;
    private final ClassBodyInfo classBodyInfo;
    private final ControlAddress controlAddress;
    private final PraxisProject project;

    private FileObject javaProxy;
    private DynamicPaths.Key pathsKey;
    private String defaultImports;
    private String classDeclaration;
    private String classEnding;

    public PXJDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        this.pxjFile = pf;
        classBodyInfo = buildClassBodyInfo(pf);
        controlAddress = findControlAddress(pf);
        project = findProject(pf);
        getCookieSet().add(new Open());
    }

    private ClassBodyInfo buildClassBodyInfo(FileObject f) {
        Object attr = f.getAttribute(ARGUMENT_INFO_KEY);
        if (attr instanceof ArgumentInfo) {
            ArgumentInfo info = (ArgumentInfo) attr;
            String baseClassName = Optional.ofNullable(info.properties().get(BASE_CLASS_KEY))
                    .map(Value::toString)
                    .orElse("java.lang.Object");
            List<String> baseImports = Optional.ofNullable(info.properties().get(BASE_IMPORTS_KEY))
                    .flatMap(PArray::from)
                    .orElse(PArray.EMPTY)
                    .stream()
                    .map(Value::toString)
                    .collect(Collectors.toList());
            return new ClassBodyInfo(baseClassName, baseImports);
        } else {
            return new ClassBodyInfo("java.lang.Object", List.of());
        }
    }

    private ControlAddress findControlAddress(FileObject f) {
        Object o = f.getAttribute(CONTROL_ADDRESS_KEY);
        if (o instanceof ControlAddress) {
            return (ControlAddress) o;
        }
        return null;
    }

    private PraxisProject findProject(FileObject f) {
        Object o = f.getAttribute(PROJECT_KEY);
        if (o instanceof PraxisProject) {
            return (PraxisProject) o;
        } else {
            return null;
        }
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
        if (javaProxy != null) {
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
            if (defaultImports != null && !defaultImports.isEmpty()) {
                data = data.replace(defaultImports, "");
            }
            if (classDeclaration != null) {
                data = data.replace(classDeclaration, "");
            }
//            if (classEnding != null) {
//                data = data.replace(classEnding, "");
//            }
            int lastFold = data.lastIndexOf("}");
            if (lastFold > 0) {
                data = data.substring(0, lastFold);
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
        f.setAttribute(PROJECT_KEY, project);
        f.setAttribute(CONTROL_ADDRESS_KEY, controlAddress);
        f.addFileChangeListener(new ProxyListener());
        return f;
    }

    private String constructProxyContent() {
        try {
            String fileContents = pxjFile.asText();
            BufferedReader r = new BufferedReader(new StringReader(fileContents));
            String[] extraImports = parseImportDeclarations(r);
            StringBuilder sb = new StringBuilder();
            sb.append(getDefaultImports());
            for (String extraImp : extraImports) {
                sb.append("import ").append(extraImp).append(";\n");
            }
            sb.append("\n");
            sb.append(getClassDeclaration());
            sb.append("\n");
            String line;
            boolean top = true;
            while ((line = r.readLine()) != null) {
                if (top) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    top = false;
                }
                sb.append(line).append('\n');
            }

            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            sb.append(getClassEnding());

            return sb.toString();
        } catch (IOException ex) {
            return "";
        }

    }

    private String getDefaultImports() {
        if (defaultImports == null) {
            List<String> imports = classBodyInfo.baseImports;
            if (!imports.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Default Imports\">");
                sb.append("//GEN-BEGIN:imports");
                for (String imp : imports) {
                    sb.append("\nimport ");
                    sb.append(imp);
                    sb.append(";");
                }
                sb.append("//</editor-fold> --//GEN-END:imports\n");
                defaultImports = sb.toString();
            } else {
                defaultImports = "";
            }
        }
        return defaultImports;
    }

    private String getClassDeclaration() {
        if (classDeclaration == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Declaration\">");
            sb.append("//GEN-BEGIN:classdec\n");
            sb.append("class ");
            sb.append(pxjFile.getName());
            sb.append(" extends ");
            sb.append(classBodyInfo.baseClassName);
            // @TODO support interfaces
            sb.append(" {\n");
            sb.append("//</editor-fold> --//GEN-END:classdec\n");
            classDeclaration = sb.toString();
        }
        return classDeclaration;
    }

    private String getClassEnding() {
        if (classEnding == null) {
//            StringBuilder sb = new StringBuilder();
//            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Ending\">");
//            sb.append("//GEN-BEGIN:classend\n");
//            sb.append("}\n");
//            sb.append("//</editor-fold>//GEN-END:classend\n");
//            classEnding = sb.toString();
            classEnding = "}//GEN-BEGIN:classend\n//GEN-END:classend";
        }
        return classEnding;
    }

    /**
     * Copied from Janino ClassBodyEvaluator
     *
     * Heuristically parse IMPORT declarations at the beginning of the character
     * stream produced by the given {@link Reader}. After this method returns,
     * all characters up to and including that last IMPORT declaration have been
     * read from the {@link Reader}.
     * <p>
     * This method does not handle comments and string literals correctly, i.e.
     * if a pattern that looks like an IMPORT declaration appears within a
     * comment or a string literal, it will be taken as an IMPORT declaration.
     *
     * @param r A {@link Reader} that supports MARK, e.g. a
     * {@link BufferedReader}
     * @return The parsed imports, e.g. {@code { "java.util.*", "static java.util.Map.Entry"
     * }}
     */
    private static String[]
            parseImportDeclarations(Reader r) throws IOException {
        final CharBuffer cb = CharBuffer.allocate(10000);
        r.mark(cb.limit());
        r.read(cb);
        cb.rewind();

        List<String> imports = new ArrayList<String>();
        int afterLastImport = 0;
        for (Matcher matcher = IMPORT_STATEMENT_PATTERN.matcher(cb); matcher.find();) {
            imports.add(matcher.group(1));
            afterLastImport = matcher.end();
        }
        r.reset();
        r.skip(afterLastImport);
        return imports.toArray(new String[imports.size()]);
    }
    private static final Pattern IMPORT_STATEMENT_PATTERN = Pattern.compile(
            "\\bimport\\s+"
            + "("
            + "(?:static\\s+)?"
            + "[\\p{javaLowerCase}\\p{javaUpperCase}_\\$][\\p{javaLowerCase}\\p{javaUpperCase}\\d_\\$]*"
            + "(?:\\.[\\p{javaLowerCase}\\p{javaUpperCase}_\\$][\\p{javaLowerCase}\\p{javaUpperCase}\\d_\\$]*)*"
            + "(?:\\.\\*)?"
            + ");"
    );

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

    private static class ClassBodyInfo {

        private final String baseClassName;
        private final List<String> baseImports;

        public ClassBodyInfo(String baseClassName, List<String> baseImports) {
            this.baseClassName = baseClassName;
            this.baseImports = List.copyOf(baseImports);
        }

    }

}
