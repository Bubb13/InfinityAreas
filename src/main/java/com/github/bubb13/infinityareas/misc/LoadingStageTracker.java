
package com.github.bubb13.infinityareas.misc;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.gui.stage.LoadingStage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

public class LoadingStageTracker extends StackTaskTracker
{
    private final Object loadingStageInitLock = new Object();

    private boolean loadingStageInitQueued;
    private LoadingStage loadingStage;

    @Override
    public void init()
    {
        GlobalState.pushModalStage(null);
        loadingStageInitQueued = true;

        // Open the loading stage if the task takes more than 300 milliseconds
        final Timeline timeline = new Timeline(new KeyFrame(
            Duration.millis(300),
            event -> initStage()
        ));
        timeline.setCycleCount(1);
        timeline.play();
    }

    private void initStage()
    {
        synchronized (loadingStageInitLock)
        {
            if (!loadingStageInitQueued) return;
            loadingStageInitQueued = false;
            loadingStage = new LoadingStage();
            loadingStage.bind(messageProperty, progressProperty);
            loadingStage.show();
        }
    }

    @Override
    public void done()
    {
        boolean wasLoadingStageInitQueued = false;

        // Stop the loading stage from opening if it hasn't already
        synchronized (loadingStageInitLock)
        {
            if (loadingStageInitQueued)
            {
                loadingStageInitQueued = false;
                wasLoadingStageInitQueued = true;
            }
        }

        // Close the loading stage if it is open
        if (!wasLoadingStageInitQueued)
        {
            if (Platform.isFxApplicationThread())
            {
                loadingStage.close();
            }
            else
            {
                Platform.runLater(loadingStage::close);
            }
        }

        GlobalState.popModalStage(null);
    }
}
