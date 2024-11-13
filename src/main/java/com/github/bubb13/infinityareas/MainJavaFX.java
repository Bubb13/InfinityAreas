
package com.github.bubb13.infinityareas;

import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.scene.PrimaryScene;
import com.github.bubb13.infinityareas.gui.stage.GamePickerStage;
import com.github.bubb13.infinityareas.misc.tasktracking.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.tasktracking.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.tasktracking.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.SettingsUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.nio.file.Files;
import java.nio.file.Path;

public class MainJavaFX extends Application
{
    // Buffers used to save the x and y values from before a maximize operation
    private static final int[] bufferedX = new int[2];
    private static final int[] bufferedY = new int[2];

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
        GlobalState.createRobot();
        resumeOrAskForGame();
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

        GlobalState.cleanTemp();
        runAttemptLoadGameTask(onInvalidAskForGame(GlobalState.getSettingsFile().getRoot()));
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    //-------//
    // Tasks //
    //-------//

    // Loops until a game has been successfully loaded
    private static TrackedTask<Void> attemptLoadGameTask(final KeyFile keyFile)
    {
        if (keyFile == null)
        {
            // User exited out of the game selection dialog, do nothing
            return null;
        }

        return GlobalState.loadGameTask(keyFile)
            .chainOnFail((final TaskTrackerI tracker, final Throwable exception) ->
            {
                tracker.hide();
                final KeyFile retryKeyFile = JavaFXUtil.waitForFxThreadToExecute(() ->
                {
                    ErrorAlert.openAndWait("An exception occurred while loading game " +
                        "resources. You will be asked to select a new game install.", exception);

                    return onInvalidAskForGame(GlobalState.getSettingsFile().getRoot());
                });
                tracker.show();

                return attemptLoadGameTask(retryKeyFile);
            })
            .onSucceededFx(MainJavaFX::showPrimaryStage);
    }

    private static void runAttemptLoadGameTask(final KeyFile keyFile)
    {
        final TrackedTask<Void> task = attemptLoadGameTask(keyFile);
        if (task == null) return;
        task.trackWith(new LoadingStageTracker()).start();
    }

    //----------------//
    // Task Internals //
    //----------------//

    private static KeyFile attemptLoadPreviousGameKey(final TaskTrackerI tracker) throws Exception
    {
        tracker.updateMessage("Resuming previous game ...");
        tracker.updateProgress(0, 1);

        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        final JsonElement lastGameDirectoryElement = settingsRoot.get("lastGameDirectory");

        KeyFile keyFile = null;

        // 'lastGameDirectory' set, try to use it
        final Path lastGameDirectoryPath = Path.of(lastGameDirectoryElement.getAsString());

        // Check if `lastGameDirectoryPath` is valid
        if (Files.isDirectory(lastGameDirectoryPath))
        {
            // `lastGameDirectoryPath` is a directory, try to load chitin.key

            final Path chitinPath = FileUtil.resolveCaseInsensitiveElseError(
                lastGameDirectoryPath, "chitin.key",
                (errorPathStr) -> String.format("Key file does not exist: \"%s\"", errorPathStr));

            keyFile = new KeyFile(chitinPath);
            keyFile.load(tracker);
        }

        return keyFile;
    }

    //---------//
    // General //
    //---------//

    private static void resumeOrAskForGame()
    {
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        final JsonElement lastGameDirectoryElement = settingsRoot.get("lastGameDirectory");

        if (lastGameDirectoryElement == null)
        {
            // 'lastGameDirectory' not set, ask for game
            runAttemptLoadGameTask(showGamePicker());
            return;
        }

        final TrackedTask<KeyFile> task = new TrackedTask<>()
        {
            @Override
            protected KeyFile doTask(final TaskTrackerI tracker)
            {
                try
                {
                    return attemptLoadPreviousGameKey(tracker);
                }
                catch (final Exception e)
                {
                    ErrorAlert.openAndWait("An exception occurred while loading the previously " +
                        "opened key file. You will be asked to select a new game install.", e);
                }

                tracker.hide();
                final KeyFile keyFile = onInvalidAskForGame(settingsRoot);
                tracker.show();
                return keyFile;
            }
        };

        task.trackWith(new LoadingStageTracker())
            .chainOnSuccess(MainJavaFX::attemptLoadGameTask)
            .onFailedFx(ErrorAlert::openAndWait)
            .start();
    }

