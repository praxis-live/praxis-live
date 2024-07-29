package org.praxislive.ide.core.api;

import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Related tests for Task, AbstractTask and SerialTasks.
 */
public class TasksTest {

    public TasksTest() {
    }

    @Test
    public void testCompleteTask() {
        runInEDT(() -> {
            Task task = new CompleteTask();
            assertEquals(Task.State.NEW, task.getState());
            Task.State state = task.execute();
            assertEquals(Task.State.COMPLETED, state);
            assertEquals(Task.State.COMPLETED, task.getState());
        });
    }

    @Test
    public void testAsyncCompleteTask() throws Exception {
        CompletableFuture<Task.State> futureState = new CompletableFuture<>();
        runInEDT(() -> {
            Task task = new AsyncCompleteTask();
            assertEquals(Task.State.NEW, task.getState());
            Task.State state = task.execute();
            assertEquals(Task.State.RUNNING, state);
            task.addPropertyChangeListener(ev -> {
                assertEquals(Task.PROP_STATE, ev.getPropertyName());
                assertEquals(Task.State.RUNNING, ev.getOldValue());
                assertEquals(Task.State.COMPLETED, ev.getNewValue());
                futureState.complete(task.getState());
            });
        });
        assertEquals(Task.State.COMPLETED, futureState.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    public void testAsyncCompleteCancelledTask() throws Exception {
        CompletableFuture<Task.State> futureState = new CompletableFuture<>();
        runInEDT(() -> {
            Task task = new AsyncCompleteTask();
            assertEquals(Task.State.NEW, task.getState());
            Task.State state = task.execute();
            assertEquals(Task.State.RUNNING, state);
            task.addPropertyChangeListener(ev -> {
                assertEquals(Task.PROP_STATE, ev.getPropertyName());
                assertEquals(Task.State.RUNNING, ev.getOldValue());
                assertEquals(Task.State.CANCELLED, ev.getNewValue());
                futureState.complete(task.getState());
            });
            task.cancel();
        });
        assertEquals(Task.State.CANCELLED, futureState.get(1, TimeUnit.SECONDS));
    }
    
    @Test
    public void testSerialTasks() throws Exception {
        CompletableFuture<Task.State> futureState = new CompletableFuture<>();
        runInEDT(() -> {
            Task task1 = new AsyncCompleteTask();
            Task task2 = new CompleteTask();
            Task task3 = new AsyncCompleteTask();
            Task task = new SerialTasks(task1, task2, task3);
            assertEquals(Task.State.NEW, task.getState());
            Task.State state = task.execute();
            assertEquals(Task.State.RUNNING, state);
            task.addPropertyChangeListener(ev -> {
                assertEquals(Task.PROP_STATE, ev.getPropertyName());
                assertEquals(Task.State.RUNNING, ev.getOldValue());
                assertEquals(Task.State.COMPLETED, ev.getNewValue());
                assertEquals(Task.State.COMPLETED, task1.getState());
                assertEquals(Task.State.COMPLETED, task2.getState());
                assertEquals(Task.State.COMPLETED, task3.getState());
                futureState.complete(task.getState());
            });
        });
        assertEquals(Task.State.COMPLETED, futureState.get(1, TimeUnit.SECONDS));
    }

    private void runInEDT(Runnable test) {
        if (EventQueue.isDispatchThread()) {
            test.run();
        } else {
            try {
                CompletableFuture.runAsync(test, EventQueue::invokeLater).get();
            } catch (Exception ex) {
                if (ex.getCause() instanceof AssertionError aex) {
                    throw aex;
                } else {
                    throw new Error(ex);
                }
            }
        }
    }

    private static class CompleteTask extends AbstractTask {

        @Override
        protected void handleExecute() throws Exception {
            updateState(State.COMPLETED);
        }

    }

    private static class AsyncCompleteTask extends AbstractTask {

        @Override
        protected void handleExecute() throws Exception {
            EventQueue.invokeLater(() -> {
                if (State.RUNNING == getState()) {
                    updateState(State.COMPLETED);
                }
            });
        }

        @Override
        protected boolean handleCancel() {
            return true;
        }

    }

}
