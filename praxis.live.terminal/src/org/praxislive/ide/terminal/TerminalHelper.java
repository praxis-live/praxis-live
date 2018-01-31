/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.praxislive.ide.terminal;

import org.praxislive.core.Component;
import org.praxislive.ide.core.api.ExtensionProvider;
import org.praxislive.ide.util.AbstractHelperComponent;
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
