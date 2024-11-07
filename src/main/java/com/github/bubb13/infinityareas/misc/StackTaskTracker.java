
package com.github.bubb13.infinityareas.misc;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Stack;

public class StackTaskTracker extends TaskTracker
{
    private final Stack<SavedProgress> savedProgressStack = new Stack<>();
    protected final StringProperty messageProperty;
    protected final DoubleProperty progressProperty;
    final private Object updateLock = new Object();

    private String pendingMessage;
    private double pendingProgress;
    private boolean updateQueued;

    public StackTaskTracker()
    {
        this.messageProperty = new SimpleStringProperty("");
        this.progressProperty = new SimpleDoubleProperty();
    }

    @Override
    public void subtask(final ThrowingRunnable<Exception> runnable) throws Exception
    {
        savedProgressStack.push(new SavedProgress(messageProperty.get(), progressProperty.get()));
        super.subtask(runnable);
    }

    @Override
    public void subtask(final ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception
    {
        savedProgressStack.push(new SavedProgress(messageProperty.get(), progressProperty.get()));
        super.subtask(consumer);
    }

    @Override
    public <T> T subtaskFunc(final ThrowingFunction<TaskTrackerI, T, Exception> function) throws Exception
    {
        savedProgressStack.push(new SavedProgress(messageProperty.get(), progressProperty.get()));
        return super.subtaskFunc(function);
    }

    @Override
    public void subtaskDone()
    {
        final SavedProgress savedProgress = savedProgressStack.pop();
        updateMessageAndProgress(savedProgress.message(), savedProgress.progress());
    }

    @Override
    public void updateMessage(final String message)
    {
        if (Platform.isFxApplicationThread())
        {
            messageProperty.set(message);
        }
        else
        {
            synchronized (updateLock)
            {
                pendingMessage = message;
                queueUpdate();
            }
        }
    }

    @Override
    public void updateProgress(final double workDone, final double max)
    {
        final double newProgress = workDone / max;
        if (Platform.isFxApplicationThread())
        {
            progressProperty.set(newProgress);
        }
        else
        {
            synchronized (updateLock)
            {
                pendingProgress = newProgress;
                queueUpdate();
            }
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void updateMessageAndProgress(final String message, final double progress)
    {
        if (Platform.isFxApplicationThread())
        {
            messageProperty.set(message);
            progressProperty.set(progress);
        }
        else
        {
            synchronized (updateLock)
            {
                pendingMessage = message;
                pendingProgress = progress;
                queueUpdate();
            }
        }
    }

    // Important: Assumes already synchronized on updateLock
    private void queueUpdate()
    {
        if (!updateQueued)
        {
            Platform.runLater(this::update);
        }
    }

    private void update()
    {
        synchronized (updateLock)
        {
            if (!messageProperty.get().equals(pendingMessage))
            {
                messageProperty.set(pendingMessage);
            }

            if (progressProperty.get() != pendingProgress)
            {
                progressProperty.set(pendingProgress);
            }

            updateQueued = false;
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record SavedProgress(String message, double progress) {}
}
