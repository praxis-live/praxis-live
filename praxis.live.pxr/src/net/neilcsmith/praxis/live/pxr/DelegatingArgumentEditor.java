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

import net.neilcsmith.praxis.live.pxr.editors.EditorManager;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.pxr.api.PraxisPropertyEditor;
import net.neilcsmith.praxis.live.pxr.editors.SubCommandEditor;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
class DelegatingArgumentEditor extends PropertyEditorSupport
        implements PraxisPropertyEditor, ExPropertyEditor {

    private final static Logger LOG = Logger.getLogger(DelegatingArgumentEditor.class.getName());
    private PraxisPropertyEditor defaultEditor;
    private PraxisPropertyEditor currentEditor;
    private PraxisPropertyEditor[] allEditors;
    private DelegateListener dl;
    private ArgumentProperty property;
    private ControlInfo info;
    private PropertyEnv env;

    DelegatingArgumentEditor(ArgumentProperty property, ControlInfo info) {
        this.property = property;
        this.info = info;
        defaultEditor = EditorManager.getDefaultEditor(property, info); // must never be null
        currentEditor = defaultEditor;
        property.setValue("editor", currentEditor);
        dl = new DelegateListener();
        currentEditor.addPropertyChangeListener(dl);


    }

    @Override
    public String getPraxisInitializationString() {
        return currentEditor.getPraxisInitializationString();
    }

    @Override
    public String getDisplayName() {
        return currentEditor.getDisplayName();
    }

    @Override
    public void setValue(Object value) {
        if (value instanceof SubCommandArgument) {
//            if (currentEditor instanceof SubCommandEditor) {
            try {
                LOG.finest("Delegate setting from command line");
//                    ((SubCommandEditor) currentEditor).setFromCommand(
//                            ((SubCommandArgument) value).getCommandLine());
                setFromCommand(((SubCommandArgument) value).getCommandLine());
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Editor threw exception in command line", ex);
                throw new IllegalArgumentException(ex);
            }
//            }
        } else {
            currentEditor.setValue(value);
        }
    }

    private void setFromCommand(String command) throws Exception {
        Throwable lastEx = null;
        SubCommandEditor cEd;
        if (currentEditor instanceof SubCommandEditor) {
            cEd = (SubCommandEditor) currentEditor;
            if (editorSupportsCommandLine(cEd, command)) {
                try {
                    cEd.setFromCommand(command);
                    return;
                } catch (Exception ex) {
                    lastEx = ex;
                }
            }
        }
        for (PraxisPropertyEditor ed : getAllEditors()) {
            if (ed == currentEditor) {
                continue;
            }
            if (ed instanceof SubCommandEditor) {
                cEd = (SubCommandEditor) ed;
                if (editorSupportsCommandLine(cEd, command)) {
                    try {
                        cEd.setFromCommand(command);
                        setCurrentEditor(ed);
                        return;
                    } catch (Exception ex) {
                        lastEx = ex;
                    }
                }
            }
        }
        if (lastEx == null) {
            throw new IllegalArgumentException("Couldn't find editor matching command");
        } else {
            throw new IllegalArgumentException(lastEx);
        }

    }

    private boolean editorSupportsCommandLine(SubCommandEditor editor, String line) {
        for (String cmd : editor.getSupportedCommands()) {
            if (line.startsWith(cmd + " ")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getValue() {
        return currentEditor.getValue();
    }

    @Override
    public boolean isPaintable() {

        return currentEditor.isPaintable();

    }

    @Override
    public void paintValue(Graphics gfx, Rectangle box) {

        currentEditor.paintValue(gfx, box);

    }

    @Override
    public String getJavaInitializationString() {
        return null;
    }

    @Override
    public String getAsText() {
        return currentEditor.getAsText();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        currentEditor.setAsText(text);

    }

    @Override
    public String[] getTags() {
        return currentEditor.getTags();
    }

    @Override
    public Component getCustomEditor() {
        if ( (allEditors != null && allEditors.length > 1) ||
                EditorManager.hasAdditionalEditors(property, info)) {
            return new DelegatingArgumentCustomEditor(this, env);
        } else {
            return currentEditor.getCustomEditor();
        }
        
        
    }

    @Override
    public boolean supportsCustomEditor() {
        if (allEditors == null) {
            return currentEditor.supportsCustomEditor()
                    || EditorManager.hasAdditionalEditors(property, info);
        } else {
            return currentEditor.supportsCustomEditor()
                    || allEditors.length > 1;
        }

    }

    void setCurrentEditor(PraxisPropertyEditor editor) {
        if (currentEditor != editor) {
            currentEditor.removePropertyChangeListener(dl);
            currentEditor = editor;
            property.setValue("editor", editor);
            currentEditor.addPropertyChangeListener(dl);
            LOG.fine("Setting current editor to " + editor.getDisplayName());
        }
    }
    
    void restoreDefaultEditor() {
        setCurrentEditor(defaultEditor);
    }

    PraxisPropertyEditor getCurrentEditor() {
        return currentEditor;
    }

    PraxisPropertyEditor[] getAllEditors() {
        if (allEditors == null) {
            initializeAllEditors();
        }
        return allEditors;
    }

    private void initializeAllEditors() {
        if (EditorManager.hasAdditionalEditors(property, info)) {
            PraxisPropertyEditor[] additional = EditorManager.getAdditionalEditors(property, info);
            PraxisPropertyEditor[] all = new PraxisPropertyEditor[additional.length + 1];
            all[0] = defaultEditor;
            System.arraycopy(additional, 0, all, 1, additional.length);
            allEditors = all;
        } else {
            allEditors = new PraxisPropertyEditor[]{defaultEditor};
        }
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        if (currentEditor instanceof ExPropertyEditor) {
            ((ExPropertyEditor) currentEditor).attachEnv(env);
        }
        this.env = env;
    }

    private class DelegateListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            firePropertyChange();
        }
    }
}
