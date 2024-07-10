
package com.github.bubb13.infinityareas.misc;

public interface TaskTrackerI
{
    void init();
    void updateMessage(String message);
    void updateProgress(double workDone, double max);
    void subtask(ThrowingRunnable<Exception> runnable) throws Exception;
    void subtask(ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception;
    void subtaskDone();
    void done();
}
