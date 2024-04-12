/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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
package org.praxislive.ide.core.ui;

final class GeneralPanel extends javax.swing.JPanel {

    private final GeneralOptionsPanelController controller;

    GeneralPanel(GeneralOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        // TODO listen to changes in form fields and call controller.changed()
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        startPageCheckbox = new javax.swing.JCheckBox();
        updatesCheckbox = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(startPageCheckbox, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.startPageCheckbox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(updatesCheckbox, org.openide.util.NbBundle.getMessage(GeneralPanel.class, "GeneralPanel.updatesCheckbox.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(startPageCheckbox)
                    .addComponent(updatesCheckbox))
                .addContainerGap(335, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(startPageCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updatesCheckbox)
                .addContainerGap(284, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    void load() {
        startPageCheckbox.setSelected(Utils.isShowStart());
        updatesCheckbox.setSelected(Utils.isCheckForUpdates());
    }

    void store() {
        Utils.setShowStart(startPageCheckbox.isSelected());
        Utils.setCheckForUpdates(updatesCheckbox.isSelected());
    }

    boolean valid() {
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox startPageCheckbox;
    private javax.swing.JCheckBox updatesCheckbox;
    // End of variables declaration//GEN-END:variables
}
