/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith / David von Ah
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
package org.praxislive.ide.pxr.graph;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

class JSuggestField extends JTextField {

    /**
     * unique ID for serialization
     */
    private static final long serialVersionUID = 1756202080423312153L;

    private Popup popup;

    /**
     * List contained in the drop-down dialog.
     */
    private JList list;

    /**
     * Vectors containing the original data and the filtered data for the
     * suggestions.
     */
    private Vector<String> data, suggestions;

    /**
     * Separate matcher-thread, prevents the text-field from hanging while the
     * suggestions are beeing prepared.
     */
    private InterruptableMatcher matcher;

    /**
     * Needed for the new narrowing search, so we know when to reset the list
     */
    private String lastWord = "";

    /**
     * Create a new JSuggestField.
     *
     * @param owner Frame containing this JSuggestField
     */
    public JSuggestField(Window owner) {
        super();
        data = new Vector<String>();
        suggestions = new Vector<String>();
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                hideSuggest();
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                hideSuggest();
            }

        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hideSuggest();
            }
        });
        list = new JList();
        list.setFocusable(false);
        list.addMouseListener(new MouseAdapter() {
            private int selected;

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selected == list.getSelectedIndex()) {
                    // provide double-click for selecting a suggestion
                    setText((String) list.getSelectedValue());
                    fireActionPerformed();
                    hideSuggest();
                }
                selected = list.getSelectedIndex();
            }

        });
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (isSuggestVisible()) {
                        list.setSelectedIndex(list.getSelectedIndex() + 1);
                        list.ensureIndexIsVisible(list.getSelectedIndex() + 1);
                        setText((String) list.getSelectedValue());
                        return;
                    } else {
                        showPopup();
                    }
                    return;
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (isSuggestVisible()) {
                        list.setSelectedIndex(list.getSelectedIndex() - 1);
                        list.ensureIndexIsVisible(list.getSelectedIndex() - 1);
                        setText((String) list.getSelectedValue());
                    } else {
                        showPopup();
                    }
                    return;
                } else if ((e.getKeyCode() == KeyEvent.VK_ENTER
                        || e.getKeyCode() == KeyEvent.VK_TAB)
                        && list.getSelectedIndex() != -1
                        && suggestions.size() > 0) {
                    setText((String) list.getSelectedValue());
                    hidePopup();
                    return;
                }
                showSuggest();
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
    }

    /**
     * Create a new JSuggestField.
     *
     * @param owner Frame containing this JSuggestField
     * @param data Available suggestions
     */
    public JSuggestField(Window owner, Vector<String> data) {
        this(owner);
        setSuggestData(data);
    }

    /**
     * Sets new data used to suggest similar words.
     *
     * @param data Vector containing available words
     * @return success, true unless the data-vector was null
     */
    public boolean setSuggestData(Vector<String> data) {
        if (data == null) {
            return false;
        }
//        Collections.sort(data);
        this.data = data;
        list.setListData(data);
        suggestions.clear();
        return true;
    }

    /**
     * Get all words that are available for suggestion.
     *
     * @return Vector containing Strings
     */
    @SuppressWarnings("unchecked")
    public Vector<String> getSuggestData() {
        return (Vector<String>) data.clone();
    }

    /**
     * Force the suggestions to be displayed (Useful for buttons e.g. for using
     * JSuggestionField like a ComboBox)
     */
    public void showSuggest() {
        if (!getText().toLowerCase().contains(lastWord.toLowerCase())) {
            suggestions.clear();
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(data);
        }
        if (matcher != null) {
            matcher.stop = true;
        }
        matcher = new InterruptableMatcher();
        SwingUtilities.invokeLater(matcher);
        lastWord = getText();
    }

    /**
     * Force the suggestions to be hidden (Useful for buttons, e.g. to use
     * JSuggestionField like a ComboBox)
     */
    public void hideSuggest() {
        hidePopup();
    }

    private void showPopup() {
        if (popup != null) {
            return;
        }
        JScrollPane scroll = new JScrollPane(list,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        Dimension dim = new Dimension(getSize().width,
                Math.min(12, data.size()) *
                (list.getCellRenderer().getListCellRendererComponent(list, "XXX", 0, true, true)
                        .getPreferredSize().height + 4));
        scroll.setPreferredSize(dim);
        Point loc = getLocationOnScreen();
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Rectangle screenBounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int screenHeight = screenBounds.height - screenInsets.bottom;
        if (loc.y + getHeight() + dim.getHeight() > screenHeight) {
            loc.y -= dim.getHeight();
        } else {
            loc.y += getHeight();
        }

        popup = PopupFactory.getSharedInstance().getPopup(this, scroll, loc.x, loc.y);
        popup.show();
    }

    private void hidePopup() {
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }
    
    private boolean isSuggestVisible() {
        return popup != null;
    }

    private class InterruptableMatcher implements Runnable {

        private boolean stop;

        /**
         * Standard run method used in threads responsible for the actual search
         */
        @Override
        public void run() {
            if (stop || !JSuggestField.this.isVisible()) {
                return;
            }
            try {
//                Iterator<String> it = suggestions.iterator();
//                String word = getText();
//                while (it.hasNext()) {
//                    // rather than using the entire list, let's rather remove
//                    // the words that don't match, thus narrowing
//                    // the search and making it faster
//                    if (caseSensitive) {
//                        if (!suggestMatcher.matches(it.next(), word)) {
//                            it.remove();
//                        }
//                    } else {
//                        if (!suggestMatcher.matches(it.next().toLowerCase(), word.toLowerCase())) {
//                            it.remove();
//                        }
//                    }
//                }

                String word = getText().toLowerCase(Locale.ROOT);
                suggestions.clear();
                data.stream()
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .filter(s -> s.startsWith(word))
                        .forEachOrdered(suggestions::add);
                data.stream()
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .filter(s -> !s.startsWith(word) && s.contains(word))
                        .forEachOrdered(suggestions::add);

                if (suggestions.size() > 0) {
                    list.setListData(suggestions);
                    showPopup();
                    list.setSelectedIndex(0);
                    list.ensureIndexIsVisible(0);
                } else {
                    hidePopup();
                }
            } catch (Exception ex) {
                // Despite all precautions, external changes have occurred.
                // Let the new thread handle it...
                Logger.getLogger(JSuggestField.class.getName()).log(Level.WARNING, "Error in matcher", ex);
            }
        }
    }

    @Override
    protected void fireActionPerformed() {
        if (matcher != null) {
            matcher.stop = true;
            matcher = null;
        }
        super.fireActionPerformed();
    }


}
