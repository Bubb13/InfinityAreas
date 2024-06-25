
package com.github.bubb13.infinityareas;

import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.scene.PrimaryScene;
import com.github.bubb13.infinityareas.gui.stage.GamePickerStage;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainJavaFX extends Application
{
    //////////
    // Main //
    //////////

    public static void main(final String[] args)
    {
        Platform.setImplicitExit(false);
        launch(args);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void start(final Stage primaryStage)
    {
        GlobalState.setApplication(this);
        GlobalState.setPrimaryStage(primaryStage);
        attemptLoadGame(resumeOrAskForGame());
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
    }

    public static void closePrimaryStageAndAskForGame()
    {
        final Stage primaryStage = GlobalState.getPrimaryStage();
        primaryStage.close();

        attemptLoadGame(onInvalidAskForGame(GlobalState.getSettingsFile().getRoot()));
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private static KeyFile resumeOrAskForGame()
    {
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        final JsonElement lastGameDirectoryElement = settingsRoot.get("lastGameDirectory");
        KeyFile keyFile = null;

        if (lastGameDirectoryElement == null)
        {
            // 'lastGameDirectory' not set, ask for game
            keyFile = showGamePicker();
        }
        else
        {
            // 'lastGameDirectory' set, try to use it
            final String lastGameDirectory = lastGameDirectoryElement.getAsString();
            Path lastGameDirectoryPath = null;
            boolean fallback = false;

            // Attempt to load `lastGameDirectoryPath`
            try
            {
                lastGameDirectoryPath = Path.of(lastGameDirectory);
            }
            catch (final Exception ignored) {}

            // Check if `lastGameDirectoryPath` is valid
            if (lastGameDirectoryPath != null && Files.isDirectory(lastGameDirectoryPath))
            {
                try
                {
                    // `lastGameDirectoryPath` is a directory, try to load chitin.key
                    keyFile = new KeyFile(lastGameDirectoryPath.resolve("chitin.key"));
                }
                catch (final Exception e)
                {
                    ErrorAlert.openAndWait("An exception occurred while loading the previously " +
                        "opened key file. You will be asked to select a new game install.", e);

                    // chitin.key was invalid, fallback
                    fallback = true;
                }
            }
            else
            {
                // `lastGameDirectoryPath` was invalid, fallback
                fallback = true;
            }

            if (fallback)
            {
                // 'lastGameDirectory' is invalid, remove 'lastGameDirectory' and ask for game
                keyFile = onInvalidAskForGame(settingsRoot);
            }
        }

        return keyFile;
    }

    private static void attemptLoadGame(final KeyFile keyFile)
    {
        if (keyFile == null)
        {
            // User exited out of the game selection dialog, do nothing
            return;
        }

        new JavaFXUtil.TaskManager(GlobalState.loadGameTask(keyFile)
            .onSucceeded(MainJavaFX::showPrimaryStage)
            .onFailed((final Throwable exception) ->
            {
                ErrorAlert.openAndWait("An exception occurred while loading game " +
                    "resources. You will be asked to select a new game install.", exception);

                attemptLoadGame(onInvalidAskForGame(GlobalState.getSettingsFile().getRoot()));
            })
        ).run();
    }

    private static KeyFile onInvalidAskForGame(final JsonObject settingsRoot)
    {
        settingsRoot.remove("lastGameDirectory");
        return showGamePicker();
    }

    private static KeyFile showGamePicker()
    {
        final GamePickerStage gamePickerStage = new GamePickerStage();
        gamePickerStage.setOnCloseRequest((ignored) -> Platform.exit());
        gamePickerStage.showAndWait();
        return gamePickerStage.getPickedKeyFile();
    }

    private static void showPrimaryStage()
    {
        // valid game selected, save the game directory under 'lastGameDirectory'
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        settingsRoot.addProperty("lastGameDirectory", GlobalState.getGame().getRoot().toString());

        final Stage primaryStage = GlobalState.getPrimaryStage();
        primaryStage.setOnCloseRequest((ignored) -> Platform.exit());

        final PrimaryScene primaryScene = new PrimaryScene(primaryStage);
        primaryScene.initMainScene();

        primaryStage.show();
    }
}
