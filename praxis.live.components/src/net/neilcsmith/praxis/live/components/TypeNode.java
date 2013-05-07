/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Neil C Smith.
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
package net.neilcsmith.praxis.live.components;

import java.awt.Image;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory.MetaData;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.api.Components;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.HelpCtx;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class TypeNode extends AbstractNode {

    private ComponentType type;
    private Image icon;
    private MetaData<? extends Component> data;

    TypeNode(ComponentType type, MetaData<? extends Component> data) {
        super(Children.LEAF, Lookups.singleton(type));
        setName(type.toString());
        this.type = type;
        this.data = data;
    }

    @Override
    public Image getIcon(int type) {
        if (icon == null) {
            icon = Components.getIcon(this.type);
        }
        return icon;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(type.toString());
    }

    @Override
    public String getHtmlDisplayName() {
        if (data != null) {
            if (data.isDeprecated()) {
                return "<s>" + type.toString() + "</s>";
            } else if (data.isTest()) {
                return "<i>" + type.toString() + "</i>";
            }
        }
        return super.getHtmlDisplayName();
    }
}
