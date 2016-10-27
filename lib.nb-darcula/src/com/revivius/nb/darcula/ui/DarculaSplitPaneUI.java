package com.revivius.nb.darcula.ui;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Thanks to hudsonb: https://github.com/bulenkov/Darcula/pull/5
 *
 * @author Revivius
 */
public class DarculaSplitPaneUI extends BasicSplitPaneUI {
  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(JComponent c) {
    return new DarculaSplitPaneUI();
  }

  @Override
  public BasicSplitPaneDivider createDefaultDivider() {
    return new DarculaSplitPaneDivider(this);
  }
}
