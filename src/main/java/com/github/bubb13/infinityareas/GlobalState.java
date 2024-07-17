
package com.github.bubb13.infinityareas;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
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
    private static PixelFormat.Type nativePixelFormatType;

    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void init() throws Exception
    {
        infinityAreasRoot = MiscUtil.findInfinityAreasRoot();
        infinityAreasTemp = infinityAreasRoot.resolve("InfinityAreasTemp");
        cleanTemp();
        settingsFile = new SettingsFile(GlobalState.getInfinityAreasRoot().resolve("settings.json"));
        cacheNativePixelFormatType();
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

    public static TrackedTask<Void> loadGameTask(final KeyFile keyFile)
    {
        final Game game = new Game(keyFile);
        return game.loadTask().onSucceeded(() -> GlobalState.game = game);
    }

    public static PixelFormat.Type getNativePixelFormatType()
    {
        return nativePixelFormatType;
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

    private static void cacheNativePixelFormatType()
    {
        final Image image = new WritableImage(1, 1);
        final PixelReader pixelReader = image.getPixelReader();
        nativePixelFormatType = pixelReader.getPixelFormat().getType();
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private GlobalState() {}
}
