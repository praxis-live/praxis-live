package com.revivius.nb.darcula.ui;

import com.bulenkov.darcula.DarculaTableHeaderBorder;
import java.awt.Component;
import java.awt.Insets;

/**
 * Increases table header insets.
 * @author Revivius
 */
public class IncreasedInsetsTableHeaderBorder extends DarculaTableHeaderBorder {

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(1, 2, 1, 2);
    }
}
