/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.terminal;

import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.util.AbstractHelperComponent;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class TerminalHelper extends AbstractHelperComponent {

    private final static TerminalHelper INSTANCE = new TerminalHelper();

    private TerminalHelper() {}


    public static TerminalHelper getDefault() {
        return INSTANCE;
    }

    @ServiceProvider(service=ExtensionProvider.class)
    public static class Provider implements ExtensionProvider {

        @Override
        public Component getExtensionComponent() {
            return getDefault();
        }

    }

}
