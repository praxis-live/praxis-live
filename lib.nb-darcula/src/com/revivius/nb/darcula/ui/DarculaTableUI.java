package com.revivius.nb.darcula.ui;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableUI;

public class DarculaTableUI extends BasicTableUI {

    public static ComponentUI createUI(JComponent c) {
        return new DarculaTableUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        Object rowHeight = UIManager.get("Table.rowHeight");
        if (rowHeight != null) {
            LookAndFeel.installProperty(table, "rowHeight", rowHeight);
        }
    }

}
