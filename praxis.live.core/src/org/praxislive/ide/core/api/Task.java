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
package org.praxislive.ide.core.api;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;
import org.openide.util.Cancellable;

/**
 *
 */
public interface Task extends Cancellable {
    
    public final static String PROP_STATE = "state";
    
    public static enum State {NEW, RUNNING, CANCELLED, COMPLETED, ERROR};
    
    public State execute();
    
    public State getState();
    
    public void addPropertyChangeListener(PropertyChangeListener listener);
    
    public void removePropertyChangeListener(PropertyChangeListener listener);
    
    public default Optional<String> description() {
        return Optional.empty();
    }
    
    public default List<String> log() {
        return List.of();
    }
    
}