    private static void saveMainWindowLocation(final int x, final int y, final int w, final int h)
    {
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        settingsRoot.addProperty("mainWindowX", x);
        settingsRoot.addProperty("mainWindowY", y);
        settingsRoot.addProperty("mainWindowWidth", w);
        settingsRoot.addProperty("mainWindowHeight", h);
    }

    private static void saveMainWindowLocation()
    {
        final Stage primaryStage = GlobalState.getPrimaryStage();
        saveMainWindowLocation((int)primaryStage.getX(), (int)primaryStage.getY(),
            (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
    }

    private static void onPrimaryStageClosing(final WindowEvent event)
    {
        final Stage primaryStage = (Stage)event.getSource();
        if (!primaryStage.isMaximized())
        {
            saveMainWindowLocation();
        }

        Platform.exit();
    }

    private static void onPrimaryStageMaximizedChanged(final boolean oldValue, final boolean newValue)
    {
        if (!oldValue && newValue)
        {
            final Stage primaryStage = GlobalState.getPrimaryStage();
            saveMainWindowLocation(bufferedX[1], bufferedY[1],
                (int)primaryStage.getWidth(), (int)primaryStage.getHeight());
        }
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        settingsRoot.addProperty("mainWindowMaximized", newValue);
    }

    private static KeyFile onInvalidAskForGame(final JsonObject settingsRoot)
    {
        settingsRoot.remove("lastGameDirectory");
        return showGamePicker();
    }

    private static KeyFile showGamePicker()
    {
        return JavaFXUtil.waitForFxThreadToExecute(() ->
        {
            final GamePickerStage gamePickerStage = new GamePickerStage();
            GlobalState.registerStage(gamePickerStage);
            gamePickerStage.setOnCloseRequest((ignored) -> Platform.exit());
            gamePickerStage.showAndWait();
            GlobalState.deregisterStage(gamePickerStage);
            return gamePickerStage.getPickedKeyFile();
        });
    }

    private static void showPrimaryStage()
    {
        // valid game selected, save the game directory under 'lastGameDirectory'
        final JsonObject settingsRoot = GlobalState.getSettingsFile().getRoot();
        settingsRoot.addProperty("lastGameDirectory", GlobalState.getGame().getRoot().toString());

        final Stage primaryStage = GlobalState.getPrimaryStage();
        primaryStage.setOnCloseRequest(MainJavaFX::onPrimaryStageClosing);

        primaryStage.maximizedProperty().addListener((observable, oldValue, newValue)
            -> onPrimaryStageMaximizedChanged(oldValue, newValue));

        primaryStage.xProperty().addListener((observable, oldValue, newValue) ->
        {
            bufferedX[1] = bufferedX[0];
            bufferedX[0] = newValue.intValue();
        });

        primaryStage.yProperty().addListener((observable, oldValue, newValue) ->
        {
            bufferedY[1] = bufferedY[0];
            bufferedY[0] = newValue.intValue();
        });

        final PrimaryScene primaryScene = new PrimaryScene(primaryStage);
        primaryScene.initMainScene();

        final JavaFXUtil.Rectangle screenRect = JavaFXUtil.getScreenRect();

        SettingsUtil.attemptApplyInt(settingsRoot, "mainWindowX",
            (x) -> primaryStage.setX(Math.max(screenRect.x(), x)));

        SettingsUtil.attemptApplyInt(settingsRoot, "mainWindowY",
            (y) -> primaryStage.setY(Math.max(screenRect.y(), y)));

        SettingsUtil.attemptApplyInt(settingsRoot, "mainWindowWidth",
            (width) -> primaryStage.setWidth(Math.min(width, screenRect.width() - primaryStage.getX())));

        SettingsUtil.attemptApplyInt(settingsRoot, "mainWindowHeight",
            (height) -> primaryStage.setHeight(Math.min(height, screenRect.height() - primaryStage.getY())));

        SettingsUtil.attemptApplyBoolean(settingsRoot, "mainWindowMaximized", primaryStage::setMaximized);

        JavaFXUtil.forceToFront(primaryStage);
        primaryStage.show();

        bufferedX[0] = (int)primaryStage.getX(); bufferedX[1] = bufferedX[0];
        bufferedY[0] = (int)primaryStage.getY(); bufferedY[1] = bufferedY[0];
    }
}
