/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Neil C Smith.
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
import javax.swing.Action;
import net.neilcsmith.praxis.core.Component;
import net.neilcsmith.praxis.core.ComponentFactory.MetaData;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.Root;
import net.neilcsmith.praxis.live.components.CategoryChildren;
import net.neilcsmith.praxis.live.components.ComponentRegistry;
import net.neilcsmith.praxis.live.components.ComponentSettings;
import net.neilcsmith.praxis.meta.IconProvider;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.FilterNode;
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

    private Components() {
    }

    public static Image getIcon(ComponentType type) {
        MetaData<?> data;
        if (type.toString().startsWith("root:")) {
            data = getRootMetaData(type);
        } else {
            data = getMetaData(type);
        }
        if (data != null) {
            IconProvider ip = data.getLookup().get(IconProvider.class);
            if (ip != null) {
                Image img = ip.getIcon(16, 16);
                if (img != null) {
                    return img;
                }
            }
        }
        try {
            for (ComponentIconProvider provider : Lookup.getDefault().lookupAll(ComponentIconProvider.class)) {
                Image img = provider.getIcon(type);
                if (img != null) {
                    return img;
                }
            }
        } catch (Exception ex) {
            //fall through
        }
        return DEFAULT_ICON;
    }

    //@Deprecated
    public static Node createCategoryView() {
        return new AbstractNode(new CategoryChildren());
    }

    //@Deprecated
    public static Node createCategoryView(String... categories) {
        return new AbstractNode(new CategoryChildren(categories));
    }

    //@Deprecated
    public static PaletteController getPalette() {
        Node node = new AbstractNode(new CategoryChildren());
        return PaletteFactory.createPalette(new PaletteFilterNode(node), new EmptyPaletteActions());
    }

    //@Deprecated
    public static PaletteController getPalette(String... categories) {
        Node node = new AbstractNode(new CategoryChildren(categories));
        return PaletteFactory.createPalette(new PaletteFilterNode(node), new EmptyPaletteActions());
    }

    public static ComponentType[] getComponentTypes() {
        return ComponentRegistry.getDefault().getComponentTypes();
    }

    public static ComponentType[] getRootComponentTypes() {
        return ComponentRegistry.getDefault().getRootComponentTypes();
    }

    public static MetaData<? extends Component> getMetaData(ComponentType type) {
        return ComponentRegistry.getDefault().getMetaData(type);
    }

    public static MetaData<? extends Root> getRootMetaData(ComponentType type) {
        return ComponentRegistry.getDefault().getRootMetaData(type);
    }
    
    public static boolean getShowTestComponents() {
        return ComponentSettings.getShowTestComponents();
    }

//    public static ComponentFactory.MetaData<? extends Component> getComponentMetaData()
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

    private static class PaletteFilterNode extends FilterNode {

        private PaletteFilterNode(Node original) {
            super(original, original.isLeaf() ? Children.LEAF
                    : new PaletteFilterChildren(original));
            String html = getHtmlDisplayName();
            if (html != null) {
                setDisplayName("<html>" + html);
            }
        }
    }

    private static class PaletteFilterChildren extends FilterNode.Children {

        private PaletteFilterChildren(Node original) {
            super(original);
        }

        @Override
        protected Node copyNode(Node original) {
            return new PaletteFilterNode(original);
        }
    }
}
