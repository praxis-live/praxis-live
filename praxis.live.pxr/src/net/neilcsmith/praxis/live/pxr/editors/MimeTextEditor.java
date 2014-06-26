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
package net.neilcsmith.praxis.live.pxr.editors;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.info.ArgumentInfo;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.properties.EditorSupport;
import net.neilcsmith.praxis.live.properties.PraxisProperty;
import org.netbeans.api.actions.Openable;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.InplaceEditor;
import static org.openide.explorer.propertysheet.InplaceEditor.COMMAND_FAILURE;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith
 */
@SuppressWarnings("deprecation")
class MimeTextEditor extends EditorSupport
        implements ExPropertyEditor, InplaceEditor.Factory {

    private final PraxisProperty property;
    private final String mime;
    private final String template;
    private final EditInitializer editInit;
    private final FileListener fileListener;
    private final String fileName;

    private PropertyEnv env;
    private FileObject file;

    MimeTextEditor(PraxisProperty property, ArgumentInfo info, String mimetype) {
        this.property = property;
        this.mime = mimetype;
        this.template = info.getProperties().getString(ArgumentInfo.KEY_TEMPLATE, "");
        this.editInit = new EditInitializer();
        this.fileListener = new FileListener();
        Object obj = property.getValue("address");
        if (obj instanceof ControlAddress) {
            ControlAddress ad = (ControlAddress) obj;
            fileName = ad.getComponentAddress().getID() + "_" + ad.getID();
        } else {
            fileName = property.getName();
        }
//        property.setValue("canEditAsText", Boolean.FALSE);
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
        env.registerInplaceEditorFactory(this);
    }

    @Override
    public InplaceEditor getInplaceEditor() {
        return editInit;
    }

    public String getDisplayName() {
        return getClass().getSimpleName();
    }

    @Override
    public void reset() {
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
        FileObject f = fs.getRoot().createData(fileName, findFileExtension());
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(f.getOutputStream());
            writer.append(constructFileContent());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        f.addFileChangeListener(fileListener);
        return f;
    }

    private String constructFileContent() {
        String s = property.getValue().toString();
        if (s.trim().isEmpty()) {
            s = template;
        }
        return s;
    }

    private String findFileExtension() {
        if ("text/x-praxis-java".equals(mime)) {
            return "pxj";
        }
        if ("text/x-praxis-script".equals(mime)) {
            return "pxs";
        }
        if ("text/x-glsl-frag".equals(mime)) {
            return "frag";
        }
        if ("text/x-glsl-vert".equals(mime)) {
            return "vert";
        }
        return "";
    }

    private void updateProperty() {
        if (file != null && property.isActiveEditor(this)) {
            try {
                property.setValue(PString.valueOf(file.asText()));
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    class FileListener extends FileChangeAdapter {

        @Override
        public void fileChanged(final FileEvent fe) {
            if (EventQueue.isDispatchThread()) {
                if (fe.getFile() == file) {
                    updateProperty();
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

    class EditInitializer extends JComponent implements InplaceEditor {

        private JComponent cmp;
        private Object value;
        private PropertyEditor pe;
        private PropertyEnv env;
        private PropertyModel pm;
        private List<ActionListener> listeners;

        private EditInitializer() {
            this.listeners = new ArrayList<ActionListener>();
            addMouseListener(new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    initializeEditing();
                }

            });
            addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    initializeEditing();
                }

            });
        }

        @Override
        public void connect(PropertyEditor pe, PropertyEnv env) {
            this.pe = pe;
            this.env = env;
            value = pe.getValue();
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        private void initializeEditing() {

            ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, COMMAND_FAILURE);
            for (ActionListener l : listeners.toArray(new ActionListener[0])) {
                l.actionPerformed(ev);
            }
            openEditor();
        }

        @Override
        public void clear() {
            this.pe = null;
            this.env = null;
            this.value = null;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public void setValue(Object o) {
        }

        @Override
        public boolean supportsTextEntry() {
            return false;
        }

        @Override
        public void reset() {
        }

        @Override
        public void addActionListener(ActionListener al) {
            listeners.add(al);
        }

        @Override
        public void removeActionListener(ActionListener al) {
            listeners.remove(al);
        }

        @Override
        public KeyStroke[] getKeyStrokes() {
            return new KeyStroke[0];
        }

        @Override
        public PropertyEditor getPropertyEditor() {
            return pe;
        }

        @Override
        public PropertyModel getPropertyModel() {
            return pm;
        }

        @Override
        public void setPropertyModel(PropertyModel pm) {
            this.pm = pm;
        }

        @Override
        public boolean isKnownComponent(Component c) {
            return false;
        }

    }

}
