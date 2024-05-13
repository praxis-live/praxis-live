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
package org.praxislive.ide.core.api;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A base implementation of a Task.
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

    /**
     * Update the task state.
     *
     * @param newState new state
     * @throws IllegalStateException if new state is not valid
     */
    protected final void updateState(State newState) {
        if (!EventQueue.isDispatchThread()) {
            throw new IllegalStateException("updateState() must be called on event thread");
        }
        if (this.state == newState) {
            return;
        }
        switch (newState) {
            case RUNNING:
                if (this.state != State.NEW) {
                    throw new IllegalStateException("Trying to set finished task back to running");
                }
                break;
            case COMPLETED:
            case ERROR:
            case CANCELLED:
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

    /**
     * Handle a call to execution. The state will have been updated to
     * {@link State#RUNNING} prior to this method. This method should call
     * {@link #updateState(org.praxislive.ide.core.api.Task.State)} to change
     * the state returned from {@link #execute()}, or update the state at a
     * later point. If this method throws an exception, the task will be set to
     * the {@link State#ERROR} state.
     *
     * @throws Exception if the task cannot be executed
     */
    protected abstract void handleExecute() throws Exception;

    /**
     * If the task is cancellable, this method should be overridden and handle
     * any necessary steps, returning {@code true} if the task has been
     * cancelled. The state will be updated automatically.
     *
     * @return true to have task marked as cancelled
     */
    protected boolean handleCancel() {
        return false;
    }

}
