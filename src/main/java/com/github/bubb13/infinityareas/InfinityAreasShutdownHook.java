
package com.github.bubb13.infinityareas;

import java.io.IOException;

public class InfinityAreasShutdownHook extends Thread
{
    @Override
    public void run()
    {
        final SettingsFile settingsFile = GlobalState.getSettingsFile();

        if (settingsFile != null)
        {
            try
            {
                settingsFile.writeToDisk();
            }
            catch (final IOException ignored) {}
        }
    }
}
