
package com.github.bubb13.infinityareas.util;

import com.github.bubb13.infinityareas.GlobalState;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.concurrent.Phaser;
import java.util.function.Consumer;

public final class JavaFXUtil
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void forceToFront(final Stage stage)
    {
        // Force the window to the top of the window stack
        // Note: Stage::toFront() doesn't work for an unknown reason
        if (stage.isShowing())
        {
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
            GlobalState.checkFrontStage();
        }
        else
        {
            stage.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>()
            {
                @Override
                public void handle(WindowEvent window)
                {
                    stage.removeEventHandler(WindowEvent.WINDOW_SHOWN, this);
                    stage.setAlwaysOnTop(true);
                    stage.setAlwaysOnTop(false);
                    GlobalState.checkFrontStage();
                }
            });
        }
    }

    public static Rectangle getScreenRect()
    {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final Screen screen : Screen.getScreens())
        {
            final Rectangle2D bounds = screen.getBounds();
            minX = Math.min(minX, (int)bounds.getMinX());
            minY = Math.min(minY, (int)bounds.getMinY());
            maxX = Math.max(maxX, (int)bounds.getMaxX());
            maxY = Math.max(maxY, (int)bounds.getMaxY());
        }

        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    @SuppressWarnings("unused")
    public static WidthHeight calculateWidthHeight(final Consumer<Pane> consumer)
    {
        final Pane dummyPane = new Pane();
        consumer.accept(dummyPane);

        // Needed for layout even though the reference is unused
        final Scene dummyScene = new Scene(dummyPane);

        // Do layout
        while (dummyPane.isNeedsLayout())
        {
            dummyPane.applyCss();
            dummyPane.layout();
        }

        final Bounds bounds = dummyPane.getBoundsInLocal();
        for (Node child : dummyPane.getChildren())
        {
            System.out.printf("[child] width: %f, height: %f\n", child.prefWidth(-1), child.prefHeight(-1));
        }

        return new WidthHeight(bounds.getWidth(), bounds.getHeight());
    }

    public static void waitForFxThreadToExecute(final Runnable runnable)
    {
        if (Platform.isFxApplicationThread())
        {
            runnable.run();
        }
        else
        {
            final Phaser waitLatch = new Phaser(1);

            Platform.runLater(() ->
            {
                runnable.run();
                waitLatch.arrive();
            });

            waitLatch.awaitAdvance(waitLatch.getPhase());
        }
    }

    public static void loadInlineStylesheet(final Parent parent, final String css)
    {
        parent.getStylesheets().add("data:text/css," + css
            .replace("\n", "")
            .replace(" ", "%20"));
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private JavaFXUtil() {}

    ////////////////////
    // Public Classes //
    ////////////////////

    public record Rectangle(int x, int y, int width, int height) {}

    public record WidthHeight(double width, double height) {}
}
