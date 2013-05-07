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
import net.neilcsmith.praxis.core.ComponentFactory;
import net.neilcsmith.praxis.core.ComponentType;
import net.neilcsmith.praxis.core.Lookup;
import net.neilcsmith.praxis.impl.InstanceLookup;
import net.neilcsmith.praxis.meta.DisplayNameProvider;
import net.neilcsmith.praxis.meta.IconProvider;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Neil C Smith
 */
public class MetaDataDecorator<T> extends ComponentFactory.MetaData<T> {
    
    private final ComponentFactory.MetaData<T> delegate;
    private final Lookup lookup;
    
    public MetaDataDecorator(ComponentFactory.MetaData<T> delegate, Object ... exts) {
        if (exts.length == 0) {
            lookup = delegate.getLookup();
        } else {
            lookup = InstanceLookup.create(delegate.getLookup(), exts);
        }
        this.delegate = delegate;    
    }

    @Override
    public Class<T> getComponentClass() {
        return delegate.getComponentClass();
    }

    @Override
    public boolean isTest() {
        return delegate.isTest();
    }

    @Override
    public boolean isDeprecated() {
        return delegate.isDeprecated();
    }

    @Override
    public ComponentType getReplacement() {
        return delegate.getReplacement();
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public static class DisplayName implements DisplayNameProvider {

        private final String name;
        
        public DisplayName(String name) {
            if (name == null) {
                throw new NullPointerException();
            }
            this.name = name;
        }
        
        @Override
        public String getDisplayName() {
            return name;
        }
        
    }
    
    public static class Icon implements IconProvider {
        
        private final String location;
        
        public Icon(String location) {
            if (location == null) {
                throw new NullPointerException();
            }
            this.location = location;
        }

        @Override
        public Image getIcon(int width, int height) {
            return ImageUtilities.loadImage(location, true);
        }
        
    }
    
}
