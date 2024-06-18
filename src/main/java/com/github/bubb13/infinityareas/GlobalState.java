
package com.github.bubb13.infinityareas;

import javafx.stage.Stage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class GlobalState
{
    private static Path infinityAreasRoot;
    private static SettingsFile settingsFile;
    private static Game game;
    private static Stage primaryStage;

    public static void init() throws URISyntaxException, IOException
    {
        infinityAreasRoot = MiscUtil.findInfinityAreasRoot();
        settingsFile = new SettingsFile(GlobalState.getInfinityAreasRoot().resolve("settings.json"));
    }

    public static Path getInfinityAreasRoot()
    {
        return infinityAreasRoot;
    }

    public static SettingsFile getSettingsFile()
    {
        return settingsFile;
    }

    public static Game getGame()
    {
        return game;
    }

    public static Stage getPrimaryStage()
    {
        return primaryStage;
    }

    public static void setPrimaryStage(Stage primaryStage)
    {
        GlobalState.primaryStage = primaryStage;
    }

    public static JavaFXUtil.TaskManager.ManagedTask<Void> loadGameTask(final KeyFile keyFile)
    {
        final Game game = new Game(keyFile);
        return game.loadResourcesTask().onSucceeded(() -> GlobalState.game = game);
    }

    private GlobalState() {}
}
