/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Neil C Smith.
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

package net.neilcsmith.praxis.live.pxr.api;

import javax.swing.Action;
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.live.components.api.Components;
import net.neilcsmith.praxis.live.pxr.palette.ComponentPalette;
import org.netbeans.spi.palette.PaletteActions;
import org.netbeans.spi.palette.PaletteController;
import org.netbeans.spi.palette.PaletteFactory;
import org.netbeans.spi.palette.PaletteFilter;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
public class PaletteUtils {
    
    private PaletteUtils() {}
    
    
    public static PaletteController getPalette(String ... categories) {
        String palettePath = ComponentPalette.getDefault().getFolderPath();
        DataFolder paletteFolder = DataFolder.findFolder(FileUtil.getConfigFile(palettePath));
        return PaletteFactory.createPalette(paletteFolder.getNodeDelegate(), 
                new DefaultPaletteActions(),
                new DefaultPaletteFilter(categories.clone()),
                null);
    }
    
    
    private static class DefaultPaletteFilter extends PaletteFilter {
        
        private String[] categories;

        private DefaultPaletteFilter(String[] categories) {
            this.categories = categories;
        }
        
        @Override
        public boolean isValidCategory(Lookup lkp) {
            Node categoryNode = lkp.lookup(Node.class);
            if (isValidCategory(categoryNode.getName())) {
                DataFolder folder = categoryNode.getCookie(DataFolder.class);
                for (DataObject file : folder.getChildren()) {
                    if (isValidItem(file.getLookup())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isValidItem(Lookup lkp) {
            ComponentFactory.MetaData<?> data = lkp.lookup(ComponentFactory.MetaData.class);
            if (data != null) {
                if (data.isTest()) {
                    return Components.getShowTestComponents();
                } else if (data.isDeprecated()) {
                    return false;
                }
            }
            return true;
        }
        
        private boolean isValidCategory(String name) {
             for (String category : categories) {
                if (name.startsWith(category)) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
     private static class PaletteFilterNode extends FilterNode {

        private PaletteFilterNode(Node original) {
            super(original, original.isLeaf() ? FilterNode.Children.LEAF
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
    
    private static class DefaultPaletteActions extends PaletteActions {

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
