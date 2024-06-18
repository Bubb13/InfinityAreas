
package com.github.bubb13.infinityareas;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Application;
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
        launch(args);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void start(final Stage primaryStage)
    {
        GlobalState.setPrimaryStage(primaryStage);
        attemptLoadGame(resumeOrAskForGame());
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private KeyFile resumeOrAskForGame()
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
                    new ErrorAlert("An exception occurred while loading the previously opened key file. " +
                        "You will be asked to select a new game install.\n\n" + MiscUtil.formatStackTrace(e)
                    ).showAndWait();

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

        if (keyFile != null)
        {
            // valid chitin.key selected, save the game directory under 'lastGameDirectory'
            settingsRoot.addProperty("lastGameDirectory", keyFile.getPath().getParent().toString());
        }
        return keyFile;
    }

    private void attemptLoadGame(final KeyFile keyFile)
    {
        if (keyFile == null)
        {
            // User exited out of the game selection dialog, do nothing
            return;
        }

        new JavaFXUtil.TaskManager(GlobalState.loadGameTask(keyFile)
            .onSucceeded(this::showPrimaryStage)
            .onFailed((final Throwable exception) ->
            {
                new ErrorAlert("An exception occurred while loading game resources. You will " +
                    "be asked to select a new game install.\n\n" + MiscUtil.formatStackTrace(exception)
                ).showAndWait();

                attemptLoadGame(onInvalidAskForGame(GlobalState.getSettingsFile().getRoot()));
            })
        ).run();
    }

    private KeyFile onInvalidAskForGame(final JsonObject settingsRoot)
    {
        settingsRoot.remove("lastGameDirectory");
        return showGamePicker();
    }

    private KeyFile showGamePicker()
    {
        return new GamePickerStage().getPickedKeyFile();
    }

    private void showPrimaryStage()
    {
        final Stage primaryStage = GlobalState.getPrimaryStage();

        final PrimaryScene primaryScene = new PrimaryScene(primaryStage);
        primaryScene.initMainScene();

        primaryStage.show();
    }
}
