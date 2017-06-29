/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2017 Neil C Smith / David von Ah
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
package net.neilcsmith.praxis.live.pxr.graph;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.IllegalComponentStateException;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

class JSuggestField extends JTextField {

    /**
     * unique ID for serialization
     */
    private static final long serialVersionUID = 1756202080423312153L;

    /**
     * Dialog used as the drop-down list.
     */
    private JDialog d;

    /**
     * Location of said drop-down list.
     */
    private Point location;

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
     * The last chosen variable which exists. Needed if user continued to type
     * but didn't press the enter key
     *
     */
    private String lastChosenExistingVariable;

    /**
     * Hint that will be displayed if the field is empty
     */
    private String hint;

    /**
     * Listeners, fire event when a selection as occured
     */
    private LinkedList<ActionListener> listeners;

    private Matcher suggestMatcher = (dataWord, searchWord) -> dataWord.contains(searchWord);

    private boolean caseSensitive = false;

    /**
     * Create a new JSuggestField.
     *
     * @param owner Frame containing this JSuggestField
     */
    public JSuggestField(Window owner) {
        super();
        data = new Vector<String>();
        suggestions = new Vector<String>();
        listeners = new LinkedList<ActionListener>();
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                d.setVisible(false);
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                d.setVisible(false);
            }

        });
        addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                d.setVisible(false);
