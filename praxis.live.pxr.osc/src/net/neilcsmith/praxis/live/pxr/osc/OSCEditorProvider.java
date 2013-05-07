/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.neilcsmith.praxis.live.pxr.osc;

import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.pxr.api.RootEditor;
import net.neilcsmith.praxis.live.pxr.api.RootProxy;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Neil C Smith
 */
@ServiceProvider(service=RootEditor.Provider.class)
public class OSCEditorProvider implements RootEditor.Provider {

    @Override
    public RootEditor createEditor(RootProxy model) {
        ComponentType type = model.getType();
        if (type.toString().equals("root:osc")) {
            return new OSCEditor(model);
        }
        return null;
    }
    
}

