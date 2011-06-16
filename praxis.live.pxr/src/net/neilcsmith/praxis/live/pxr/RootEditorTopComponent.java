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

import java.awt.BorderLayout;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.neilcsmith.praxis.live.pxr.PXRParser.*;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import net.neilcsmith.praxis.live.pxr.api.RootRegistry;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.CloneableTopComponent;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class RootEditorTopComponent extends CloneableTopComponent {

    
    private PXRDataObject dob;
    private RootEditor editor;
    private JComponent editorPanel;
    private EditorLookup lookup;
    private RootProxy root;

    public RootEditorTopComponent(PXRDataObject dob) {
        this.setDisplayName(dob.getName());
        this.setIcon(dob.getNodeDelegate().getIcon(BeanInfo.ICON_COLOR_16x16));
        this.dob = dob;
        lookup = new EditorLookup(Lookups.singleton(dob), dob.getLookup());
        associateLookup(lookup);
        setLayout(new BorderLayout());
        root = RootRegistry.getDefault().findRootForFile(dob.getPrimaryFile());
        installEditor(root);
        RootRegistry.getDefault().addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                checkRoot();
            }
        });
        checkRoot();
    }

    @Override
    protected void componentShowing() {
        editor.componentShowing();
    }

    @Override
    protected void componentActivated() {
        editor.componentActivated();
    }

    @Override
    protected void componentDeactivated() {
        editor.componentDeactivated();
    }

    @Override
    protected void componentHidden() {
        editor.componentHidden();
    }




    private void checkRoot() {
        RootProxy root = RootRegistry.getDefault().findRootForFile(dob.getPrimaryFile());
        if (root == this.root) {
            return;
        }
        uninstallEditor();
        this.root = root;
        installEditor(root);
    }

    private void installEditor(RootProxy root) {
        if (root == null) {
            editor = new BlankEditor();
        } else {
//            editor = new DebugRootEditor(root);
            editor = findEditor(root);
        }
        editorPanel = editor.getEditorComponent();
        add(editorPanel);
        lookup.setEditor(editor);
    }

    private void uninstallEditor() {
        remove(editorPanel);
        editorPanel = null;
        editor.dispose();
        editor = null;
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    private RootEditor findEditor(RootProxy root) {
        for (RootEditor.Provider provider : Lookup.getDefault().lookupAll(RootEditor.Provider.class)) {
            RootEditor ed = provider.createEditor(root);
            if (ed != null) {
                return ed;
            }
        }
        return new DebugRootEditor(root);
    }



    private class BlankEditor extends RootEditor {

        @Override
        public Lookup getLookup() {
            return Lookups.singleton(dob.getNodeDelegate());
        }


        @Override
        public JComponent getEditorComponent() {
            return new JLabel("<Build " + dob.getName() + " to edit>", JLabel.CENTER);
        }
        
    }


    private class EditorLookup extends ProxyLookup {

        private Lookup current;

        private EditorLookup(Lookup ... lookups) {
            super(lookups);
        }

        private void setEditor(RootEditor editor) {
            Lookup[] lkps = getLookups();
            List<Lookup> lst = new ArrayList<Lookup>(Arrays.asList(lkps));
            if (current != null) {
                lst.remove(current);
            }
            current = editor.getLookup();
            if (current != null) {
                lst.add(current);
            }
            setLookups(lst.toArray(lkps));
        }

    }







    @Override
    protected CloneableTopComponent createClonedObject() {
        return new RootEditorTopComponent(dob);
    }

    
}
