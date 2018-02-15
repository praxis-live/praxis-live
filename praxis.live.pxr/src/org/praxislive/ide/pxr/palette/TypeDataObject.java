/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Neil C Smith.
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

import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.praxislive.core.services.ComponentFactory;
import org.praxislive.core.ComponentType;
import org.praxislive.ide.components.api.Components;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

@MIMEResolver.ExtensionRegistration(
        displayName = "Type",
        mimeType = "application/x-component-type",
        extension = "type"
)
@DataObject.Registration(
        mimeType = "application/x-component-type",
        //        iconBase = "org/praxislive/ide/components/resources/default-icon.png",
        displayName = "Type",
        position = 300
)
public class TypeDataObject extends MultiDataObject {

    private final static Logger LOG = Logger.getLogger(TypeDataObject.class.getName());

    static final String TYPE_ATTR_KEY = "componentType";

    private ComponentType type;
    private ComponentFactory.MetaData<?> data;

    public TypeDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        try {
            type = ComponentType.create(pf.getAttribute(TYPE_ATTR_KEY).toString());
            data = Components.getMetaData(type);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Invalid or non-existent ComponentType in .type file.", ex);
        }
    }

    @Override
    protected DataNode createNodeDelegate() {
        return new Node();
    }

    @Override
    public Lookup getLookup() {
        if (type == null) {
            return super.getLookup();
        } else {
            return new ProxyLookup(super.getLookup(), Lookups.fixed(type, data));
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        if (type == null) {
            return super.getHelpCtx();
        } else {
            return new HelpCtx(type.toString());
        }
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    private class Node extends DataNode {

        Node() {
            super(TypeDataObject.this, Children.LEAF, TypeDataObject.this.getLookup());
        }

        @Override
        public Image getIcon(int size) {
            if (type == null) {
                return super.getIcon(size);
            } else {
                return Components.getIcon(type);
            }
        }

        @Override
        public Image getOpenedIcon(int size) {
            return getIcon(size);
        }

        @Override
        public String getDisplayName() {
            return type.toString();
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

}
