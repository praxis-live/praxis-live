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
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.praxislive.core.ControlAddress;
import org.praxislive.core.ArgumentInfo;
import org.praxislive.core.ControlInfo;
import org.praxislive.core.types.PString;
import org.praxislive.ide.project.api.PraxisProject;
import org.netbeans.api.actions.Openable;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.praxislive.core.Value;
import org.praxislive.core.types.PArray;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.code.api.ClassBodyWrapper;
import org.praxislive.ide.core.api.Callback;
import org.praxislive.ide.model.HubProxy;
import org.praxislive.ide.model.RootProxy;
import org.praxislive.ide.properties.PraxisProperty;

@NbBundle.Messages({
    "# {0} - property id",
    "EditCodeLabel=Edit {0}",
    "# {0} - property id",
    "ResetCodeLabel=Reset {0}",
    "CreateSharedBaseLabel=Create shared base",
    "EditSharedBaseLabel=Edit shared base",
    "SharedBaseClassLabel=Shared base class name.",
    "# {0} - class name",
    "SharedBaseClassError=Error creating shared base class {0}."
})
class BoundCodeProperty extends BoundArgumentProperty {

    static final String KEY_LAST_SAVED = "last-saved";

    private static final Map<String, String> mimeToExt = new HashMap<>();
    private static final String PXJ_MIME = "text/x-praxis-java";

    static {
        // include common
        mimeToExt.put(PXJ_MIME, "pxj");
        mimeToExt.put("text/x-praxis-script", "pxs");
        // GLSL mime types in Praxis core don't match editor mime type (text/x-glsl)
        mimeToExt.put("text/x-glsl-frag", "frag");
        mimeToExt.put("text/x-glsl-vert", "vert");
    }

    private final PraxisProject project;
    private final String mimeType;
    private final String template;
    private final FileListener fileListener;
    private final String fileName;
    private final Action editAction;
    private final Action resetAction;
    private final Action quickEditAction;
    private final SharedBaseAction sharedBaseAction;

    private FileObject file;

    BoundCodeProperty(PraxisProject project, ControlAddress address, ControlInfo info, String mimeType) {
        super(project, address, info);
        this.project = project;
        this.mimeType = mimeType;
        this.template = info.outputs().get(0).properties().getString(ArgumentInfo.KEY_TEMPLATE, "");
        this.fileListener = new FileListener();
        fileName = safeFileName(address);
        String id = address.controlID();
        editAction = new EditAction(id);
        resetAction = new ResetAction(id);
        quickEditAction = new QuickEditAction(id);
        setHidden(true);
        if (PXJ_MIME.equals(mimeType)) {
            sharedBaseAction = new SharedBaseAction(info.outputs().get(0));
            addPropertyChangeListener(sharedBaseAction);
            sharedBaseAction.checkForBase();
        } else {
            sharedBaseAction = null;
        }
    }

    private String safeFileName(ControlAddress address) {
        String name = address.component().componentID() + "_" + address.controlID();
        name = name.replace('-', '_');
        return name;
    }

    @Override
    public void restoreDefaultValue() {
        super.restoreDefaultValue();
        deleteFile();
    }

    @Override
    public void dispose() {
        super.dispose();
        deleteFile();
    }

    Action getEditAction() {
        return editAction;
    }

    Action getResetAction() {
        return resetAction;
    }

    Action getQuickEditAction() {
        return quickEditAction;
    }

    Action getSharedBaseAction() {
        return sharedBaseAction;
    }

    String getCode() {
        String code = getValue().toString();
        if (code.isBlank()) {
            code = template;
        }
        return code;
    }

    private void openEditor() {
        try {
            if (file == null) {
                file = constructFile();
            }
            DataObject dob = DataObject.find(file);
            Openable openable = dob.getLookup().lookup(Openable.class);
            if (openable != null) {
                openable.open();
            }
        } catch (Exception ex) {
        }
    }

