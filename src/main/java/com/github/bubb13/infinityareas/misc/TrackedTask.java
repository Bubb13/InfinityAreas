
package com.github.bubb13.infinityareas.misc;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.concurrent.Phaser;
import java.util.function.Consumer;

public abstract class TrackedTask<T>
{
    ////////////////////
    // Private Fields //
    ////////////////////

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

    public TrackedTask<T> trackWith(final TaskTrackerI tracker)
    {
        this.tracker = tracker;
        return this;
    }

    public TrackedTask<T> onSucceeded(final Consumer<T> callback)
    {
        this.onSucceeded.add(callback);
        return this;
    }

    public TrackedTask<T> onSucceeded(final Runnable callback)
    {
        this.onSucceeded.add((ignored) -> callback.run());
        return this;
    }

    public TrackedTask<T> onSucceededFx(final Consumer<T> callback)
    {
        this.onSucceededFx.add(callback);
        return this;
    }

    public TrackedTask<T> onSucceededFx(final Runnable callback)
    {
        this.onSucceededFx.add((ignored) -> callback.run());
        return this;
    }

    public TrackedTask<T> onFailed(final Consumer<Throwable> callback)
    {
        this.onFailed.add(callback);
        return this;
    }

    public TrackedTask<T> onFailedFx(final Consumer<Throwable> callback)
    {
        this.onFailedFx.add(callback);
        return this;
    }

    public TrackedTask<T> onCancelled(final Runnable callback)
    {
        this.onCancelled.add(callback);
        return this;
    }

    public TrackedTask<T> onCancelledFx(final Runnable callback)
    {
        this.onCancelledFx.add(callback);
        return this;
    }

    public void start()
    {
        tracker.init();
        // Start asynchronous thread
        thread = new Thread(this::run);
        thread.setDaemon(true);
        thread.start();
    }

    public void subtask(final ThrowingRunnable<Exception> runnable) throws Exception
    {
        tracker.subtask(runnable);
    }

    public void subtask(final ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception
    {
        tracker.subtask(consumer);
    }

    public void waitForFxThreadToExecute(final Runnable runnable)
    {
        Platform.runLater(() ->
        {
            runnable.run();
            waitLatch.arrive();
        });

        waitLatch.awaitAdvance(waitLatch.getPhase());
    }

    public void cancel()
    {
        doOnCancelled();
        thread.interrupt();
    }

    public TaskTrackerI getTracker()
    {
        return tracker;
    }

    public T getValue()
    {
        return value;
    }

    public Exception getException()
    {
        return exception;
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    protected T doTask() throws Exception
    {
        return null;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void run()
    {
        try
        {
            value = doTask();
        }
        catch (final Exception e)
        {
            exception = e;
        }
        finally
        {
            tracker.done();

            if (exception == null)
            {
                doOnSucceeded();
            }
            else
            {
                doOnFailed();
            }
        }
    }

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

    private void doOnSucceededFx()
    {
        for (final Consumer<T> callback : onSucceededFx)
        {
            callback.accept(value);
        }
    }

    private void doOnFailed()
    {
        for (final Consumer<Throwable> callback : onFailed)
        {
            callback.accept(exception);
        }

        if (!onFailedFx.isEmpty())
        {
            Platform.runLater(this::doOnFailedFx);
        }
    }

    private void doOnFailedFx()
    {
        for (final Consumer<Throwable> callback : onFailedFx)
        {
            callback.accept(exception);
        }
    }

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

    private void doOnCancelledFx()
    {
        for (final Runnable callback : onCancelledFx)
        {
            callback.run();
        }
    }
}
