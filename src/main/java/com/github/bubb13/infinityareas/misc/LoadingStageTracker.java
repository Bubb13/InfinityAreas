
package com.github.bubb13.infinityareas.misc;

import com.github.bubb13.infinityareas.gui.stage.LoadingStage;
import javafx.application.Platform;

public class LoadingStageTracker extends StackTaskTracker
{
    private final Object loadingStageInitLock = new Object();

    private boolean loadingStageInitQueued;
    private LoadingStage loadingStage;

    @Override
    public void init()
    {
        loadingStageInitQueued = true;
        if (Platform.isFxApplicationThread())
        {
            initStage();
        }
        else
        {
            Platform.runLater(this::initStage);
        }
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
        synchronized (loadingStageInitLock)
        {
            if (loadingStageInitQueued)
            {
                loadingStageInitQueued = false;
                return;
            }
        }

        if (Platform.isFxApplicationThread())
        {
            loadingStage.close();
        }
        else
        {
            Platform.runLater(loadingStage::close);
        }
    }
}
