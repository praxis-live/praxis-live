/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith.
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
package net.neilcsmith.praxis.live.editor.saveflash;

//import java.awt.Color;
import java.util.prefs.Preferences;
import javax.swing.Timer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
//import javax.swing.text.StyleConstants;
import javax.swing.text.Utilities;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.editor.settings.FontColorNames;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.spi.editor.highlighting.HighlightsChangeListener;
import org.netbeans.spi.editor.highlighting.HighlightsContainer;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.HighlightsSequence;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.util.Exceptions;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class FlashOnSaveHighlight implements HighlightsContainer {

    private final static int DELAY = 500;
    final static String ENABLED_KEY = "flash-enabled";
//    private final static Color DEFAULT_COLOR = new Color(216, 54, 53);
    
    static boolean enabled = MimeLookup.getLookup(MimePath.EMPTY).lookup(Preferences.class)
                        .getBoolean(FlashOnSaveHighlight.ENABLED_KEY, false);
    
    
    private final OffsetsBag bag;
    private final AttributeSet attributes;
    private final JTextComponent component;
    private final Timer timer;

    private FlashOnSaveHighlight(JTextComponent component) {
        String mimeType = getMimeType(component);
        MimePath mimePath = mimeType == null ? MimePath.EMPTY : MimePath.parse(mimeType);
        attributes = getAttributes(mimePath);

        this.component = component;

        this.bag = new OffsetsBag(component.getDocument());

        timer = new Timer(DELAY, e -> bag.clear());
        timer.setRepeats(false);
    }

    @Override
    public HighlightsSequence getHighlights(int startOffset, int endOffset) {
        return bag.getHighlights(startOffset, endOffset);
    }

    @Override
    public void addHighlightsChangeListener(HighlightsChangeListener listener) {
        bag.addHighlightsChangeListener(listener);
    }

    @Override
    public void removeHighlightsChangeListener(HighlightsChangeListener listener) {
        bag.removeHighlightsChangeListener(listener);
    }

    void highlight(Element root) {
        if (!enabled) {
            return;
        }
        try {
            int count = root.getElementCount();
            if (count == 0) {
                bag.addHighlight(root.getStartOffset(), root.getEndOffset(), attributes);
            } else {
                for (int i = 0; i < root.getElementCount(); i++) {
                    Element e = root.getElement(i);
                    int start = Utilities.getRowStart(component, e.getStartOffset());
                    int end = Utilities.getRowEnd(component, e.getEndOffset());
                    if (start < 0 || end < 0) continue;
                    String text = component.getDocument().getText(start, end - start);
                    bag.addHighlight(
                            start,
                            end + (text.endsWith("\n") ? 0 : 1),
                            attributes);
                }
            }

            timer.restart();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static AttributeSet getAttributes(MimePath mimePath) {
        FontColorSettings fcs = MimeLookup.getLookup(mimePath).lookup(FontColorSettings.class);
        AttributeSet attribs = fcs.getFontColors(FontColorNames.INC_SEARCH_COLORING);
//        AttributeSet attribs = AttributesUtilities.createImmutable(
//                    StyleConstants.Background, DEFAULT_COLOR);
        if (attribs != null) {
            return AttributesUtilities.createImmutable(attribs,
                    AttributesUtilities.createImmutable(
                            ATTR_EXTENDS_EMPTY_LINE, Boolean.TRUE,
                            ATTR_EXTENDS_EOL, Boolean.TRUE));
        } else {
            return SimpleAttributeSet.EMPTY;
        }
    }

    private static String getMimeType(JTextComponent component) {
        Document doc = component.getDocument();
        String mime = (String) doc.getProperty("mimeType");
        if (mime == null) {
            EditorKit kit = component.getUI().getEditorKit(component);
            if (kit != null) {
                mime = kit.getContentType();
            }
        }
        return mime;
    }

    @MimeRegistration(mimeType = "", service = HighlightsLayerFactory.class)
    public static class Factory implements HighlightsLayerFactory {

        @Override
        public HighlightsLayer[] createLayers(Context context) {

            FlashOnSaveHighlight hl = new FlashOnSaveHighlight(context.getComponent());
            context.getDocument().putProperty(FlashOnSaveHighlight.class, hl);

            return new HighlightsLayer[]{
                HighlightsLayer.create(FlashOnSaveHighlight.class.getName(),
                ZOrder.TOP_RACK.forPosition(10000),
                false,
                hl)
            };
        }

    }

}