//
//                if (getText().equals("") && e.getOppositeComponent() != null && e.getOppositeComponent().getName() != null) {
//                    if (!e.getOppositeComponent().getName().equals("suggestFieldDropdownButton")) {
//                        setText(hint);
//                    }
//                } else if (getText().equals("")) {
//                    setText(hint);
//                }
            }

            @Override
            public void focusGained(FocusEvent e) {
//                if (getText().equals(hint)) {
//                    setText("");
//                }

//                showSuggest();
            }
        });
        d = new JDialog(owner);
        d.setUndecorated(true);
        d.setFocusableWindowState(false);
        d.setFocusable(false);
        list = new JList();
        list.addMouseListener(new MouseListener() {
            private int selected;

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selected == list.getSelectedIndex()) {
                    // provide double-click for selecting a suggestion
                    setText((String) list.getSelectedValue());
                    lastChosenExistingVariable = list.getSelectedValue().toString();
//                    fireActionEvent();
                    fireActionPerformed();
                    d.setVisible(false);
                }
                selected = list.getSelectedIndex();
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });
        d.add(new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
        d.pack();
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
                    if (d.isVisible()) {
                        list.setSelectedIndex(list.getSelectedIndex() + 1);
                        list.ensureIndexIsVisible(list.getSelectedIndex() + 1);
                        setText((String) list.getSelectedValue());
                        return;
                    } else {
                        showSuggest();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (d.isVisible()) {
                        list.setSelectedIndex(list.getSelectedIndex() - 1);
                        list.ensureIndexIsVisible(list.getSelectedIndex() - 1);
                        setText((String) list.getSelectedValue());
                    } else {
                        showSuggest();
                    }
                    return;
                } else if ((e.getKeyCode() == KeyEvent.VK_ENTER ||
                        e.getKeyCode() == KeyEvent.VK_TAB)
                        && list.getSelectedIndex() != -1 
                        && suggestions.size() > 0) {
                    setText((String) list.getSelectedValue());
                    lastChosenExistingVariable = list.getSelectedValue().toString();
                    d.setVisible(false);
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
     * Set preferred size for the drop-down that will appear.
     *
     * @param size Preferred size of the drop-down list
     */
    public void setPreferredSuggestSize(Dimension size) {
        d.setPreferredSize(size);
    }

    /**
     * Set minimum size for the drop-down that will appear.
     *
     * @param size Minimum size of the drop-down list
     */
    public void setMinimumSuggestSize(Dimension size) {
        d.setMinimumSize(size);
    }

    /**
     * Set maximum size for the drop-down that will appear.
     *
     * @param size Maximum size of the drop-down list
     */
    public void setMaximumSuggestSize(Dimension size) {
        d.setMaximumSize(size);
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
        relocate();
    }

    /**
     * Force the suggestions to be hidden (Useful for buttons, e.g. to use
     * JSuggestionField like a ComboBox)
     */
    public void hideSuggest() {
        d.setVisible(false);
    }

    /**
     * @return boolean Visibility of the suggestion window
     */
    public boolean isSuggestVisible() {
        return d.isVisible();
    }

    /**
     * Place the suggestion window under the JTextField.
     */
    private void relocate() {
        try {
            Point location = getLocationOnScreen();
            GraphicsConfiguration gc = getGraphicsConfiguration();
            Rectangle screenBounds = gc.getBounds();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            int screenHeight = screenBounds.height - screenInsets.bottom;
            if (location.y + getHeight() + d.getHeight() > screenHeight) {
                location.y -= d.getHeight();
            } else {
                location.y += getHeight();
            }
            d.setLocation(location);
        } catch (IllegalComponentStateException e) {
            return; // might happen on window creation
        }
    }

    private class InterruptableMatcher implements Runnable {
        
        private boolean stop;

        /**
         * Standard run method used in threads responsible for the actual search
         */
        @Override
        public void run() {
            if (stop) {
                return;
            }
            try {
                Iterator<String> it = suggestions.iterator();
                String word = getText();
                while (it.hasNext()) {
                    // rather than using the entire list, let's rather remove
                    // the words that don't match, thus narrowing
                    // the search and making it faster
                    if (caseSensitive) {
                        if (!suggestMatcher.matches(it.next(), word)) {
                            it.remove();
                        }
                    } else {
                        if (!suggestMatcher.matches(it.next().toLowerCase(), word.toLowerCase())) {
                            it.remove();
                        }
                    }
                }
                if (suggestions.size() > 0) {
                    list.setListData(suggestions);
                    list.setSelectedIndex(0);
                    list.ensureIndexIsVisible(0);
                    d.setVisible(true);
                } else {
                    d.setVisible(false);
                }
            } catch (Exception ex) {
                // Despite all precautions, external changes have occurred.
                // Let the new thread handle it...
                Logger.getLogger(JSuggestField.class.getName()).log(Level.WARNING, "Error in matcher", ex);
            }
        }
    }

//    /**
//     * Adds a listener that notifies when a selection has occured
//     *
//     * @param listener ActionListener to use
//     */
//    public void addSelectionListener(ActionListener listener) {
//        if (listener != null) {
//            listeners.add(listener);
//        }
//    }
//
//    /**
//     * Removes the Listener
//     *
//     * @param listener ActionListener to remove
//     */
//    public void removeSelectionListener(ActionListener listener) {
//        listeners.remove(listener);
//    }
//
//    /**
//     * Use ActionListener to notify on changes so we don't have to create an
//     * extra event
//     */
//    private void fireActionEvent() {
//        ActionEvent event = new ActionEvent(this, 0, getText());
//        for (ActionListener listener : listeners) {
//            listener.actionPerformed(event);
//        }
//    }

    @Override
    protected void fireActionPerformed() {
        if (matcher != null) {
            matcher.stop = true;
            matcher = null;
        }
        super.fireActionPerformed();
    } 

    
    
    /**
     * Returns the selected value in the drop down list
     *
     * @return selected value from the user or null if the entered value does
     * not exist
     */
    public String getLastChosenExistingVariable() {
        return lastChosenExistingVariable;
    }

    /**
     * Get the hint that will be displayed when the field is empty
     *
     * @return The hint of null if none was defined
     */
    public String getHint() {
        return hint;
    }

    /**
     * Set a text that will be displayed when the field is empty
     *
     * @param hint Hint such as "Search..."
     */
    public void setHint(String hint) {
        this.hint = hint;
    }

    /**
     * Determine how the suggestions are generated. Default is the simple
     * {@link ContainsMatcher}
     *
     * @param suggestMatcher matcher that determines if a data word may be
     * suggested for the current search word.
     */
    public void setSuggestMatcher(Matcher suggestMatcher) {
        this.suggestMatcher = suggestMatcher;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public static interface Matcher {

        public boolean matches(String dataWord, String searchWord);
    }

}
