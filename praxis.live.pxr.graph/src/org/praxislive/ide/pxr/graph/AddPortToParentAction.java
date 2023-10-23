/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2019 Neil C Smith.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License version 3
 * along with this work; if not, see http://www.gnu.org/licenses/
 *
 *
 * Please visit http://neilcsmith.net if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.graph;

import org.praxislive.core.PortAddress;
import org.praxislive.core.Value;
import org.praxislive.core.types.PMap;
import org.praxislive.ide.graph.PinID;
import org.praxislive.ide.model.ContainerProxy;
import org.praxislive.ide.properties.PraxisProperty;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 *
 */
@NbBundle.Messages({
    "LBL.addPortToParent=Add Port to Parent",
    "LBL.enterPortID=Enter an ID for the port",
    "ERR.noPortsProperty=No ports property found on parent",
    "ERR.invalidPortID=The Port ID is invalid",
    "ERR.existingPortID=The Port ID already exists"
})
class AddPortToParentAction extends AbstractAction {

    private final GraphEditor editor;
    private final PinID<String> pin;

    AddPortToParentAction(GraphEditor editor, PinID<String> pin) {
        super(Bundle.LBL_addPortToParent());
        this.editor = editor;
        this.pin = pin;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // need invoke later?? - problems with dialogs and menus in past.
        EventQueue.invokeLater(this::addPortToParent);
    }
    
    private void addPortToParent() {
        PraxisProperty<Value> portProp = findPortsProperty();
        if (portProp == null) {
            DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message(Bundle.ERR_noPortsProperty(),
                            NotifyDescriptor.ERROR_MESSAGE));
            return;
        }
        PMap ports = PMap.from(portProp.getValue()).orElse(PMap.EMPTY);
        String suggestedID = pin.getParent() + "-" + pin.getName();
        if (ports.get(suggestedID) != null) {
            suggestedID = "";
        }
        NotifyDescriptor.InputLine dlg = new NotifyDescriptor.InputLine(
                "ID:", Bundle.LBL_enterPortID());
        dlg.setInputText(suggestedID);
        Object retval = DialogDisplayer.getDefault().notify(dlg);
        if (retval == NotifyDescriptor.OK_OPTION) {
            String id = dlg.getInputText();
            if (!PortAddress.isValidID(id)) {
                DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message(Bundle.ERR_invalidPortID(),
                            NotifyDescriptor.ERROR_MESSAGE));
                return;
            }
            if (ports.get(id) != null) {
                DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Message(Bundle.ERR_existingPortID(),
                            NotifyDescriptor.ERROR_MESSAGE));
                return;
            }
            PMap.Builder bld = PMap.builder();
            for (String key : ports.keys()) {
                bld.put(key, ports.get(key));
            }
            bld.put(id, pin.getParent() + "!" + pin.getName());
            portProp.setValue(bld.build());
        }
    }
    
    
    private PraxisProperty<Value> findPortsProperty() {
        ContainerProxy container = editor.getContainer();
        for (Node.PropertySet props : container.getNodeDelegate().getPropertySets()) {
            for (Node.Property<?> prop : props.getProperties()) {
                if ("ports".equals(prop.getName()) && prop instanceof PraxisProperty) {
                    return (PraxisProperty<Value>) prop;
                }
            }
        }
        return null;
    } 
    
    
}