    private FileObject constructFile() throws Exception {

        FileSystem fs = FileUtil.createMemoryFileSystem();
        FileObject f;
        String ext = findExtension(mimeType);
        if (ext != null) {
            f = fs.getRoot().createData(fileName, ext);
        } else {
            f = fs.getRoot().createData(fileName);
        }

        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(f.getOutputStream());
            writer.append(constructFileContent());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        f.setAttribute("project", project);
        f.setAttribute("controlAddress", getAddress());
        f.setAttribute("argumentInfo", getInfo().outputs().get(0));
        f.addFileChangeListener(fileListener);
        return f;
    }

    private String findExtension(String mimeType) {
        if (mimeToExt.containsKey(mimeType)) {
            return mimeToExt.get(mimeType);
        }
        List<String> exts = FileUtil.getMIMETypeExtensions(mimeType);
        String ext = null;
        if (exts.size() > 0) {
            ext = exts.get(0);
        }
        mimeToExt.put(mimeType, ext);
        return ext;
    }

    private String constructFileContent() {
        var content = getValue().toString();
        if (content.isBlank()) {
            var lastSaved = getValue(KEY_LAST_SAVED);
            if (lastSaved instanceof Value) {
                content = lastSaved.toString();
            }
        }
        if (content.isBlank()) {
            content = template;
        }
        return content;
    }

