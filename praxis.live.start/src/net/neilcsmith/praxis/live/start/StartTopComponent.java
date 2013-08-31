/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
package net.neilcsmith.praxis.live.start;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.modules.ModuleInfo;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;
import org.xml.sax.InputSource;

/**
 * Welcome Page
 */
@ConvertAsProperties(
    dtd = "-//net.neilcsmith.praxis.live.start//Start//EN",
autostore = false)
@TopComponent.Description(
    preferredID = "StartTopComponent",
//iconBase="SET/PATH/TO/ICON/HERE", 
persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "net.neilcsmith.praxis.live.start.StartTopComponent")
//@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
    displayName = "#CTL_StartAction",
preferredID = "StartTopComponent")
@Messages({
    "CTL_StartAction=Start",
    "CTL_StartTopComponent=Praxis LIVE",
    "HINT_StartTopComponent=Welcome to Praxis LIVE"
})
public final class StartTopComponent extends TopComponent {

    private final static Color LINK_COLOR = new Color(204, 204, 204);
    private final static Color LINK_COLOR_HOVER = LINK_COLOR.brighter().brighter();
    private final static URI WEBSITE_LINK = URI.create("http://www.praxislive.org");
    private final static URI DOWNLOAD_LINK = URI.create("http://www.praxislive.org/download");
//    private final static URI RESOURCES_LINK = URI.create("http://www.praxislive.org/resources");
    private final static URI MANUAL_LINK = URI.create("http://www.praxislive.org/resources/manual");
    private final static URI EXAMPLES_LINK = URI.create("http://www.praxislive.org/resources/examples");
    private final static URI SUBGRAPHS_LINK = URI.create("http://www.praxislive.org/resources/subgraphs");
    private final static URI KB_LINK = URI.create("http://www.praxislive.org/resources/kb");
    private final static URI TWITTER_LINK = URI.create("http://twitter.com/PraxisLIVE");
    private final static String UPDATE_CHECKING = "Checking for updates ...";
    private final static String UPDATE_OK = "Praxis LIVE is up to date.";
    private final static String UPDATE_ERROR = "Unable to check for updates.";
    private final static String UPDATE_AVAILABLE = "New version of Praxis LIVE available.";
    private final static RequestProcessor RP = new RequestProcessor(StartTopComponent.class);
    private final JLabel updateLabel;
    private final JProgressBar updateProgress;
    private final JButton downloadButton;

    public StartTopComponent() {
        initComponents();
        setName(Bundle.CTL_StartTopComponent());
        setToolTipText(Bundle.HINT_StartTopComponent());
        putClientProperty("activateAtStartup", Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, true);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, true);

        initRollover(websiteButton);
        initRollover(manualButton);
        initRollover(examplesButton);
        initRollover(kbButton);
        initRollover(subgraphButton);
        initRollover(twitterButton);

