/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.praxislive.ide.pxs;

import org.praxislive.core.CallArguments;
import org.praxislive.core.Component;
import org.praxislive.core.services.ScriptService;
import org.praxislive.core.services.ServiceUnavailableException;
import org.praxislive.core.types.PString;
import org.praxislive.ide.core.api.ExtensionProvider;
import org.praxislive.ide.core.api.HubUnavailableException;
import org.praxislive.ide.util.AbstractHelperComponent;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service = ExtensionProvider.class)
public class PXSHelper implements ExtensionProvider {

    private final static ComponentImpl component = new ComponentImpl();

    @Override
    public Component getExtensionComponent() {
        return component;
    }

    public static void executeScript(String script) {
        component.executeScript(script);
    }

    private static class ComponentImpl extends AbstractHelperComponent {

        private void executeScript(String script) {
            try {
                send(ScriptService.INSTANCE, ScriptService.EVAL, CallArguments.create(PString.valueOf(script)), null);
            } catch (HubUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ServiceUnavailableException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
