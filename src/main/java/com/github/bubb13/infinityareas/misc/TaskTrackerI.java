
package com.github.bubb13.infinityareas.misc;

public interface TaskTrackerI
{
    /**
     * Called when the task's thread is started.
     */
    void init();

    /**
     * Updates the message corresponding to the task's status. This may or may not be shown to the user
     * depending on the specific {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclass.
     *
     * @param message The message to set.
     */
    void updateMessage(String message);

    /**
     * Updates the progress corresponding to the task's status. This may or may not be shown to the user
     * depending on the specific {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclass.
     *
     * @param workDone The current amount of work completed by the task.
     * @param max The maximum amount of work expected of the task.
     */
    void updateProgress(double workDone, double max);

    /**
     * Executes {@code runnable} as a subtask of the current task. While implementation differences exist
     * between {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclasses, the general expectation
     * is that the act of executing a subtask involves pushing/popping any relevant state (such as the
     * current message / progress) of the parent task before/after the subtask's execution.
     *
     * @param runnable The subtask to execute.
     * @throws Exception The exception thrown by {@code runnable}.
     */
    void subtask(ThrowingRunnable<Exception> runnable) throws Exception;

    /**
     * Executes {@code consumer} as a subtask of the current task. While implementation differences exist
     * between {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclasses, the general expectation
     * is that the act of executing a subtask involves pushing/popping any relevant state (such as the
     * current message / progress) of the parent task before/after the subtask's execution.
     *
     * @param consumer The subtask to execute.
     * @throws Exception The exception thrown by {@code consumer}.
     */
    void subtask(ThrowingConsumer<TaskTrackerI, Exception> consumer) throws Exception;

    /**
     * Executes {@code function} as a subtask of the current task. While implementation differences exist
     * between {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclasses, the general expectation
     * is that the act of executing a subtask involves pushing/popping any relevant state (such as the
     * current message / progress) of the parent task before/after the subtask's execution.
     *
     * @param function The subtask to execute.
     * @return The value returned by {@code function}.
     * @param <T> The return type of {@code function}.
     * @throws Exception The exception thrown by {@code function}.
     */
    <T> T subtaskFunc(ThrowingFunction<TaskTrackerI, T, Exception> function) throws Exception;

    /**
     * Called when a subtask ends, either via its natural completion or via an {@link java.lang.Exception}
     * being thrown by the subtask.
     */
    void subtaskDone();

    /**
     * Called when the task ends, either via its natural completion or via an {@link java.lang.Exception}
     * being thrown.
     */
    void done();

    /**
     * Hides the tracker if the given {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclass blocks
     * user interaction.
     */
    void hide();

    /**
     * Shows the tracker if the given {@link com.github.bubb13.infinityareas.misc.TaskTrackerI} subclass blocks
     * user interaction.
     */
    void show();
}
