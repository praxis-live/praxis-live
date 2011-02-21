/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.neilcsmith.praxis.live.pxs;

import java.util.logging.Logger;
import net.neilcsmith.praxis.core.Call;
import net.neilcsmith.praxis.core.CallArguments;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentAddress;
import net.neilcsmith.praxis.core.ControlAddress;
import net.neilcsmith.praxis.core.PacketRouter;
import net.neilcsmith.praxis.core.info.ControlInfo;
import net.neilcsmith.praxis.core.interfaces.ScriptService;
import net.neilcsmith.praxis.core.interfaces.ServiceUnavailableException;
import net.neilcsmith.praxis.core.types.PString;
import net.neilcsmith.praxis.impl.AbstractComponent;
import net.neilcsmith.praxis.impl.AbstractControl;
import net.neilcsmith.praxis.live.core.api.ExtensionProvider;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
@ServiceProvider(service=ExtensionProvider.class)
public class HelperExtension implements ExtensionProvider {
    
    private final static Logger LOG = Logger.getLogger(HelperExtension.class.getName());

    private final static ComponentImpl component = new ComponentImpl();

    @Override
    public Component getExtensionComponent() {
        return component;
    }

    static void executeScript(String script) {
        try {
            component.executeScript(script);
        } catch (ServiceUnavailableException ex) {
            Exceptions.printStackTrace(ex);
        }
    }


    private static class ComponentImpl extends AbstractComponent {

        private ScriptControl control;

        private ComponentImpl() {
            control = new ScriptControl();
            registerControl("script-control", control);

        }

        private void executeScript(String script) throws ServiceUnavailableException {
            ComponentAddress service = findService(ScriptService.INSTANCE);
            ControlAddress to = ControlAddress.create(service, ScriptService.EVAL);
            ControlAddress from = control.getAddress();
            PString scr = PString.valueOf(script);
            Call call = Call.createCall(to, from, System.nanoTime(), CallArguments.create(scr));
            getPacketRouter().route(call);
        }

        private class ScriptControl extends AbstractControl {

            @Override
            public void call(Call call, PacketRouter router) throws Exception {
                if (call.getType() != Call.Type.RETURN) {
                    LOG.warning("Script Error");
                }
            }

            @Override
            public ControlInfo getInfo() {
                return null;
            }

        }

    }

}
