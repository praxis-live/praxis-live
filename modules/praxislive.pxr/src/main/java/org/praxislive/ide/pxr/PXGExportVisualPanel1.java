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

import java.io.File;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.NbBundle;

@NbBundle.Messages({
    "PXGExportVisualPanel1.name=File",
    "PXGExportVisualPanel1.fileSelect=Select"
})
final class PXGExportVisualPanel1 extends JPanel implements DocumentListener {

    private final PXGExportWizardPanel1 wizardPanel;
    private final FileChooserBuilder fileChooser;
    private File location;

    PXGExportVisualPanel1(PXGExportWizardPanel1 wizardPanel) {
        this.wizardPanel = wizardPanel;
        initComponents();
        PXGExportWizard wizard = wizardPanel.getWizard();
        location = wizard.getDefaultLocation();
        fileChooser = new FileChooserBuilder(PXGExportVisualPanel1.class)
                .setDirectoriesOnly(true)
                .setApproveText(Bundle.PXGExportVisualPanel1_fileSelect())
                .setDefaultWorkingDirectory(location)
                .forceUseOfDefaultWorkingDirectory(true);
        locationField.setText(location.toString());
        fileField.setText(location.toString());
        nameField.setText(wizard.getSuggestedFileName());
        paletteCategoryField.setText(wizard.getSuggestedPaletteCategory());
        nameField.getDocument().addDocumentListener(this);
        paletteCategoryField.getDocument().addDocumentListener(this);
        if (wizard.hasLibraries()) {
            librariesCheckbox.setEnabled(true);
            librariesCheckbox.setSelected(wizard.mightUseLibraries());
        } else {
            librariesCheckbox.setEnabled(false);
            librariesCheckbox.setSelected(false);
        }
        if (wizard.hasSharedCode()) {
            sharedCodeCheckbox.setEnabled(true);
            sharedCodeCheckbox.setSelected(wizard.mightUseSharedCode());
        } else {
            sharedCodeCheckbox.setEnabled(false);
            sharedCodeCheckbox.setSelected(false);
        }

    }

    @Override
    public String getName() {
        return Bundle.PXGExportVisualPanel1_name();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        update();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nameLabel = new javax.swing.JLabel();
        locationLabel = new javax.swing.JLabel();
        fileLabel = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        locationField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        fileField = new javax.swing.JTextField();
        paletteCategoryLabel = new javax.swing.JLabel();
        paletteCategoryField = new javax.swing.JTextField();
        paletteCheckbox = new javax.swing.JCheckBox();
        librariesCheckbox = new javax.swing.JCheckBox();
        sharedCodeCheckbox = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(nameLabel, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.nameLabel.text_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(locationLabel, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.locationLabel.text_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.fileLabel.text_1")); // NOI18N

        nameField.setText(org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.nameField.text_1")); // NOI18N

        locationField.setEditable(false);
        locationField.setText(org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.locationField.text_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.browseButton.text_1")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        fileField.setEditable(false);
        fileField.setText(org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.fileField.text_1")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(paletteCategoryLabel, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.paletteCategoryLabel.text_1")); // NOI18N

        paletteCategoryField.setText(org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.paletteCategoryField.text_1")); // NOI18N

        paletteCheckbox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(paletteCheckbox, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.paletteCheckbox.text_1")); // NOI18N
        paletteCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paletteCheckboxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(librariesCheckbox, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.librariesCheckbox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(sharedCodeCheckbox, org.openide.util.NbBundle.getMessage(PXGExportVisualPanel1.class, "PXGExportVisualPanel1.sharedCodeCheckbox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(fileLabel)
                            .addComponent(locationLabel)
                            .addComponent(nameLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(nameField)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(locationField, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(browseButton))
                            .addComponent(fileField)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(paletteCategoryLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(paletteCategoryField))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(paletteCheckbox)
                            .addComponent(librariesCheckbox)
                            .addComponent(sharedCodeCheckbox))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameLabel)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(locationLabel)
                    .addComponent(locationField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(browseButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fileLabel)
                    .addComponent(fileField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(paletteCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(paletteCategoryLabel)
                    .addComponent(paletteCategoryField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(librariesCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sharedCodeCheckbox)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        File loc = fileChooser.showOpenDialog();
        if (loc != null) {
            location = loc;
            locationField.setText(location.toString());
            update();
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void paletteCheckboxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paletteCheckboxActionPerformed
        paletteCategoryField.setEnabled(paletteCheckbox.isSelected());
        update();
    }//GEN-LAST:event_paletteCheckboxActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JTextField fileField;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JCheckBox librariesCheckbox;
    private javax.swing.JTextField locationField;
    private javax.swing.JLabel locationLabel;
    private javax.swing.JTextField nameField;
    private javax.swing.JLabel nameLabel;
    private javax.swing.JTextField paletteCategoryField;
    private javax.swing.JLabel paletteCategoryLabel;
    private javax.swing.JCheckBox paletteCheckbox;
    private javax.swing.JCheckBox sharedCodeCheckbox;
    // End of variables declaration//GEN-END:variables

    @Override
    public void insertUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        update();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        update();
    }

    File getFileLocation() {
        return location;
    }

    String getFileName() {
        return nameField.getText();
    }

    String getPaletteCategory() {
        return paletteCheckbox.isSelected() ? paletteCategoryField.getText() : "";
    }

    boolean includeLibraries() {
        return librariesCheckbox.isSelected();
    }

    boolean includeSharedCode() {
        return sharedCodeCheckbox.isSelected();
    }

    private void update() {
        String name = nameField.getText();
        if (!name.isEmpty() && !name.endsWith(".pxg")) {
            name = name + ".pxg";
        }
        fileField.setText(locationField.getText() + File.separator + name);
        wizardPanel.validate();
    }
}
