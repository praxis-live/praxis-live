/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxs;

import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.interfaces.ScriptService;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import net.neilcsmith.praxis.live.core.api.HubUnavailableException;
import net.neilcsmith.praxis.live.util.AbstractHelperComponent;
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
