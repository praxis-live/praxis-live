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

    @Test
    public void testResultTask() throws Exception {
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        runInEDT(() -> {
            AsyncResultTask task = new AsyncResultTask();
            assertEquals(Task.State.NEW, task.getState());
            Task.State state = task.execute();
            assertEquals(Task.State.RUNNING, state);
            task.addPropertyChangeListener(ev -> {
                assertEquals(Task.PROP_STATE, ev.getPropertyName());
                assertEquals(Task.State.RUNNING, ev.getOldValue());
                assertEquals(Task.State.COMPLETED, ev.getNewValue());
                futureResult.complete(task.result());
            });
        });
        assertEquals("FOO", futureResult.get(1, TimeUnit.SECONDS));
    }

    @Test
    public void testTaskCompletionStage() throws Exception {
        CompletableFuture<Boolean> futureResult = new CompletableFuture<>();
        runInEDT(() -> {
            Task.run(new AsyncCompleteTask())
                    .thenApply(v -> {
                        assertTrue(EventQueue.isDispatchThread());
                        return true;
                    })
                    .thenAccept(b -> futureResult.complete(b));
        });
        assertTrue(futureResult.get(1, TimeUnit.SECONDS));

        CompletableFuture<Boolean> futureResult2 = new CompletableFuture<>();
        runInEDT(() -> {
            Task.run(new AsyncCompleteTask(true))
                    .whenComplete((r, ex) -> {
                        futureResult2.complete(ex != null);
                    });
        });
        assertTrue(futureResult2.get(1, TimeUnit.SECONDS));

    }

    @Test
    public void testTaskResultCompletionStage() throws Exception {
        CompletableFuture<String> futureResult = new CompletableFuture<>();
        runInEDT(() -> {
            Task.WithResult.compute(new AsyncResultTask())
                    .thenAccept(s -> futureResult.complete(s));
        });
        assertEquals("FOO", futureResult.get(1, TimeUnit.SECONDS));
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

        private final boolean fail;

        private AsyncCompleteTask() {
            this(false);
        }

        private AsyncCompleteTask(boolean error) {
            this.fail = error;
        }

        @Override
        protected void handleExecute() throws Exception {
            EventQueue.invokeLater(() -> {
                if (State.RUNNING == getState()) {
                    updateState(fail ? State.ERROR : State.COMPLETED);
                }
            });
        }

        @Override
        protected boolean handleCancel() {
            return true;
        }

    }

    private static class AsyncResultTask extends AbstractTask.WithResult<String> {

        @Override
        protected void handleExecute() throws Exception {
            EventQueue.invokeLater(() -> complete("FOO"));
        }

    }

}
