/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2020 Neil C Smith.
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
package org.praxislive.ide.project;

import java.util.Objects;
import org.praxislive.ide.project.api.ExecutionElement;
import org.praxislive.ide.project.spi.ElementHandler;

/**
 *
 */
final class ExecutionEntry {
    
    private final ExecutionElement element;
    private final ElementHandler handler;

    public ExecutionEntry(ExecutionElement element, ElementHandler handler) {
        this.element = Objects.requireNonNull(element);
        this.handler = Objects.requireNonNull(handler);
    }
    
    public ExecutionElement element() {
        return element;
    }

    public ElementHandler handler() {
        return handler;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + Objects.hashCode(this.element);
        hash = 53 * hash + Objects.hashCode(this.handler);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ExecutionEntry other = (ExecutionEntry) obj;
        if (!Objects.equals(this.element, other.element)) {
            return false;
        }
        return Objects.equals(this.handler, other.handler);
    }
    
    
    
    
}
