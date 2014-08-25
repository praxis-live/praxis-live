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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Style;
import javax.swing.text.html.HTMLDocument;
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
    private final static String UPDATE_CHECKING = "Checking for updates ...";
    private final static String UPDATE_OK = "Praxis LIVE (testing) is up to date.";
    private final static String UPDATE_ERROR = "Unable to check for updates.";
    private final static String UPDATE_AVAILABLE = "New version of Praxis LIVE (testing) available.";
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

        initHTMLPane();

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

    private void initHTMLPane() {
        URL defPage = getClass().getResource("resources/defaultStart.html");
        if (defPage != null) {
            try {
                htmlPane.setPage(defPage);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        htmlPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    Utils.openExternalLink(e.getURL());
                } else {
                    String styleName;
                    if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                        styleName = "a:hover";
                    } else {
                        styleName = "a";
                    }
                    HTMLDocument doc = (HTMLDocument) htmlPane.getDocument();
                    Style newStyle = doc.getStyleSheet().getStyle(styleName);
                    if (newStyle == null) {
                        newStyle = doc.getStyleSheet().getRule("a");
                    }
                    int start = e.getSourceElement().getStartOffset();
                    int end = e.getSourceElement().getEndOffset();
                    doc.setCharacterAttributes(start, end - start, newStyle, false);
                }

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
        updatePanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        htmlPane = new javax.swing.JEditorPane();

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

        updatePanel.setBackground(new java.awt.Color(0, 0, 0));
        updatePanel.setForeground(new java.awt.Color(204, 204, 204));
        updatePanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jScrollPane1.setBackground(new java.awt.Color(0, 0, 0));

        htmlPane.setEditable(false);
        htmlPane.setBackground(new java.awt.Color(0, 0, 0));
        htmlPane.setContentType("text/html"); // NOI18N
        htmlPane.setText(org.openide.util.NbBundle.getMessage(StartTopComponent.class, "StartTopComponent.htmlPane.text")); // NOI18N
        htmlPane.setFocusable(false);
        jScrollPane1.setViewportView(htmlPane);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(updatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
                        .addGap(234, 234, 234))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(logo)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(logo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addGap(64, 64, 64)
                .addComponent(updatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    private void logoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoActionPerformed
        Utils.openExternalLink(WEBSITE_LINK);
    }//GEN-LAST:event_logoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane htmlPane;
    private javax.swing.JButton jButton2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton logo;
    private javax.swing.JPanel updatePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        updatePanel.add(updateLabel);
        if (Boolean.getBoolean("praxislive.start.suppresscheck")) {
            updateLabel.setText(UPDATE_OK);
        } else {
            updateLabel.setText(UPDATE_CHECKING);
            updatePanel.add(updateProgress);
            RP.execute(new UpdateCheck());
        }
        updatePanel.revalidate();
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
            URI startPage = null;
            if (!error) {
                XPath xpath = XPathFactory.newInstance().newXPath();
                InputSource source = new InputSource("http://www.praxislive.org/release-check/testing/" + version);
                try {
                    String result = xpath.evaluate("//build-version", source);
                    current = version.equals(result);
//                    int curV = Integer.parseInt(version);
//                    int resV = Integer.parseInt(result);
//                    if (curV < resV) {
//                        current = false;
//                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                    error = true;
                }
                try {
                    String result = xpath.evaluate("//start-page", source);
                    if (result != null && !result.isEmpty()) {
                        startPage = new URI(result);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            final boolean err = error;
            final boolean cur = current;
            final URI start = startPage;
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (start != null) {
                        try {
                            htmlPane.setPage(start.toURL());
                        } catch (MalformedURLException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
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
