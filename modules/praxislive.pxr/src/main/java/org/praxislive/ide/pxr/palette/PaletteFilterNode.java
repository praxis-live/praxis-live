/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2024 Neil C Smith.
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
 * Please visit https://www.praxislive.org if you need additional information or
 * have any questions.
 */
package org.praxislive.ide.pxr.palette;

import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 *
 */
class PaletteFilterNode extends FilterNode {

    PaletteFilterNode(DefaultComponentPalette componentPalette, Node original) {
        super(original, new PaletteChildren(componentPalette, original),
                new ProxyLookup(original.getLookup(), Lookups.singleton(componentPalette)));
    }

    @Override
    public String getDisplayName() {
        String html = getHtmlDisplayName();
        if (html != null) {
            return "<html>" + html;
        } else {
            String name = super.getDisplayName().replace("_", ":");
            int index = name.lastIndexOf(":");
            if (isLeaf() && index > 0 && index < name.length() - 1) {
                return name.substring(index + 1);
            } else {
                return name;
            }
        }
    }

    @Override
    public String getHtmlDisplayName() {
        return getOriginal().getHtmlDisplayName();
    }

    private static class PaletteChildren extends FilterNode.Children {

        private final DefaultComponentPalette componentPalette;

        private PaletteChildren(DefaultComponentPalette componentPalette, Node original) {
            super(original);
            this.componentPalette = componentPalette;
        }

        @Override
        protected Node copyNode(Node original) {
            return new PaletteFilterNode(componentPalette, original);
        }
    }

}