    private void updateFromFile() {
        if (file != null) {
            try {
                setValue(PString.of(file.asText()));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private void deleteFile() {
        if (file == null) {
            return;
        }
        try {
            file.delete();
            file = null;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private BoundSharedCodeProperty findSharedCodeProperty() {
        try {
            HubProxy hub = project.getLookup().lookup(HubProxy.class);
            RootProxy root = hub.getRoot(getAddress().component().rootID());
            PraxisProperty<?> property = root.getProperty("shared-code");
            if (property instanceof BoundSharedCodeProperty) {
                return (BoundSharedCodeProperty) property;
            }
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    class FileListener extends FileChangeAdapter {

        @Override
        public void fileChanged(final FileEvent fe) {
            if (EventQueue.isDispatchThread()) {
                if (fe.getFile() == file) {
                    updateFromFile();
                } else {
                    fe.getFile().removeFileChangeListener(this);
                }
            } else {
                EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        fileChanged(fe);
                    }
                });
            }

        }

    }

    class EditAction extends AbstractAction {

        private EditAction(String id) {
            super(Bundle.EditCodeLabel(id));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            openEditor();
        }

    }

    class QuickEditAction extends AbstractAction {

        private QuickEditAction(String id) {
            super(Bundle.EditCodeLabel(id));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (sharedBaseAction != null) {
                if (sharedBaseAction.openSharedBase()) {
                    return;
                }
            }
            editAction.actionPerformed(e);
        }

    }

    class ResetAction extends AbstractAction {

        private ResetAction(String id) {
            super(Bundle.ResetCodeLabel(id));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Object last = BoundCodeProperty.this.getValue(KEY_LAST_SAVED);
            BoundCodeResetPanel panel = new BoundCodeResetPanel(last instanceof Value);
            DialogDescriptor dialog = new DialogDescriptor(panel, getName());
            if (DialogDisplayer.getDefault().notify(dialog) == NotifyDescriptor.OK_OPTION) {
                if (panel.isLastSavedOption()) {
                    setValue((Value) last);
                    deleteFile();
                } else {
                    restoreDefaultValue();
                    if (last != null) {
                        BoundCodeProperty.this.setValue(KEY_LAST_SAVED, PString.EMPTY);
                    }
                }
            }
        }

    }

    // copied from ClassBodyWrapper - @TODO API?
    private static final Pattern EXTENDS_STATEMENT_PATTERN = Pattern.compile(
            "\\s*extends\\s+"
            + "("
            + "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"
            + "(?:\\.\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)*"
            + "(?:\\.\\*)?"
            + ");"
    );

    private static final String SHARED_PREFIX = "SHARED.";

    class SharedBaseAction extends AbstractAction implements PropertyChangeListener {

        private final String primaryBaseClass;
        private final List<String> primaryBaseImports;

        private boolean hasSharedBase;

        private SharedBaseAction(ArgumentInfo info) {
            super(Bundle.CreateSharedBaseLabel());
            primaryBaseClass = Optional.ofNullable(info.properties().get("base-class"))
                    .map(Value::toString)
                    .orElse("java.lang.Object");
            List<String> baseImports = Optional.ofNullable(info.properties().get("base-imports"))
                    .flatMap(PArray::from)
                    .orElse(PArray.EMPTY)
                    .stream()
                    .map(Value::toString)
                    .collect(Collectors.toList());
            this.primaryBaseImports = List.copyOf(baseImports);
            hasSharedBase = false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (hasSharedBase) {
                openSharedBase();
            } else {
                createSharedBase();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            checkForBase();
        }

        private void checkForBase() {
            String code = BoundCodeProperty.this.getCode();
            hasSharedBase = !code.isBlank() && code.lines().allMatch(line
                    -> line.isBlank()
                    || (EXTENDS_STATEMENT_PATTERN.matcher(line).lookingAt()
                    && line.contains(SHARED_PREFIX))
            );
            putValue(Action.NAME,
                    hasSharedBase ? Bundle.EditSharedBaseLabel()
                            : Bundle.CreateSharedBaseLabel());
        }

        private void createSharedBase() {
            try {
                BoundSharedCodeProperty shared = findSharedCodeProperty();
                PMap sharedCode = PMap.from(shared.getValue()).orElseThrow();
                NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine(
                        Bundle.SharedBaseClassLabel(),
                        Bundle.CreateSharedBaseLabel()
                );
                String defaultBaseName = calculateBaseName();
                if (!sharedCode.keys().contains("SHARED." + defaultBaseName)) {
                    input.setInputText(calculateBaseName());
                }
                if (DialogDisplayer.getDefault().notify(input) == NotifyDescriptor.OK_OPTION) {
                    String baseName = input.getInputText().strip();
                    String baseSource = ClassBodyWrapper.create()
                            .className(SHARED_PREFIX + baseName)
                            .extendsType(primaryBaseClass)
                            .defaultImports(primaryBaseImports)
                            .wrap(constructFileContent());
                    PMap updatedSharedCode = PMap.merge(sharedCode,
                            PMap.of("SHARED." + baseName, baseSource),
                            PMap.REPLACE);
                    shared.setValue(updatedSharedCode,
                            Callback.create(result -> {
                                if (result.isError()) {
                                    notifyError(baseName);
                                } else {
                                    deleteFile();
                                    BoundCodeProperty.this.setValue(PString.of(
                                            "extends SHARED." + baseName + ";"
                                    ), Callback.create(result2 -> {
                                        if (result2.isError()) {
                                            notifyError(baseName);
                                        } else {
                                            shared.openFile(SHARED_PREFIX + baseName);
                                        }
                                    }));

                                }
                            }));
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private boolean openSharedBase() {
            if (hasSharedBase) {
                BoundSharedCodeProperty shared = findSharedCodeProperty();
                String code = BoundCodeProperty.this.getCode();
                String sharedBaseType = code.lines()
                        .map(line -> {
                            Matcher m = EXTENDS_STATEMENT_PATTERN.matcher(line);
                            if (m.lookingAt()) {
                                String superClass = m.group(1);
                                if (superClass.startsWith(SHARED_PREFIX)) {
                                    return superClass;
                                }
                            }
                            return "";
                        })
                        .filter(s -> !s.isBlank())
                        .findFirst().orElse(null);
                if (sharedBaseType != null) {
                    shared.openFile(sharedBaseType);
                    return true;
                }
            }
            return false;
        }

        private String calculateBaseName() {
            String componentID = getAddress().component().componentID();
            StringBuilder sb = new StringBuilder();
            boolean capitalize = true;
            for (int i = 0; i < componentID.length(); i++) {
                char c = componentID.charAt(i);
                if ((i > 0 && Character.isJavaIdentifierPart(c))
                        || Character.isJavaIdentifierStart(c)) {
                    sb.append(capitalize ? Character.toUpperCase(c) : c);
                    capitalize = false;
                } else {
                    capitalize = true;
                }
            }
            return sb.toString();
        }

        private void notifyError(String baseName) {
            DialogDisplayer.getDefault().notifyLater(
                    new NotifyDescriptor.Message(
                            Bundle.SharedBaseClassError(baseName),
                            NotifyDescriptor.ERROR_MESSAGE));
        }

    }

}
