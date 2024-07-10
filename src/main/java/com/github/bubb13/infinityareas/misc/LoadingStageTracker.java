
package com.github.bubb13.infinityareas.misc;

import com.github.bubb13.infinityareas.gui.stage.LoadingStage;
import javafx.application.Platform;

public class LoadingStageTracker extends StackTaskTracker
{
    private final LoadingStage loadingStage = new LoadingStage();

    public LoadingStageTracker()
    {
        loadingStage.bind(messageProperty, progressProperty);
    }

    @Override
    public void init()
    {
        if (Platform.isFxApplicationThread())
        {
            loadingStage.show();
        }
        else
        {
            Platform.runLater(loadingStage::show);
        }
    }

    @Override
    public void done()
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
}
