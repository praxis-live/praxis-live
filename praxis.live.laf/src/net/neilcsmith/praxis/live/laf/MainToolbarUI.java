package net.neilcsmith.praxis.live.laf;

import com.nilo.plaf.nimrod.NimRODToolBarUI;
import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class MainToolbarUI extends NimRODToolBarUI implements ContainerListener {

    @Override
    public void installUI(JComponent jc) {
        super.installUI(jc);
        jc.addContainerListener(this);
        for (Component c : jc.getComponents()) {
            if (c instanceof AbstractButton) {
                resetButton((AbstractButton)c);
            }
        }
    }

    @Override
    public void uninstallUI(JComponent jc) {
        super.uninstallUI(jc);
        jc.removeContainerListener(this);
    }

    private void resetButton(AbstractButton button) {
        button.setOpaque(true);
        button.setBorderPainted(true);
    }

 
    

    @Override
    public void componentAdded(ContainerEvent ce) {
        Component c = ce.getChild();
        if (c instanceof AbstractButton) {
            resetButton((AbstractButton)c);
        }
    }

    @Override
    public void componentRemoved(ContainerEvent ce) {
    }
    
    public static ComponentUI createUI(JComponent c) {
        return new MainToolbarUI();
    }
}
