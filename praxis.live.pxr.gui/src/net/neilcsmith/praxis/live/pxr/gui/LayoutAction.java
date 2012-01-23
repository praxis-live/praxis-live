/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Neil C Smith.
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
package net.neilcsmith.praxis.live.pxr.gui;

import java.awt.event.ActionEvent;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class LayoutAction extends AbstractAction implements ContextAwareAction, LookupListener, ChangeListener {

    private final static String RESOURCE_DIR = "net/neilcsmith/praxis/live/pxr/gui/resources/";

    static enum Type {

        MoveUp("Move Up", ImageUtilities.loadImageIcon(RESOURCE_DIR + "go-up.png", true)),
        MoveDown("Move Down", ImageUtilities.loadImageIcon(RESOURCE_DIR + "go-down.png", true)),
        MoveLeft("Move Left", ImageUtilities.loadImageIcon(RESOURCE_DIR + "go-left.png", true)),
        MoveRight("Move Right", ImageUtilities.loadImageIcon(RESOURCE_DIR + "go-right.png", true)),
        IncreaseSpanX("Increase Grid Width", ImageUtilities.loadImageIcon(RESOURCE_DIR + "increase-width.png", true)),
        DecreaseSpanX("Decrease Grid Width", ImageUtilities.loadImageIcon(RESOURCE_DIR + "decrease-width.png", true)),
        IncreaseSpanY("Increase Grid Height", ImageUtilities.loadImageIcon(RESOURCE_DIR + "increase-height.png", true)),
        DecreaseSpanY("Decrease Grid Height", ImageUtilities.loadImageIcon(RESOURCE_DIR + "decrease-height.png", true));

        private String name;
        private Icon icon;
        
        Type(String name, Icon icon) {
            this.name = name;
            this.icon = icon;
        }
        
        String getName() {
            return name;
        }
        
        Icon getIcon() {
            return icon;
        }
        
    };
    
    
    private Type type;
    private EditLayer editLayer;
    private Lookup.Result<EditLayer> result;

    LayoutAction(Type type) {
        this(type, Utilities.actionsGlobalContext());
    }

    LayoutAction(Type type, Lookup context) {
        super(type.getName(), type.getIcon());
        this.type = type;
        putValue(Action.SHORT_DESCRIPTION, type.getName());
        setEnabled(false);
        this.result = context.lookupResult(EditLayer.class);
        this.result.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        editLayer.performLayoutAction(type);
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        return new LayoutAction(type, actionContext);
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        setEnabled(editLayer.isLayoutActionEnabled(type));
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        reset();
        Collection<? extends EditLayer> layers = result.allInstances();
        if (layers.isEmpty()) {
            return;
        }
        setup(layers.iterator().next());
    }

    private void reset() {
        if (editLayer != null) {
            editLayer.removeChangeListener(this);
            editLayer = null;
            setEnabled(false);
        }
    }

    private void setup(EditLayer editLayer) {
        this.editLayer = editLayer;
        editLayer.addChangeListener(this);
        setEnabled(editLayer.isLayoutActionEnabled(type));
    }
    
    

}
