
package com.github.bubb13.infinityareas;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.Consumer;

public class JavaFXUtil
{
    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private JavaFXUtil() {}

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    @SuppressWarnings("unused")
    public static WidthHeight calculateWidthHeight(final Consumer<Pane> consumer)
    {
        final Pane dummyPane = new Pane();
        consumer.accept(dummyPane);

        // Needed for layout even though the reference is unused
        final Scene dummyScene = new Scene(dummyPane);

        // Do layout
        while (dummyPane.isNeedsLayout())
        {
            dummyPane.applyCss();
            dummyPane.layout();
        }

        final Bounds bounds = dummyPane.getBoundsInLocal();
        for (Node child : dummyPane.getChildren())
        {
            System.out.printf("[child] width: %f, height: %f\n", child.prefWidth(-1), child.prefHeight(-1));
        }

        return new WidthHeight(bounds.getWidth(), bounds.getHeight());
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class TaskManager
    {
        ///////////////////////////
        // Private Static Fields //
        ///////////////////////////

        private static final int BIND_DELAY = 200;

        ////////////////////
        // Private Fields //
        ////////////////////

        private final LoadingStage loadingStage;
        private final Stack<ManagedTask<?>> trackedTaskStack = new Stack<>();
        private ManagedTask<?> currentlyTrackedTask;
        private final Object bindLock = new Object();

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TaskManager(final ManagedTask<?> task)
        {
            this.loadingStage = new LoadingStage(task);
            this.trackTask(task);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        // JavaFX / other thread
        public void run()
        {
            this.attemptBindAfterDelay(this.currentlyTrackedTask);

            if (Platform.isFxApplicationThread())
            {
                // Currently on the JavaFX thread, run the task asynchronously

                // Register callbacks for asynchronous handling [setOnSucceeded(), setOnFailed(), setOnCancelled()]
                this.currentlyTrackedTask.registerAsynchronousCallbacks();

                // Start asynchronous thread
                final Thread thread = new Thread(this.currentlyTrackedTask);
                thread.setDaemon(true);
                thread.start();
            }
            else
            {
                // Not currently on the JavaFX thread, run the task synchronously
                this.currentlyTrackedTask.run();
            }
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        // JavaFX / other thread
        private void trackTask(final ManagedTask<?> task)
        {
            synchronized (this.bindLock)
            {
                // If a task is already being tracked, save it to the stack
                if (this.currentlyTrackedTask != null)
                {
                    this.trackedTaskStack.push(this.currentlyTrackedTask);
                }
                // Mark task as being tracked
                this.currentlyTrackedTask = task;
                task.setManager(this);
            }
        }

        private void attemptBindAfterDelay(final ManagedTask<?> task)
        {
            // Call `bindToTask(task)` after `BIND_DELAY` milliseconds
            new Timeline(new KeyFrame(Duration.millis(BIND_DELAY), event -> doBindToTask(task))).play();
        }

        // JavaFX / other thread
        private void bindToTask(final ManagedTask<?> task)
        {
            if (Platform.isFxApplicationThread())
            {
                // Currently on the JavaFX thread, bind the stage immediately
                this.loadingStage.bindToTask(task);
            }
            else
            {
                // Not currently on the JavaFX thread, bind the stage sometime later
                Platform.runLater(() -> doBindToTask(task));
            }
        }

        // JavaFX thread
        private void doBindToTask(final ManagedTask<?> task)
        {
            synchronized (this.bindLock)
            {
                if (this.currentlyTrackedTask == task)
                {
                    // The currently tracked task is still the requested task-to-be-bound, bind the stage
                    this.loadingStage.bindToTask(task);

                    // Show the stage if it isn't already open
                    if (!loadingStage.isShowing())
                    {
                        this.loadingStage.show();
                    }
                }
                else if (this.currentlyTrackedTask != null && !loadingStage.isShowing())
                {
                    // The task-to-be-bound isn't the currently tracked task anymore, but some other task
                    // is currently being tracked without the stage being shown. Bind it.
                    this.loadingStage.bindToTask(this.currentlyTrackedTask);
                    this.loadingStage.show();
                }
            }
        }

        // JavaFX / other thread
        private void untrackTask()
        {
            synchronized (this.bindLock)
            {
                this.currentlyTrackedTask.setManager(null);
                if (this.trackedTaskStack.isEmpty())
                {
                    // All tasks done, close the stage
                    this.closeStage();
                    this.currentlyTrackedTask = null;
                }
                else
                {
                    this.currentlyTrackedTask = this.trackedTaskStack.pop();

                    // If the stage is currently showing, bind the newly tracked task to it
                    if (this.loadingStage.isShowing())
                    {
                        this.bindToTask(this.currentlyTrackedTask);
                    }
                }
            }
        }

        // JavaFX / other thread
        private void closeStage()
        {
            if (Platform.isFxApplicationThread())
            {
                this.loadingStage.close();
            }
            else
            {
                Platform.runLater(this::doCloseStage);
            }
        }

        // JavaFX thread
        private void doCloseStage()
        {
            synchronized (this.bindLock)
            {
                // If there is still no task being tracked, close the stage
                if (this.currentlyTrackedTask == null)
                {
                    this.loadingStage.close();
                }
            }
        }

        ////////////////////
        // Public Classes //
        ////////////////////

        public abstract static class ManagedTask<T> extends Task<T>
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            private TaskManager manager;
            private final ArrayList<Consumer<T>> customOnSucceeded = new ArrayList<>();
            private final ArrayList<Consumer<Throwable>> customOnFailed = new ArrayList<>();
            private final ArrayList<Runnable> customOnCancelled = new ArrayList<>();

            ////////////////////
            // Public Methods //
            ////////////////////

            public ManagedTask<T> onSucceeded(final Consumer<T> callback)
            {
                this.customOnSucceeded.add(callback);
                return this;
            }

            public ManagedTask<T> onSucceeded(final Runnable callback)
            {
                this.customOnSucceeded.add((ignored) -> callback.run());
                return this;
            }

            public ManagedTask<T> onFailed(final Consumer<Throwable> callback)
            {
                this.customOnFailed.add(callback);
                return this;
            }

            public ManagedTask<T> onCancelled(final Runnable callback)
            {
                this.customOnCancelled.add(callback);
                return this;
            }

            ///////////////////////
            // Protected Methods //
            ///////////////////////

            /**
             * Synchronous
             */
            protected final <U> U subtask(final ManagedTask<U> subTask) throws Exception
            {
                // Subtasks are synchronous; they cannot be run from the JavaFX thread (as that would block the GUI)
                if (Platform.isFxApplicationThread())
                {
                    throw new IllegalStateException("Cannot run subtask on JavaFX application thread");
                }

                if (this.manager != null)
                {
                    // This task is being managed, track + run it through the manager
                    this.manager.trackTask(subTask);
                    this.manager.run();
                }
                else
                {
                    // This task is unmanaged, run it directly
                    subTask.run();
                }

                // Synchronous subtask is done, check if an exception was thrown
                switch (subTask.state())
                {
                    case RUNNING ->
                    {
                        throw new IllegalStateException("Synchronous task cannot still be running after run()");
                    }
                    case SUCCESS ->
                    {
                        // Task completed successfully, run callbacks and return the task's result
                        subTask.doOnSucceeded();
                        return subTask.resultNow();
                    }
                    case FAILED ->
                    {
                        // Task failed, run callbacks and rethrow the exception
                        subTask.doOnFailed();
                        throw new Exception(subTask.exceptionNow());
                    }
                    case CANCELLED ->
                    {
                        // Task cancelled, run callbacks
                        subTask.doOnCancelled();
                    }
                }

                // Cancelled
                return null;
            }

            /////////////////////
            // Private Methods //
            /////////////////////

            private void setManager(final TaskManager manager)
            {
                this.manager = manager;
            }

            private void registerAsynchronousCallbacks()
            {
                this.setOnSucceeded((ignored) -> this.doOnSucceeded());
                this.setOnFailed((ignored) -> this.doOnFailed());
                this.setOnCancelled((ignored) -> this.doOnCancelled());
            }

            // JavaFX / other thread
            private void doOnSucceeded()
            {
                this.attemptUntrack();
                for (final Consumer<T> callback : this.customOnSucceeded)
                {
                    callback.accept(this.getValue());
                }
            }

            // JavaFX / other thread
            private void doOnFailed()
            {
                this.attemptUntrack();
                for (final Consumer<Throwable> callback : this.customOnFailed)
                {
                    callback.accept(this.getException());
                }
            }

            // JavaFX / other thread
            private void doOnCancelled()
            {
                this.attemptUntrack();
                for (final Runnable callback : this.customOnCancelled)
                {
                    callback.run();
                }
            }

            // JavaFX / other thread
            private void attemptUntrack()
            {
                if (this.manager != null)
                {
                    this.manager.untrackTask();
                }
            }
        }
    }

    public record WidthHeight(double width, double height) {}
}
