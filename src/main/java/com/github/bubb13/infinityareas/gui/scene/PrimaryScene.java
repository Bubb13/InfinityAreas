
package com.github.bubb13.infinityareas.gui.scene;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.MainJavaFX;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.gui.control.SimpleTreeView;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.pane.AreaPane;
import com.github.bubb13.infinityareas.gui.pane.TISPane;
import com.github.bubb13.infinityareas.gui.pane.WEDPane;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
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
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Stage stage;
    private final StackPane rightPane = new StackPane();
    private final AreaPane areaPane = new AreaPane();
    private final TISPane tisPane = new TISPane();
    private final WEDPane wedPane = new WEDPane();

    private Node curRightNode = null;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public PrimaryScene(final Stage stage)
    {
        this.stage = stage;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

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

                    final TreeItem<Object> rootNode = createAreaTreeViewNodes();
                    final SimpleTreeView<Object> treeView = new SimpleTreeView<>(rootNode);
                    treeView.setShowRoot(false);
                    treeView.setOnActivate((selected) -> this.onSelectResourceSource(((ResourceSourceHolder)selected).source()));


                leftPane.getChildren().add(treeView);


            splitPane.getItems().addAll(leftPane, rightPane);


        VBox.setVgrow(splitPane, Priority.ALWAYS);
        vbox.getChildren().addAll(toolbar, splitPane);

        ///////////
        // Scene //
        ///////////

        final Scene scene = new Scene(vbox, 500, 500);

        stage.setTitle("Infinity Areas");
        stage.setScene(scene);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private TreeItem<Object> createAreaTreeViewNodes()
    {
        final TreeItem<Object> areNode = new TreeItem<>("ARE");
        final TreeItem<Object> tisNode = new TreeItem<>("TIS");
        final TreeItem<Object> wedNode = new TreeItem<>("WED");

        for (final Game.Resource resource : GlobalState.getGame().getResources())
        {
            final ResourceIdentifier identifier = resource.getIdentifier();

            switch (KeyFile.NumericResourceType.fromNumericType(identifier.numericType()))
            {
                case ARE -> addResourceTypeNode(areNode, resource);
                case TIS -> addResourceTypeNode(tisNode, resource);
                case WED -> addResourceTypeNode(wedNode, resource);
            }
        }

        final TreeItem<Object> rootNode = new TreeItem<>();
        //noinspection unchecked
        rootNode.getChildren().addAll(areNode, tisNode, wedNode);
        return rootNode;
    }

    private void addResourceTypeNode(final TreeItem<Object> parentNode, final Game.Resource resource)
    {
        final TreeItem<Object> resrefNode = new TreeItem<>(
            new ResourceSourceHolder(null, resource.getIdentifier().resref())
        );
        addSourceNodes(resrefNode, resource);
        parentNode.getChildren().add(resrefNode);
    }

    private void addSourceNodes(final TreeItem<Object> parentNode, final Game.Resource resource)
    {
        for (final Game.ResourceSource source : resource.sources())
        {
            switch (source.getSourceType())
            {
                case BIF ->
                {
                    final Game.BifSource bifSource = (Game.BifSource)source;
                    final String pathStr = bifSource.getRelativePathStr();

                    final TreeItem<Object> sourceNode = new TreeItem<>(
                        new ResourceSourceHolder(source, "bif: " + pathStr)
                    );
                    parentNode.getChildren().add(sourceNode);
                }
                case LOOSE_FILE ->
                {
                    final Game.LooseFileSource looseFileSource = (Game.LooseFileSource)source;
                    final String pathStr = looseFileSource.getRelativePathStr();

                    final TreeItem<Object> sourceNode = new TreeItem<>(
                        new ResourceSourceHolder(source, "file: " + pathStr)
                    );
                    parentNode.getChildren().add(sourceNode);
                }
            }
        }
    }

    private void onSelectChangeGame()
    {
        MainJavaFX.closePrimaryStageAndAskForGame();
    }

    private void onSelectResourceSource(final Game.ResourceSource source)
    {
        switch (source.getNumericType())
        {
            case ARE ->
            {
                final Area area = new Area(source);
                area.loadTask()
                    .trackWith(new LoadingStageTracker())
                    .onSucceeded(() -> renderPrimaryOverlay(area))
                    .onFailed((e) ->
                        ErrorAlert.openAndWait("An exception occurred while loading the area.", e))
                    .start();
            }
            case TIS ->
            {
                tisPane.setSourceTask(source)
                    .trackWith(new LoadingStageTracker())
                    .onSucceededFx(() -> changeRightNode(tisPane))
                    .onFailed((e) ->
                        ErrorAlert.openAndWait("An exception occurred while loading the tileset.", e))
                    .start();
            }
            case WED ->
            {
                wedPane.setSourceTask(source)
                    .trackWith(new LoadingStageTracker())
                    .onSucceededFx(() -> changeRightNode(wedPane))
                    .onFailed((e) ->
                        ErrorAlert.openAndWait("An exception occurred while loading the WED.", e))
                    .start();
            }
        }
    }

    private void changeRightNode(final Node newNode)
    {
        if (newNode != curRightNode)
        {
            curRightNode = newNode;
            final ObservableList<Node> children = rightPane.getChildren();
            children.clear();
            children.add(newNode);
        }
    }

    private void renderPrimaryOverlay(final Area area)
    {
        if (area.getOverlayCount() <= 0)
        {
            return;
        }

        area.renderOverlaysTask(0, 1, 2, 3, 4)
            .trackWith(new LoadingStageTracker())
            .onSucceededFx(this::showRenderedOverlay)
            .onFailed((e) ->
                ErrorAlert.openAndWait("An exception occurred while rendering the primary area overlay.", e))
            .start();
    }

    private void showRenderedOverlay(final BufferedImage overlay)
    {
        areaPane.setImage(overlay);
        changeRightNode(areaPane);
    }

    private void debugStepThroughAllAreas()
    {
        new TrackedTask<>()
        {
            @Override
            protected Void doTask() throws Exception
            {
                for (final Game.Resource resource : GlobalState.getGame()
                    .getResourcesOfType(KeyFile.NumericResourceType.ARE))
                {
                    try
                    {
                        final Area area = new Area(resource.getPrimarySource());
                        area.load(getTracker());

                        final BufferedImage overlay = area.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
                        waitForFxThreadToExecute(() -> showRenderedOverlay(overlay));
                    }
                    catch (final Exception e)
                    {
                        ErrorAlert.openAndWait(String.format(
                            "Failed to render area \"%s\"", resource.getIdentifier().resref()), e);
                    }
                    //Thread.sleep(200);
                }
                return null;
            }
        }.start();
    }

    private void debugSaveOverlays()
    {
        new TrackedTask<>()
        {
            @Override
            protected Void doTask() throws Exception
            {
                final Game game = GlobalState.getGame();
                final Path debugPath = game.getRoot().resolve("debug");
                Files.createDirectories(debugPath);

                final Game.Resource resource = GlobalState.getGame().getResource(
                    new ResourceIdentifier("AR0011", KeyFile.NumericResourceType.ARE));

                final Area area = new Area(resource.getPrimarySource());
                area.load(getTracker());

                for (int i = 0; i < area.getOverlayCount(); ++i)
                {
                    final BufferedImage overlay = area.renderOverlays(getTracker(), i);

                    ImageIO.write(overlay, "png", debugPath.resolve(
                        String.format("OVERLAY_%d.PNG", i)).toFile());
                }

                return null;
            }
        }.start();
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
