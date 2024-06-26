
package com.github.bubb13.infinityareas;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.application.Application;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalState
{
    private static Path infinityAreasRoot;
    private static Path infinityAreasTemp;
    private static SettingsFile settingsFile;
    private static Game game;
    private static Application application;
    private static Stage primaryStage;

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void init() throws Exception
    {
        infinityAreasRoot = MiscUtil.findInfinityAreasRoot();
        infinityAreasTemp = infinityAreasRoot.resolve("InfinityAreasTemp");
        cleanTemp();
        settingsFile = new SettingsFile(GlobalState.getInfinityAreasRoot().resolve("settings.json"));
    }

    public static Path getInfinityAreasRoot()
    {
        return infinityAreasRoot;
    }

    public static Path getInfinityAreasTemp() throws Exception
    {
        createTemp();
        return infinityAreasTemp;
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

    public static Application getApplication()
    {
        return application;
    }

    public static void setApplication(Application application)
    {
        GlobalState.application = application;
    }

    public static JavaFXUtil.TaskManager.ManagedTask<Void> loadGameTask(final KeyFile keyFile)
    {
        final Game game = new Game(keyFile);
        return game.loadResourcesTask().onSucceeded(() -> GlobalState.game = game);
    }

    public static void cleanTemp()
    {
        if (Files.isDirectory(infinityAreasTemp))
        {
            try
            {
                FileUtil.forAllInPath(infinityAreasTemp, (final Path path) ->
                {
                    try
                    {
                        Files.delete(path);
                    }
                    catch (final Exception ignored) {}
                });
            }
            catch (final Exception ignored) {}
        }
    }

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

    private static void createTemp() throws Exception
    {
        if (!Files.isDirectory(infinityAreasTemp))
        {
            if (Files.exists(infinityAreasTemp))
            {
                throw new IllegalStateException(String.format("Path \"%s\" should be a folder", infinityAreasTemp));
            }

            Files.createDirectories(infinityAreasTemp);
        }
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private GlobalState() {}
}
