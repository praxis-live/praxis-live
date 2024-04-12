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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.editors;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.filesystems.FileChooserBuilder;

/**
 *
 */
class FileListCustomEditor extends javax.swing.JPanel
        implements VetoableChangeListener {

    private final static Logger LOG = Logger.getLogger(FileListCustomEditor.class.getName());

    private final URI base;
    private File directory;
    private final PropertyEnv env;
    private final FileListEditor editor;

    /** Creates new form ResourceCustomEditor */
    FileListCustomEditor(FileListEditor editor, URI base, File directory, PropertyEnv env) {
        initComponents();
        if (directory != null) {
            dirField.setText(directory.toString());
        }
        this.base = base;
        this.env = env;
        this.editor = editor;
        this.directory = directory;
        env.setState(PropertyEnv.STATE_NEEDS_VALIDATION);
        env.addVetoableChangeListener(this);
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        dirField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();

        dirField.setEditable(false);
        dirField.setText(org.openide.util.NbBundle.getMessage(FileListCustomEditor.class, "FileListCustomEditor.dirField.text")); // NOI18N

        browseButton.setText(org.openide.util.NbBundle.getMessage(FileListCustomEditor.class, "FileListCustomEditor.browseButton.text")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(dirField, javax.swing.GroupLayout.DEFAULT_SIZE, 309, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browseButton)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dirField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addContainerGap(259, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File loc = null;
        if (directory != null) {
            loc = directory.getParentFile();
        }
        if (loc == null && base != null && "file".equals(base.getScheme())) {
            try {
                loc = new File(base);
            } catch (Exception e) {
            }
        }
        FileChooserBuilder dlgBld = new FileChooserBuilder(FileListCustomEditor.class);
        if (loc != null) {
            dlgBld.setDefaultWorkingDirectory(loc).forceUseOfDefaultWorkingDirectory(true);
        }
        dlgBld.setDirectoriesOnly(true);
        dlgBld.setTitle("Choose Directory").setApproveText("OK");
        File file = dlgBld.showOpenDialog();
        if (file != null) {
            directory = file;
            dirField.setText(directory.toString());
        }
    }//GEN-LAST:event_browseButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField dirField;
    // End of variables declaration//GEN-END:variables





    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if (directory != null) {
            try {
                LOG.fine("Setting directory to " + directory);
                editor.setDirectory(directory);
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Exception setting directory", ex);
            }
        }
        
    }
}
