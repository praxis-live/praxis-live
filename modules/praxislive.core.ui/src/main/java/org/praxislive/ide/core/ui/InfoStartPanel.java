/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2018 Neil C Smith.
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

import java.io.IOException;
import java.net.URL;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Style;
import javax.swing.text.html.HTMLDocument;
import org.openide.util.Exceptions;
import org.praxislive.ide.core.Core;
import org.praxislive.ide.core.ui.spi.StartPagePanelProvider;

/**
 *
 */
public class InfoStartPanel extends javax.swing.JPanel {

    private final static String KEY_NEWS_LINK = "start-page";

    /**
     * Creates new form NewsStartPanel
     */
    public InfoStartPanel() {
        initComponents();
        initHTMLPane();
    }

    private void initHTMLPane() {
        URL defPage = getClass().getResource("resources/defaultNews.html");
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
    
    
    private boolean refresh() {
        String newsLink = Core.getInstance().getPreferences().get(KEY_NEWS_LINK, null);
        if (newsLink != null) {
            try {
                htmlPane.setPage(newsLink);
                return true;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return false;
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scrollPane = new javax.swing.JScrollPane();
        htmlPane = new javax.swing.JEditorPane();

        scrollPane.setBorder(null);

        htmlPane.setEditable(false);
        htmlPane.setBackground(new java.awt.Color(0, 0, 0));
        htmlPane.setBorder(null);
        htmlPane.setContentType("text/html"); // NOI18N
        htmlPane.setForeground(new java.awt.Color(204, 204, 204));
        scrollPane.setViewportView(htmlPane);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 146, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 134, Short.MAX_VALUE)
                    .addContainerGap()))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane htmlPane;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

    static class Provider implements StartPagePanelProvider {

        private final InfoStartPanel panel = new InfoStartPanel();

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public JPanel getPanel() {
            return panel;
        }

        @Override
        public boolean refresh() {
            return panel.refresh();
        }
        
        

    }

}
