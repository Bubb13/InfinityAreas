
package com.github.bubb13.infinityareas;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.misc.InstanceHashMap;
import com.github.bubb13.infinityareas.misc.tasktracking.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.application.Application;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.awt.Toolkit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;

public class GlobalState
{
    private static final InstanceHashMap<Stage, ExtraStageInfo> extraStageInfo = new InstanceHashMap<>();
    private static final Stack<Stage> modalStages = new Stack<>();

    private static Path infinityAreasRoot;
    private static Path infinityAreasTemp;
    private static SettingsFile settingsFile;
    private static Game game;
    private static Application application;
    private static Stage primaryStage;
    private static PixelFormat.Type nativePixelFormatType;
    private static Stage frontStage;
    private static String infinityAreasStylesheet;
    private static Robot robot;

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
        loadInfinityAreasStylesheet();
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
        registerStage(primaryStage);
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

    public static void registerStage(final Stage stage)
    {
        final EventHandler<Event> eventFilter = new EventHandler<>()
        {
            private boolean modalMousePress;

            @Override
            public void handle(Event event)
            {
                if (modalStages.empty() || modalStages.peek() == stage)
                {
                    // Allow event. Either no modal stages are open, or the topmost modal stage is the target.
                    return;
                }

                // Consume the event so that the blocked stage doesn't respond to the input
                event.consume();

                boolean modalBeep = false;

                // Beep an error tone if the user attempts to interact with a blocked stage
                final EventType<?> eventType = event.getEventType();
                if (eventType == MouseEvent.MOUSE_PRESSED)
                {
                    modalMousePress = true;
                }
                else if (eventType == MouseEvent.MOUSE_CLICKED && modalMousePress)
                {
                    // Only beep if the click started after the modal mode was entered
                    modalMousePress = false;
                    modalBeep = true;
                }
                else if (eventType == WindowEvent.WINDOW_CLOSE_REQUEST)
                {
                    // Also beep if a window close is attempted during a modal
                    modalBeep = true;
                }

                if (modalBeep)
                {
                    Toolkit.getDefaultToolkit().beep();
                    for (final Stage modalStage : modalStages)
                    {
                        if (modalStage == null) continue;
                        modalStage.toFront();
                        break;
                    }
                }
            }
        };

        stage.addEventFilter(Event.ANY, eventFilter);
        extraStageInfo.put(stage, new ExtraStageInfo(eventFilter));
    }

    public static void deregisterStage(final Stage stage)
    {
        final ExtraStageInfo extraStageInfo = GlobalState.extraStageInfo.remove(stage);
        stage.removeEventFilter(Event.ANY, extraStageInfo.filters());
    }

    public static void pushModalStage(final Stage stage)
    {
        modalStages.push(stage);
    }

    public static void popModalStage(final Stage stage)
    {
        modalStages.remove(stage);
    }

    public static void checkFrontStage()
    {
        if (frontStage != null)
        {
            frontStage.setAlwaysOnTop(true);
            frontStage.setAlwaysOnTop(false);
        }
    }

    public static void setFrontStage(final Stage stage)
    {
        frontStage = stage;
    }

    public static String getInfinityAreasStylesheet()
    {
        return infinityAreasStylesheet;
    }

    public static void createRobot()
    {
        robot = new Robot();
    }

    public static Point2D getMousePosition()
    {
        return robot.getMousePosition();
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

    private static void loadInfinityAreasStylesheet()
    {
        infinityAreasStylesheet = GlobalState.class.getClassLoader()
            .getResource("infinity_areas.css").toExternalForm();
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

    /////////////////////
    // Private Classes //
    /////////////////////

    private record ExtraStageInfo(EventHandler<Event> filters) {}
}
