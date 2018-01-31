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
package org.praxislive.ide.util;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.praxislive.ide.core.api.Task;

/**
 *
 * @author Neil C Smith (http://neilcsmith.net)
 */
public abstract class AbstractTask implements Task {

    private final PropertyChangeSupport pcs;
    
    private State state;
    
    protected AbstractTask() {
        pcs = new PropertyChangeSupport(this);
        state = State.NEW;
    }
    
    @Override
    public final State execute() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("execute() must be called on event thread");
        }
        if (state != State.NEW) {
            throw new IllegalStateException("task has already been executed or cancelled");
        }
        updateState(State.RUNNING);
        try {
            handleExecute();
        } catch (Exception ex) {
            updateState(State.ERROR);
        }
        return state;
    }

    @Override
    public final State getState() {
        return state;
    }

    @Override
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public final boolean cancel() {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("cancel() must be called on event thread");
        }
        if (state == State.NEW || state == State.RUNNING) {
            if (handleCancel()) {
                updateState(State.CANCELLED);
                return true;
            }
        }
        return false;
    }
    
    protected final void updateState(State newState) {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("updateState() must be called on event thread");
        }
        if (this.state == newState) {
            return;
        }
        switch (newState) {
            case RUNNING :
                if (this.state != State.NEW) {
                    throw new IllegalStateException("Trying to set finished task back to running");
                }
                break;
            case COMPLETED :
            case ERROR :
            case CANCELLED :
                if (this.state != State.NEW && this.state != State.RUNNING) {
                    throw new IllegalStateException("Trying to set state of finished task");
                }
                break;
            default:
                throw new IllegalStateException();
        }
        State old = this.state;
        this.state = newState;
        pcs.firePropertyChange(PROP_STATE, old, state);
    }
    
    protected abstract void handleExecute() throws Exception;
    
    protected boolean handleCancel() {
        return false;
    }
    
}
