
package com.github.bubb13.infinityareas.gui.scene;

import com.github.bubb13.infinityareas.MainJavaFX;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.gui.control.SimpleTreeView;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PrimaryScene extends Stage
{
    /////////////////////
    // Instance Fields //
    /////////////////////

    private final Stage stage;
    private ImageView imageView;

    //////////////////
    // Constructors //
    //////////////////

    public PrimaryScene(final Stage stage)
    {
        this.stage = stage;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    /////////////////////
    // Private Methods //
    /////////////////////

    public void initMainScene()
    {
        ///////////////
        // Main VBox //
        ///////////////

        final VBox vbox = new VBox();

            //////////////////
            // Toolbar HBox //
            //////////////////

            final HBox toolbar = new HBox();

            final MenuButton gameDropdown = new MenuButton("Game");
            final MenuItem changeButton = new MenuItem("Change");
            changeButton.setOnAction((ignored) -> this.onSelectChangeGame());
            gameDropdown.getItems().addAll(changeButton);

            final MenuButton debugDropdown = new MenuButton("Debug");
            final MenuItem stepButton = new MenuItem("Step through all areas");
            stepButton.setOnAction((ignored) -> this.debugStepThroughAllAreas());
            debugDropdown.getItems().addAll(stepButton);

            toolbar.getChildren().addAll(gameDropdown, debugDropdown);

            ////////////////////
            // Main SplitPane //
            ////////////////////

            final SplitPane splitPane = new SplitPane();

                ///////////////
                // Left Pane //
                ///////////////

                final StackPane leftPane = new StackPane();

                    //////////////
                    // TreeView //
                    //////////////

                    final TreeItem<ResourceSourceHolder> rootNode = createAreaTreeViewNodes();
                    final SimpleTreeView<ResourceSourceHolder> treeView = new SimpleTreeView<>(rootNode);
                    treeView.setShowRoot(false);
                    treeView.setOnActivate((selected) -> this.onSelectAreaSource(selected.source()));


                leftPane.getChildren().add(treeView);

                ////////////////
                // Right Pane //
                ////////////////

                final StackPane rightPane = new StackPane();

                imageView = new ImageView()
                {
                    @Override
                    public double minHeight(final double width)
                    {
                        return 80;
                    }

                    @Override
                    public double minWidth(final double height)
                    {
                        return 80;
                    }
                };

                imageView.fitWidthProperty().bind(rightPane.widthProperty());
                imageView.fitHeightProperty().bind(rightPane.heightProperty());
                imageView.setPreserveRatio(true);

                rightPane.getChildren().add(imageView);


            splitPane.getItems().addAll(leftPane, rightPane);


        VBox.setVgrow(splitPane, Priority.ALWAYS);
        vbox.getChildren().addAll(toolbar, splitPane);

        ///////////
        // Scene //
        ///////////

        final Scene scene = new Scene(vbox, 500, 500);

        stage.setTitle("Infinity Areas");
        stage.setScene(scene);

        //debugStepThroughAllAreas();
    }

    private TreeItem<ResourceSourceHolder> createAreaTreeViewNodes()
    {
        final Game game = GlobalState.getGame();
        final TreeItem<ResourceSourceHolder> rootNode = new TreeItem<>();

        for (final Game.Resource resource : game.getResourcesOfType(KeyFile.NumericResourceType.ARE))
        {
            final TreeItem<ResourceSourceHolder> resrefNode = new TreeItem<>(
                new ResourceSourceHolder(null, resource.getIdentifier().resref()));

            for (final Game.ResourceSource source : resource.sources())
            {
                switch (source.getSourceType())
                {
                    case BIF ->
                    {
                        final Game.BifSource bifSource = (Game.BifSource)source;
                        final String pathStr = bifSource.getRelativePathStr();

                        final TreeItem<ResourceSourceHolder> sourceNode = new TreeItem<>(
                            new ResourceSourceHolder(source, "bif: " + pathStr)
                        );
                        resrefNode.getChildren().add(sourceNode);
                    }
                    case LOOSE_FILE ->
                    {
                        final Game.LooseFileSource looseFileSource = (Game.LooseFileSource) source;
                        final String pathStr = looseFileSource.getRelativePathStr();

                        final TreeItem<ResourceSourceHolder> sourceNode = new TreeItem<>(
                            new ResourceSourceHolder(source, "file: " + pathStr)
                        );
                        resrefNode.getChildren().add(sourceNode);
                    }
                }
            }

            rootNode.getChildren().add(resrefNode);
        }
        return rootNode;
    }

    private void onSelectChangeGame()
    {
        MainJavaFX.closePrimaryStageAndAskForGame();
    }

    private void onSelectAreaSource(final Game.ResourceSource areaSource)
    {
        final Area area = new Area(areaSource);
        new JavaFXUtil.TaskManager(area.loadAreaTask()
            .onSucceeded(() -> renderPrimaryOverlay(area))
            .onFailed((e) ->
            {
                ErrorAlert.openAndWait("An exception occurred while loading the area.", e);
            })
        ).run();
    }

    private void renderPrimaryOverlay(final Area area)
    {
        if (area.getOverlayCount() <= 0)
        {
            return;
        }

        new JavaFXUtil.TaskManager(area.renderOverlaysTask(0, 1, 2, 3, 4)
            .onSucceeded(this::showRenderedOverlay)
            .onFailed((e) ->
            {
                ErrorAlert.openAndWait("An exception occurred while rendering " +
                    "the primary area overlay.", e);
            })
        ).run();
    }

    private void showRenderedOverlay(final BufferedImage overlay)
    {
        final Image image = SwingFXUtils.toFXImage(overlay, null);
        imageView.setImage(image);
    }

    private void debugStepThroughAllAreas()
    {
        new JavaFXUtil.TaskManager(new JavaFXUtil.TaskManager.ManagedTask<>()
        {
            @Override
            protected Void call() throws Exception
            {
                for (final Game.Resource resource : GlobalState.getGame()
                    .getResourcesOfType(KeyFile.NumericResourceType.ARE))
                {
                    try
                    {
                        final Area area = new Area(resource.getPrimarySource());
                        subtask(area.loadAreaTask());

                        final BufferedImage overlay = subtask(area.renderOverlayTask(0));
                        waitForGuiThreadToExecute(() -> showRenderedOverlay(overlay));
                    }
                    catch (final Exception e)
                    {
                        ErrorAlert.openAndWait(String.format(
                            "Failed to render area \"%s\"", resource.getIdentifier().resref()), e);
                    }
                    //Thread.sleep(50);
                }
                return null;
            }
        }).run();
    }

    private void debugSaveOverlays()
    {
        new JavaFXUtil.TaskManager(new JavaFXUtil.TaskManager.ManagedTask<>()
        {
            @Override
            protected Void call() throws Exception
            {
                final Game game = GlobalState.getGame();
                final Path debugPath = game.getRoot().resolve("debug");
                Files.createDirectories(debugPath);

                final Game.Resource resource = GlobalState.getGame().getResource(
                    new ResourceIdentifier("AR0011", KeyFile.NumericResourceType.ARE));

                final Area area = new Area(resource.getPrimarySource());
                subtask(area.loadAreaTask());

                for (int i = 0; i < area.getOverlayCount(); ++i)
                {
                    final BufferedImage overlay = subtask(area.renderOverlayTask(i));

                    ImageIO.write(overlay, "png", debugPath.resolve(
                        String.format("OVERLAY_%d.PNG", i)).toFile());
                }

                return null;
            }
        }).run();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record ResourceSourceHolder(Game.ResourceSource source, String text)
    {
        @Override
        public String toString()
        {
            return text;
        }
    }
}
