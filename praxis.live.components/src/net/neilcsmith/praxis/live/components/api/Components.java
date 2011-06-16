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

package net.neilcsmith.praxis.live.components.api;

import java.awt.Image;
import java.util.Set;
import javax.swing.Action;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.live.components.CategoryChildren;
import net.neilcsmith.praxis.live.components.ComponentRegistry;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public class Components {

    private final static Image DEFAULT_ICON = ImageUtilities.loadImage(
            "net/neilcsmith/praxis/live/components/resources/default-icon.png", true);

    private Components() {}

    public static Image getIcon(ComponentType type) {
        return DEFAULT_ICON;
    }

    public static Node createCategoryView() {
        return new AbstractNode(new CategoryChildren());
    }

    public static Node createCategoryView(String ... categories) {
        return new AbstractNode(new CategoryChildren(categories));
    }

    public static PaletteController getPalette() {
        return PaletteFactory.createPalette(new AbstractNode(new CategoryChildren()), new EmptyPaletteActions());
    }

    public static PaletteController getPalette(String ... categories) {
        return PaletteFactory.createPalette(new AbstractNode(new CategoryChildren(categories)), new EmptyPaletteActions());
    }

    public static Set<ComponentType> getAllTypes() {
        return ComponentRegistry.getDefault().getAllComponents();
    }

    public static Set<ComponentType> getAllRootTypes() {
        return ComponentRegistry.getDefault().getAllRoots();
    }

    private static class EmptyPaletteActions extends PaletteActions {

        private static Action[] EMPTY_ACTIONS = new Action[0];

        @Override
        public Action[] getImportActions() {
            return EMPTY_ACTIONS;
        }

        @Override
        public Action[] getCustomPaletteActions() {
            return EMPTY_ACTIONS;
        }

        @Override
        public Action[] getCustomCategoryActions(Lookup category) {
            return EMPTY_ACTIONS;
        }

        @Override
        public Action[] getCustomItemActions(Lookup item) {
            return EMPTY_ACTIONS;
        }

        @Override
        public Action getPreferredAction(Lookup item) {
            return null;
        }

    }

}
