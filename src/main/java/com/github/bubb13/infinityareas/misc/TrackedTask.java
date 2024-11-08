
package com.github.bubb13.infinityareas.misc;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Phaser;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class TrackedTask<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final LinkedList<TrackedTask<T>> toDoQueue = new LinkedList<>();

    private final ArrayList<Function<T, TrackedTask<?>>> chainOnSuccess = new ArrayList<>();
    private final ArrayList<BiFunction<TaskTrackerI, Throwable, TrackedTask<T>>> chainOnFail = new ArrayList<>();

    private final ArrayList<Consumer<T>> onSucceeded = new ArrayList<>();
    private final ArrayList<Consumer<T>> onSucceededFx = new ArrayList<>();

    private final ArrayList<Consumer<Throwable>> onFailed = new ArrayList<>();
    private final ArrayList<Consumer<Throwable>> onFailedFx = new ArrayList<>();

    private final ArrayList<Runnable> onCancelled = new ArrayList<>();
    private final ArrayList<Runnable> onCancelledFx = new ArrayList<>();

    private final Phaser waitLatch = new Phaser(1);

    private TaskTrackerI tracker = TaskTracker.DUMMY;
    private Thread thread;
    private T value;
    private Exception exception;

    ////////////////////
    // Public Methods //
    ////////////////////

    /**
     * Tracks this task with {@code tracker}.
     * @param tracker The {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} to track this task with.
     * @return {@code this}
     */
    public TrackedTask<T> trackWith(final TaskTrackerI tracker)
    {
        this.tracker = tracker;
        return this;
    }

    /**
     * Registers {@code chain} to be run when the task succeeds (ends without an exception).
     * {@code chain} is called by the task's thread.
     * @param chain The task to chain.
     * @return {@code this}
     */
    public TrackedTask<T> chainOnSuccess(final Function<T, TrackedTask<?>> chain)
    {
        this.chainOnSuccess.add(chain);
        return this;
    }

    /**
     * Registers {@code chain} to be run when the task fails (ends with an exception).
     * {@code chain} is called by the task's thread.
     *
     * <br><br>
     * <b>Note:</b> If a failure chain
     * is executed, the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onFailed}
     * / {@link com.github.bubb13.infinityareas.misc.TrackedTask#onFailedFx} will not be executed, as it is assumed
     * that the failure chain will handle the exception.
     *
     * @param chain The task to chain.
     * @return {@code this}
     */
    public TrackedTask<T> chainOnFail(final BiFunction<TaskTrackerI, Throwable, TrackedTask<T>> chain)
    {
        this.chainOnFail.add(chain);
        return this;
    }

    /**
     * Registers {@code consumer} to be called when the task succeeds (ends without an exception).
     * {@code consumer} is called by the task's thread.
     * @param consumer The consumer.
     * @return {@code this}
     */
    public TrackedTask<T> onSucceeded(final Consumer<T> consumer)
    {
        this.onSucceeded.add(consumer);
        return this;
    }

    /**
     * Registers {@code callback} to be called when the task succeeds (ends without an exception).
     * {@code callback} is called by the task's thread.
     * @param callback The callback.
     * @return {@code this}
     */
    public TrackedTask<T> onSucceeded(final Runnable callback)
    {
        this.onSucceeded.add((ignored) -> callback.run());
        return this;
    }

    /**
     * Registers {@code consumer} to be called when the task succeeds (ends without an exception).
     * {@code consumer} is scheduled to run on the JavaFX thread.
     * @param consumer The consumer.
     * @return {@code this}
     */
    public TrackedTask<T> onSucceededFx(final Consumer<T> consumer)
    {
        this.onSucceededFx.add(consumer);
        return this;
    }

    /**
     * Registers {@code callback} to be called when the task succeeds (ends without an exception).
     * {@code callback} is scheduled to run on the JavaFX thread.
     * @param callback The callback.
     * @return {@code this}
     */
    public TrackedTask<T> onSucceededFx(final Runnable callback)
    {
        this.onSucceededFx.add((ignored) -> callback.run());
        return this;
    }

    /**
     * Registers {@code consumer} to be called when the task fails (ends with an exception).
     * {@code consumer} is called by the task's thread.
     * @param consumer The consumer.
     * @return {@code this}
     */
    public TrackedTask<T> onFailed(final Consumer<Throwable> consumer)
    {
        this.onFailed.add(consumer);
        return this;
    }

    /**
     * Registers {@code consumer} to be called when the task fails (ends with an exception).
     * {@code consumer} is scheduled to run on the JavaFX thread.
     * @param consumer The consumer.
     * @return {@code this}
     */
    public TrackedTask<T> onFailedFx(final Consumer<Throwable> consumer)
    {
        this.onFailedFx.add(consumer);
        return this;
    }

    /**
     * Registers {@code callback} to be called when the task is cancelled.
     * {@code callback} is called by the task's thread.
     * @param callback The callback.
     * @return {@code this}
     */
    public TrackedTask<T> onCancelled(final Runnable callback)
    {
        this.onCancelled.add(callback);
        return this;
    }

    /**
     * Registers {@code callback} to be called when the task is cancelled.
     * {@code callback} is scheduled to run on the JavaFX thread.
     * @param callback The callback.
     * @return {@code this}
     */
    public TrackedTask<T> onCancelledFx(final Runnable callback)
    {
        this.onCancelledFx.add(callback);
        return this;
    }

    /**
     * Starts the task by initializing the task's tracker (if any) and starting the task's thread.
     */
    public void start()
    {
        tracker.init();
        // Start asynchronous thread
        thread = new Thread(this::runThread);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Executes any registered "cancelled" callbacks and interrupts the task's thread. Tasks should only be
     * cancelled if they were specifically designed to handle an interrupt.
     */
    public void cancel()
    {
        doOnCancelled();
        thread.interrupt();
    }

    /**
     * @return The {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} tracking the task.
     */
    public TaskTrackerI getTracker()
    {
        return tracker;
    }

    /**
     * @return The value returned by the task's {@link com.github.bubb13.infinityareas.misc.TrackedTask#doTask} method,
     * or {@code null} if the task is not yet completed / failed.
     */
    public T getValue()
    {
        return value;
    }

    /**
     * @return The exception thrown by the task's {@link com.github.bubb13.infinityareas.misc.TrackedTask#doTask}
     * method, or {@code null} if the task is not yet completed / succeeded.
     */
    public Exception getException()
    {
        return exception;
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    /**
     * The main method of the task - does the work, and returns the task's result.
     */
    protected T doTask(final TaskTrackerI tracker) throws Exception
    {
        return null;
    }

    /**
     * Executes {@code runnable} as a subtask of the current task.
     * @param runnable The subtask.
     * @throws Exception The exception thrown by {@code runnable}.
     */
    protected void untrackedSubtask(final ThrowingRunnable<Exception> runnable) throws Exception
    {
        tracker.subtask(runnable);
    }

    /**
     * Executes {@code consumer} as a subtask of the current task.
     * @param consumer The subtask.
     * @throws Exception The exception thrown by {@code consumer}.
     */
    protected void subtask(final ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception
    {
        tracker.subtask(consumer);
    }

    /**
     * Executes {@code trackedTask} as a subtask of the current task.
     * @param trackedTask The subtask.
     * @throws Exception The exception thrown by {@code trackedTask}.
     */
    protected <SubtaskReturnType> SubtaskReturnType subtask(
        final TrackedTask<SubtaskReturnType> trackedTask) throws Exception
    {
        tracker.subtask(() -> trackedTask.trackWith(tracker).runChain());

        if (trackedTask.exception == null)
        {
            return trackedTask.value;
        }
        else
        {
            throw trackedTask.exception;
        }
    }

    /**
     * Executes {@code function} as a subtask of the current task.
     *
     * @param function The function.
     * @return The value returned by {@code function}.
     * @param <SubtaskResultT> The type returned by {@code function}.
     * @throws Exception The exception thrown by {@code function}.
     */
    protected <SubtaskResultT> SubtaskResultT subtaskFunc(
        final ThrowingFunction<TaskTrackerI, SubtaskResultT, Exception> function) throws Exception
    {
        return tracker.subtaskFunc(function);
    }

    /**
     * Schedules {@code runnable} to run on the JavaFX thread and yields the task's thread until {@code runnable}
     * has been executed.
     *
     * @param runnable The runnable.
     */
    protected void waitForFxThreadToExecute(final Runnable runnable)
    {
        Platform.runLater(() ->
        {
            runnable.run();
            waitLatch.arrive();
        });

        waitLatch.awaitAdvance(waitLatch.getPhase());
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    /**
     * Thread-entry for the task.
     */
    private void runThread()
    {
        toDoQueue.add(this);
        loop(false);
    }

    /**
     * Runs the current task as a chain, i.e. the tracker isn't automatically closed on task end.
     */
    private void runChain()
    {
        toDoQueue.add(this);
        loop(true);
    }

    /**
     * Runs the task by calling {@link com.github.bubb13.infinityareas.misc.TrackedTask#doTask} and handling
     * any relevant callbacks.
     */
    private void loop(final boolean isChain)
    {
        final TrackedTask<T> toDoTask = toDoQueue.removeFirst();

        do
        {
            try
            {
                value = toDoTask.doTask(tracker);
                exception = null;
            }
            catch (final Exception e)
            {
                value = null;
                exception = e;
            }
            finally
            {
                boolean ignoreFailed = false;

                if (exception == null)
                {
                    doChainOnSuccess();
                }
                else
                {
                    ignoreFailed = doChainOnFail(toDoQueue);
                }

                if (exception == null)
                {
                    doOnSucceeded();
                }
                else if (!ignoreFailed)
                {
                    doOnFailed();
                }
            }
        }
        while (!toDoTask.toDoQueue.isEmpty());

        if (!isChain)
        {
            tracker.done();
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#chainOnSuccess}.
     */
    private void doChainOnSuccess()
    {
        for (final Function<T, TrackedTask<?>> chain : chainOnSuccess)
        {
            final TrackedTask<?> chainedTask = chain.apply(value);
            if (chainedTask != null)
            {
                chainedTask.trackWith(tracker).runChain();
            }
        }
    }

    /**
     * Schedules the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#chainOnFail}
     * to run on the current task's thread.
     *
     * @return {@code true} if a chain was registered, {@code false} otherwise.
     */
    private boolean doChainOnFail(final Queue<TrackedTask<T>> toDoQueue)
    {
        boolean ranChain = false;

        for (final BiFunction<TaskTrackerI, Throwable, TrackedTask<T>> chain : chainOnFail)
        {
            final TrackedTask<T> chainedTask = chain.apply(tracker, exception);
            if (chainedTask != null)
            {
                ranChain = true;
                toDoQueue.add(chainedTask.trackWith(tracker));
            }
        }

        return ranChain;
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onSucceeded}
     * and schedules the callbacks registered
     * by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onSucceededFx}.
     */
    private void doOnSucceeded()
    {
        for (final Consumer<T> callback : onSucceeded)
        {
            callback.accept(value);
        }

        if (!onSucceededFx.isEmpty())
        {
            Platform.runLater(this::doOnSucceededFx);
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onSucceededFx}.
     */
    private void doOnSucceededFx()
    {
        for (final Consumer<T> callback : onSucceededFx)
        {
            callback.accept(value);
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onFailed}.
     * and schedules the callbacks registered
     * by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onFailedFx}.
     */
    private void doOnFailed()
    {
        for (final Consumer<Throwable> consumer : onFailed)
        {
            consumer.accept(exception);
        }

        if (!onFailedFx.isEmpty())
        {
            Platform.runLater(this::doOnFailedFx);
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onFailedFx}.
     */
    private void doOnFailedFx()
    {
        for (final Consumer<Throwable> callback : onFailedFx)
        {
            callback.accept(exception);
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onCancelled}.
     * and schedules the callbacks registered
     * by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onCancelledFx}.
     */
    private void doOnCancelled()
    {
        for (final Runnable callback : onCancelled)
        {
            callback.run();
        }

        if (!onCancelledFx.isEmpty())
        {
            Platform.runLater(this::doOnCancelledFx);
        }
    }

    /**
     * Runs the callbacks registered by {@link com.github.bubb13.infinityareas.misc.TrackedTask#onCancelledFx}.
     */
    private void doOnCancelledFx()
    {
        for (final Runnable callback : onCancelledFx)
        {
            callback.run();
        }
    }
}