        updateLabel = new JLabel();
        updateProgress = new JProgressBar();
        updateProgress.setIndeterminate(true);
        downloadButton = new JButton("Download");
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openExternalLink(DOWNLOAD_LINK);
            }
        });

    }

    private void initRollover(final JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(LINK_COLOR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(LINK_COLOR);
            }
        });

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        logo = new javax.swing.JButton();
        resourceLabel = new javax.swing.JLabel();
        websiteButton = new javax.swing.JButton();
        manualButton = new javax.swing.JButton();
        examplesButton = new javax.swing.JButton();
        kbButton = new javax.swing.JButton();
        twitterButton = new javax.swing.JButton();
        updatePanel = new javax.swing.JPanel();
        subgraphButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.jButton2.text")); // NOI18N

        setBackground(java.awt.Color.black);
        setOpaque(true);

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/start/resources/praxis live.png"))); // NOI18N
        logo.setBorderPainted(false);
        logo.setContentAreaFilled(false);
        logo.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        logo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logoActionPerformed(evt);
            }
        });

        resourceLabel.setBackground(new java.awt.Color(0, 0, 0));
        resourceLabel.setFont(resourceLabel.getFont().deriveFont(resourceLabel.getFont().getStyle() | java.awt.Font.BOLD, resourceLabel.getFont().getSize()+6));
        resourceLabel.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(resourceLabel, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.resourceLabel.text")); // NOI18N

        websiteButton.setFont(websiteButton.getFont().deriveFont(websiteButton.getFont().getSize()+2f));
        websiteButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(websiteButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.websiteButton.text")); // NOI18N
        websiteButton.setBorderPainted(false);
        websiteButton.setContentAreaFilled(false);
        websiteButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        websiteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                websiteButtonActionPerformed(evt);
            }
        });

        manualButton.setFont(manualButton.getFont().deriveFont(manualButton.getFont().getSize()+2f));
        manualButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(manualButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.manualButton.text")); // NOI18N
        manualButton.setBorderPainted(false);
        manualButton.setContentAreaFilled(false);
        manualButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        manualButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualButtonActionPerformed(evt);
            }
        });

        examplesButton.setFont(examplesButton.getFont().deriveFont(examplesButton.getFont().getSize()+2f));
        examplesButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(examplesButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.examplesButton.text")); // NOI18N
        examplesButton.setBorderPainted(false);
        examplesButton.setContentAreaFilled(false);
        examplesButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        examplesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                examplesButtonActionPerformed(evt);
            }
        });

        kbButton.setFont(kbButton.getFont().deriveFont(kbButton.getFont().getSize()+2f));
        kbButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(kbButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.kbButton.text")); // NOI18N
        kbButton.setBorderPainted(false);
        kbButton.setContentAreaFilled(false);
        kbButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        kbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                kbButtonActionPerformed(evt);
            }
        });

        twitterButton.setFont(twitterButton.getFont().deriveFont(twitterButton.getFont().getSize()+2f));
        twitterButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(twitterButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.twitterButton.text")); // NOI18N
        twitterButton.setBorderPainted(false);
        twitterButton.setContentAreaFilled(false);
        twitterButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        twitterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                twitterButtonActionPerformed(evt);
            }
        });

        updatePanel.setBackground(new java.awt.Color(0, 0, 0));
        updatePanel.setForeground(new java.awt.Color(204, 204, 204));
        updatePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        subgraphButton.setFont(subgraphButton.getFont().deriveFont(subgraphButton.getFont().getSize()+2f));
        subgraphButton.setForeground(new java.awt.Color(204, 204, 204));
        org.openide.awt.Mnemonics.setLocalizedText(subgraphButton, org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.subgraphButton.text")); // NOI18N
        subgraphButton.setBorderPainted(false);
        subgraphButton.setContentAreaFilled(false);
        subgraphButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        subgraphButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subgraphButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(updatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(logo))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(kbButton)
                            .addComponent(twitterButton)
                            .addComponent(examplesButton)
                            .addComponent(manualButton)
                            .addComponent(websiteButton)
                            .addComponent(resourceLabel)
                            .addComponent(subgraphButton))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(resourceLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(websiteButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(manualButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(examplesButton)
                .addGap(5, 5, 5)
                .addComponent(kbButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(subgraphButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(twitterButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 285, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(logo, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(updatePanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void manualButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manualButtonActionPerformed
        Utils.openExternalLink(MANUAL_LINK);
    }//GEN-LAST:event_manualButtonActionPerformed

    private void websiteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_websiteButtonActionPerformed
        Utils.openExternalLink(WEBSITE_LINK);
    }//GEN-LAST:event_websiteButtonActionPerformed

    private void examplesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_examplesButtonActionPerformed
        Utils.openExternalLink(EXAMPLES_LINK);
    }//GEN-LAST:event_examplesButtonActionPerformed

    private void kbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_kbButtonActionPerformed
        Utils.openExternalLink(KB_LINK);
    }//GEN-LAST:event_kbButtonActionPerformed

    private void twitterButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_twitterButtonActionPerformed
        Utils.openExternalLink(TWITTER_LINK);
    }//GEN-LAST:event_twitterButtonActionPerformed

    private void logoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoActionPerformed
        Utils.openExternalLink(WEBSITE_LINK);
    }//GEN-LAST:event_logoActionPerformed

    private void subgraphButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subgraphButtonActionPerformed
        Utils.openExternalLink(SUBGRAPHS_LINK);
    }//GEN-LAST:event_subgraphButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton examplesButton;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JButton kbButton;
    private javax.swing.JButton logo;
    private javax.swing.JButton manualButton;
    private javax.swing.JLabel resourceLabel;
    private javax.swing.JButton subgraphButton;
    private javax.swing.JButton twitterButton;
    private javax.swing.JPanel updatePanel;
    private javax.swing.JButton websiteButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        updateLabel.setText(UPDATE_CHECKING);
        updatePanel.add(updateLabel);
        updatePanel.add(updateProgress);
        updatePanel.revalidate();
        RP.execute(new UpdateCheck());
    }

    @Override
    public void componentClosed() {
        updatePanel.removeAll();
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private class UpdateCheck implements Runnable {

        @Override
        public void run() {
            String version = null;
            for (ModuleInfo info : Lookup.getDefault().lookupAll(ModuleInfo.class)) {
                if (info.owns(this.getClass())) {
                    version = info.getImplementationVersion();
                }
            }
            boolean error = version == null;
            boolean current = true;
            if (!error) {
                XPath xpath = XPathFactory.newInstance().newXPath();
                InputSource source = new InputSource("http://www.praxislive.org/release-check/" + version);
                try {
                    String result = xpath.evaluate("//build-version", source);
                    current = version.equals(result);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    error = true;
                }
            }
            final boolean err = error;
            final boolean cur = current;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (updatePanel.isShowing()) {
                        updatePanel.remove(updateProgress);
                        if (err) {
                            updateLabel.setText(UPDATE_ERROR);
                        } else if (cur) {
                            updateLabel.setText(UPDATE_OK);
                        } else {
                            updateLabel.setText(UPDATE_AVAILABLE);
                            updatePanel.add(downloadButton);
                        }
                        updatePanel.revalidate();
                        StartTopComponent.this.repaint();
                    }
                }
            });
        }
    }
}
