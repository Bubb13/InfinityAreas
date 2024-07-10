
package com.github.bubb13.infinityareas.misc;

public abstract class TaskTracker implements TaskTrackerI
{
    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final TaskTracker DUMMY = new TaskTracker() {};

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void subtask(final TaskTrackerI tracker, final ThrowingRunnable<Exception> runnable) throws Exception
    {
        try
        {
            runnable.run();
        }
        finally
        {
            tracker.subtaskDone();
        }
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override public void init() {}
    @Override public void updateMessage(final String message) {}
    @Override public void updateProgress(final double workDone, final double max) {}

    @Override
    public void subtask(final ThrowingRunnable<Exception> runnable) throws Exception
    {
        TaskTracker.subtask(this, runnable);
    }

    @Override
    public void subtask(final ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception
    {
        try
        {
            consumer.accept(this);
        }
        finally
        {
            subtaskDone();
        }
    }

    @Override public void subtaskDone() {}
    @Override public void done() {}
}
