
package com.github.bubb13.infinityareas;

public class Main
{
    public static void main(final String[] args)
    {
        try
        {
            // Last chance shutdown hook
            Runtime.getRuntime().addShutdownHook(new InfinityAreasShutdownHook());

            // Init global state
            GlobalState.init();

            // Start JavaFX application; blocks until the primary stage is closed
            MainJavaFX.main(new String[]{});
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
