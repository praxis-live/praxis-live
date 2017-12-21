/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Neil C Smith.
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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.project.api.PraxisProject;
import org.netbeans.api.actions.Openable;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

class BoundCodeProperty extends BoundArgumentProperty {

    private static final Map<String, String> mimeToExt = new HashMap<>();

    static {
        // include common
        mimeToExt.put("text/x-praxis-java", "pxj");
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

    private FileObject file;

    BoundCodeProperty(PraxisProject project, ControlAddress address, ControlInfo info, String mimeType) {
        super(address, info);
        this.project = project;
        this.mimeType = mimeType;
        this.template = info.getOutputsInfo()[0].getProperties().getString(ArgumentInfo.KEY_TEMPLATE, "");
        this.fileListener = new FileListener();
        fileName = safeFileName(address);
        String id = address.getID();
        editAction = new EditAction(id);
        resetAction = new ResetAction(id);
        setHidden(true);
    }
    
    private String safeFileName(ControlAddress address) {
        String name = address.getComponentAddress().getID() + "_" + address.getID();
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
        f.setAttribute("argumentInfo", getInfo().getOutputsInfo()[0]);
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
        String s = getValue().toString();
        if (s.trim().isEmpty()) {
            s = template;
        }
        return s;
    }
    
    private void updateFromFile() {
        if (file != null) {
            try {
                setValue(PString.valueOf(file.asText()));
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
            super("Edit " + id);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            openEditor();
        }
        
    }
    
    class ResetAction extends AbstractAction {
        
        private String id;
        
        private ResetAction(String id) {
            super("Reset " + id);
            this.id = id;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NotifyDescriptor nd = new NotifyDescriptor(
                    "Reset " + id + "? Changes will be lost.",
                    "Reset " + id + "?",
                    NotifyDescriptor.YES_NO_OPTION,
                    NotifyDescriptor.WARNING_MESSAGE, 
                    null,
                    null);
            if (DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.YES_OPTION) {
                restoreDefaultValue();
            }
        }
        
    }
    
}
