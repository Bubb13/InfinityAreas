
package com.github.bubb13.infinityareas.misc;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.gui.stage.LoadingStage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class LoadingStageTracker extends StackTaskTracker
{
    private final Object loadingStageStateLock = new Object();

    private final Timeline delayedInitStage = new Timeline(new KeyFrame(
        Duration.millis(300),
        event -> checkInitStage()
    ));

    private boolean loadingStageState;
    private LoadingStage loadingStage;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LoadingStageTracker()
    {
        delayedInitStage.setCycleCount(1);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void init()
    {
        synchronized (loadingStageStateLock)
        {
            if (loadingStageState) return;

            loadingStageState = true;
            GlobalState.pushModalStage(null);

            // Open the loading stage if the task takes more than 300 milliseconds
            delayedInitStage.play();
        }
    }

    @Override
    public void done()
    {
        // Stop the loading stage from opening if it hasn't already
        synchronized (loadingStageStateLock)
        {
            if (!loadingStageState) return;

            loadingStageState = false;
            GlobalState.popModalStage(null);
            delayedInitStage.stop();

            if (Platform.isFxApplicationThread())
            {
                if (loadingStage != null) loadingStage.close();
            }
            else
            {
                Platform.runLater(this::checkCloseStage);
            }
        }
    }

    @Override
    public void hide()
    {
        done();
    }

    @Override
    public void show()
    {
        init();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void checkInitStage()
    {
        synchronized (loadingStageStateLock)
        {
            if (!loadingStageState) return;

            if (loadingStage == null)
            {
                loadingStage = new LoadingStage();
                loadingStage.bind(messageProperty, progressProperty);
            }

            loadingStage.show();
        }
    }

    private void checkCloseStage()
    {
        synchronized (loadingStageStateLock)
        {
            if (loadingStageState) return;
            if (loadingStage != null) loadingStage.close();
        }
    }
}
