
package com.github.bubb13.infinityareas.misc;

public abstract class TaskTracker implements TaskTrackerI
{
    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final TaskTracker DUMMY = new TaskTracker() {};

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override public void init() {}
    @Override public void updateMessage(final String message) {}
    @Override public void updateProgress(final double workDone, final double max) {}

    @Override
    public void subtask(final ThrowingRunnable<Exception> runnable) throws Exception
    {
        try
        {
            runnable.run();
        }
        finally
        {
            subtaskDone();
        }
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

    @Override
    public <T> T subtaskFunc(final ThrowingFunction<TaskTrackerI, T, Exception> function) throws Exception
    {
        try
        {
            return function.apply(this);
        }
        finally
        {
            subtaskDone();
        }
    }

    @Override public void subtaskDone() {}
    @Override public void done() {}

    @Override public void hide() {}
    @Override public void show() {}
}
