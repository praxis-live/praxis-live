/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.praxislive.code.ClassBodyContext;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.api.actions.Openable;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.JavaClassPathConstants;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
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
import org.praxislive.ide.project.api.ProjectProperties;

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

    private final static Logger LOG = Logger.getLogger(PXJDataObject.class.getName());

    final static String PXJ_DOB_KEY = "PXJ_DOB";
    final static String CONTROL_ADDRESS_KEY = "controlAddress";
    final static String PROJECT_KEY = "project";
    private final static String ARGUMENT_INFO_KEY = "argumentInfo";

    private final FileObject pxjFile;
    private final ClassBodyContext<?> classBodyContext;
    private final ControlAddress controlAddress;
    private final PraxisProject project;
    private final ProjectProperties projectProps;
    private final LibsListener libsListener;
    private final ClassPath sourceClasspath;
    
    private FileObject javaProxy;
    private String defaultImports;
    private String classDeclaration;
    private String classEnding;
    private ClassPath compileClasspath;

    public PXJDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        this.pxjFile = pf;
        classBodyContext = findClassBodyContext(pf);
        controlAddress = findControlAddress(pf);
        project = findProject(pf);
        projectProps = project == null ? null : findProjectProperties(project);
        libsListener = new LibsListener();
        if (projectProps != null) {
            projectProps.addPropertyChangeListener(libsListener);
        }
        getCookieSet().add(new Open());
        sourceClasspath = ClassPathSupport.createClassPath(pf.getParent());
        compileClasspath = createCompileClasspath(projectProps);
    }

    private ClassBodyContext<?> findClassBodyContext(FileObject f) {
        try {
            Object o = f.getAttribute(ARGUMENT_INFO_KEY);
            if (o instanceof ArgumentInfo) {
                o = ((ArgumentInfo) o).getProperties().get(ClassBodyContext.KEY);
                if (o != null) {
                    Class<?> cls = Class.forName(o.toString(), true, Thread.currentThread().getContextClassLoader());
                    if (cls != null && ClassBodyContext.class.isAssignableFrom(cls)) {
                        o = cls.getDeclaredConstructor().newInstance();
                        return (ClassBodyContext<?>) o;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "", ex);
        }
        return null;
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

    private ProjectProperties findProjectProperties(PraxisProject project) {
        return project.getLookup().lookup(ProjectProperties.class);
    }

    private ClassPath createCompileClasspath(ProjectProperties props) {
        List<FileObject> libs = props.getLibraries();
        if (libs.isEmpty()) {
            return ClassPathRegistry.getInstance().getCompileClasspath();
        } else {
            ClassPath libCP = ClassPathSupport.createClassPath(
                    libs.stream()
                            .filter(f -> f.isData() && f.hasExt("jar"))
                            .map(f -> FileUtil.urlForArchiveOrDir(FileUtil.toFile(f)))
                            .toArray(URL[]::new));
            return ClassPathSupport.createProxyClassPath(libCP,
                    ClassPathRegistry.getInstance().getCompileClasspath());
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
        if (projectProps != null) {
            projectProps.removePropertyChangeListener(libsListener);
        }
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

    int getJavaRelease() {
        return projectProps.getJavaRelease();
    }
    
    ClassPath getClassPath(String type) {
        switch (type) {
            case ClassPath.BOOT:
            case JavaClassPathConstants.MODULE_BOOT_PATH:
                return ClassPathRegistry.getInstance().getBootClasspath();
            case ClassPath.COMPILE:
                return compileClasspath;
            case ClassPath.SOURCE:
                return sourceClasspath;
            default:
                return null;
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
        f.setAttribute(CONTROL_ADDRESS_KEY, controlAddress);
        f.addFileChangeListener(new ProxyListener());
        return f;
    }

    private String constructProxyContent() {
        try {
            String fileContents = pxjFile.asText();
            BufferedReader r = new BufferedReader(new StringReader(fileContents));
            ClassBodyContext<?> context = classBodyContext;
            if (context == null) {
                // what now?
            }
            String[] extraImports = parseImportDeclarations(r);
            StringBuilder sb = new StringBuilder();
            sb.append(getDefaultImports(context));
            for (String extraImp : extraImports) {
                sb.append("import ").append(extraImp).append(";\n");
            }
            sb.append("\n");
            sb.append(getClassDeclaration(context));
            sb.append("\n");
            String line;
            boolean top = true;
            while ((line = r.readLine()) != null) {
                if (top) {
                    if ( line.trim().isEmpty()) {
                        continue;
                    }
                    top = false;
                }
                sb.append(line).append('\n');
            }

            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append('\n');
            }
            sb.append(getClassEnding(context));

            return sb.toString();
        } catch (IOException ex) {
            return "";
        }

    }

    private String getDefaultImports(ClassBodyContext<?> context) {
        if (defaultImports == null) {
            String[] importList = context.getDefaultImports();
            if (importList.length > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Default Imports\">");
                sb.append("//GEN-BEGIN:imports");
                for (String imp : importList) {
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

    private String getClassDeclaration(ClassBodyContext<?> context) {
        if (classDeclaration == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("//<editor-fold defaultstate=\"collapsed\" desc=\"Class Declaration\">");
            sb.append("//GEN-BEGIN:classdec\n");
            sb.append("class ");
            sb.append(pxjFile.getName());
            sb.append(" extends ");
            sb.append(context.getExtendedClass().getName());
            // @TODO support interfaces
            sb.append(" {\n");
            sb.append("//</editor-fold> --//GEN-END:classdec\n");
            classDeclaration = sb.toString();
        }
        return classDeclaration;
    }

    private String getClassEnding(ClassBodyContext<?> context) {
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

    private class LibsListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (ProjectProperties.PROP_LIBRARIES.equals(evt.getPropertyName())) {
                compileClasspath = createCompileClasspath(projectProps);
            }
        }

    }

}
