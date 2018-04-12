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

package org.praxislive.ide.pxr.palette;

import org.praxislive.core.services.ComponentFactory;
import org.praxislive.ide.components.api.Components;
import org.praxislive.meta.TypeRewriter;
import org.netbeans.spi.palette.PaletteFilter;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;

/**
 *
 * @author Neil C Smith <http://neilcsmith.net>
 */
class DefaultPaletteFilter extends PaletteFilter {
        
        private String[] categories;

        DefaultPaletteFilter(String[] categories) {
            this.categories = categories;
        }
        
        @Override
        public boolean isValidCategory(Lookup lkp) {
            Node categoryNode = lkp.lookup(Node.class);
            DataFolder folder = categoryNode.getCookie(DataFolder.class);
            if (folder == null || folder.getChildren().length == 0) {
                return true;
            }
            if (isValidCategory(categoryNode.getName())) {
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
                if (data.isDeprecated()) {
                    if (Components.getShowDeprecated()) {
                        TypeRewriter rw = data.getLookup().find(TypeRewriter.class).orElse(null);
                        if (rw == null || !TypeRewriter.isIdentity(rw)) {
                            return true;
                        }
                    }
                    return false;
                }
                if (data.isTest()) {
                    return Components.getShowTest();
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
