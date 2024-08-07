/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith
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

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;

class JSuggestField extends JTextField {

    private final List<String> suggestions;

    private List<String> data;
    private JList list;
    private Matcher matcher;
    private Popup popup;
    private String lastWord;

    JSuggestField() {
        data = List.of();
        suggestions = new ArrayList<>();
        lastWord = "";
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
            @Override
            public void mouseReleased(MouseEvent e) {
                setText((String) list.getSelectedValue());
                fireActionPerformed();
                hideSuggest();
            }

        });
        addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keycode = e.getKeyCode();
                if (keycode == KeyEvent.VK_ESCAPE) {
                    return;
                }
                if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_UP) {
                    int diff = keycode == KeyEvent.VK_DOWN ? 1 : -1;
                    if (isSuggestVisible()) {
                        list.setSelectedIndex(list.getSelectedIndex() + diff);
                        list.ensureIndexIsVisible(list.getSelectedIndex() + diff);
                        setText((String) list.getSelectedValue());
                    } else {
                        showPopup();
                    }
                    return;
                } else if ((e.getKeyCode() == KeyEvent.VK_ENTER
                        || e.getKeyCode() == KeyEvent.VK_TAB)
                        && list.getSelectedIndex() != -1
                        && !suggestions.isEmpty()) {
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

    @Override
    protected void fireActionPerformed() {
        if (matcher != null) {
            matcher.stop = true;
            matcher = null;
        }
        super.fireActionPerformed();
    }

    void setSuggestData(List<String> data) {
        this.data = List.copyOf(data);
        list.setListData(this.data.toArray());
        suggestions.clear();
    }

    List<String> getSuggestData() {
        return data;
    }

    private void showSuggest() {
        if (!getText().toLowerCase().contains(lastWord.toLowerCase())) {
            suggestions.clear();
        }
        if (suggestions.isEmpty()) {
            suggestions.addAll(data);
        }
        if (matcher != null) {
            matcher.stop = true;
        }
        matcher = new Matcher();
        SwingUtilities.invokeLater(matcher);
        lastWord = getText();
    }

    private void hideSuggest() {
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
                Math.min(12, data.size())
                * (list.getCellRenderer().getListCellRendererComponent(list, "XXX", 0, true, true)
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

    private class Matcher implements Runnable {

        private boolean stop;

        @Override
        public void run() {
            if (stop || !isVisible()) {
                return;
            }

            String word = getText().toLowerCase(Locale.ROOT);
            suggestions.clear();
            data.forEach(datum -> {
                if (datum.toLowerCase(Locale.ROOT).startsWith(word)) {
                    suggestions.add(datum);
                }
            });
            data.forEach(datum -> {
                String lower = datum.toLowerCase(Locale.ROOT);
                if (!lower.startsWith(word) && lower.contains(word)) {
                    suggestions.add(datum);
                }
            });

            if (!suggestions.isEmpty()) {
                list.setListData(suggestions.toArray());
                showPopup();
                list.setSelectedIndex(0);
                list.ensureIndexIsVisible(0);
            } else {
                hidePopup();
            }
        }
    }

}
