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
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package net.neilcsmith.praxis.live.core.ui;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Style;
import javax.swing.text.html.HTMLDocument;
import net.neilcsmith.praxis.live.core.Core;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.OnShowing;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.windows.WindowSystemEvent;
import org.openide.windows.WindowSystemListener;

/**
 * Welcome Page
 */
@ConvertAsProperties(
        dtd = "-//net.neilcsmith.praxis.live.core.ui//Start//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "StartTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "net.neilcsmith.praxis.live.core.ui.StartTopComponent")
//@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_StartAction",
        preferredID = "StartTopComponent")
@Messages({
    "CTL_StartAction=Start",
    "CTL_StartTopComponent=Praxis LIVE",
    "HINT_StartTopComponent=Welcome to Praxis LIVE",
    "LBL_NewVersion=New version available",
    "LBL_NewVersionInfo=A new version of Praxis LIVE is available to download",
    "LBL_Download=Download",
})
public final class StartTopComponent extends TopComponent {

    private final JLabel updateLabel;
    private final JButton downloadButton;

    public StartTopComponent() {
        initComponents();
        setName(Bundle.CTL_StartTopComponent());
        setToolTipText(Bundle.HINT_StartTopComponent());
        putClientProperty("activateAtStartup", Boolean.TRUE);
        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, true);
        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, true);

        initHTMLPane();

        updateLabel = new JLabel(Bundle.LBL_NewVersion());
        downloadButton = new JButton(Bundle.LBL_Download());
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openExternalLink(Utils.DOWNLOAD_LINK);
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

        mainPanel = new javax.swing.JPanel();
        logo = new javax.swing.JButton();
        updatePanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        htmlPane = new javax.swing.JEditorPane();

        setBackground(java.awt.Color.black);
        setOpaque(true);

        mainPanel.setBackground(new java.awt.Color(0, 0, 0));
        mainPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        logo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/neilcsmith/praxis/live/core/ui/resources/praxis live.png"))); // NOI18N
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

        scrollPane.setBackground(new java.awt.Color(0, 0, 0));

        htmlPane.setEditable(false);
        htmlPane.setBackground(new java.awt.Color(0, 0, 0));
        htmlPane.setContentType("text/html"); // NOI18N
        htmlPane.setFocusable(false);
        scrollPane.setViewportView(htmlPane);

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPane)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(updatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
                        .addGap(234, 234, 234))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(logo)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(logo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 390, Short.MAX_VALUE)
                .addGap(64, 64, 64)
                .addComponent(updatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void logoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logoActionPerformed
        Utils.openExternalLink(Utils.WEBSITE_LINK);
    }//GEN-LAST:event_logoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane htmlPane;
    private javax.swing.JButton logo;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel updatePanel;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        configureUpdatePanel();
        configureStartPage();
    }

    void configureUpdatePanel() {
        updatePanel.removeAll();
        if (isUpdateAvailable()) {
            updatePanel.add(updateLabel);
            updatePanel.add(downloadButton);
            updatePanel.revalidate();
        }
    }

    void configureStartPage() {
        String startPage = Core.getInstance().getPreferences().get("start-page", null);
        if (startPage != null) {
            try {
                htmlPane.setPage(startPage);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
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

    private static void checkInfo() {
        StartTopComponent start = StartTopComponent.find();
        if (start != null && start.isVisible()) {
            start.configureUpdatePanel();
            start.configureStartPage();
        }
        if (isUpdateAvailable()) {
            NotificationDisplayer.getDefault().notify(
                    Bundle.LBL_NewVersion(),
                    ImageUtilities.loadImageIcon(
                            "net/neilcsmith/praxis/live/core/ui/resources/info_icon.png", true),
                    Bundle.LBL_NewVersionInfo(),
                    null);
        }
    }

    private static boolean isUpdateAvailable() {
        Core core = Core.getInstance();
        String current = core.getBuildVersion();
        String latest = core.getLatestBuild();
        try {
            int cur = Integer.parseInt(current);
            int lat = Integer.parseInt(latest);
            return lat > cur;
        } catch (Exception ex) {
            return !Objects.equals(current, latest);
        }
    }

    static StartTopComponent find() {
        TopComponent tc = WindowManager.getDefault().findTopComponent("StartTopComponent");
        if (tc instanceof StartTopComponent) {
            return (StartTopComponent) tc;
        }
        assert false;
        return null;
    }

    @OnShowing
    public static class Installer implements Runnable {

        @Override
        public void run() {
            WindowManager.getDefault().addWindowSystemListener(new WindowSystemListener() {
                @Override
                public void beforeLoad(WindowSystemEvent event) {
                }

                @Override
                public void afterLoad(WindowSystemEvent event) {
                }

                @Override
                public void beforeSave(WindowSystemEvent event) {
                    boolean show = Utils.isShowStart();
                    TopComponent start = StartTopComponent.find();
                    if (start != null) {
                        if (show) {
                            start.open();
                            start.requestActive();
                        } else {
                            start.close();
                        }
                    }

                }

                @Override
                public void afterSave(WindowSystemEvent event) {
                }
            });

            Core.getInstance().getPreferences().addPreferenceChangeListener(
                    new PreferenceChangeListener() {

                        @Override
                        public void preferenceChange(PreferenceChangeEvent evt) {
                            update();

                        }
                    });
            
            update();

        }

        private void update() {
            EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    StartTopComponent.checkInfo();
                }

            });
        }

    }

}
