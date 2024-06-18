
package com.github.bubb13.infinityareas;

import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public final class PrimaryScene extends Stage
{
    /////////////////////
    // Instance Fields //
    /////////////////////

    private final Stage stage;

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
            rightPane.setBackground(Background.fill(Color.LIGHTCORAL));


        splitPane.getItems().addAll(leftPane, rightPane);

        ///////////
        // Scene //
        ///////////

        final Scene scene = new Scene(splitPane, 500, 500);

        stage.setTitle("Infinity Areas");
        stage.setScene(scene);
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

    private void onSelectAreaSource(final Game.ResourceSource areaSource)
    {
        final Area area = new Area(areaSource);
        new JavaFXUtil.TaskManager(area.loadAreaTask()
            .onFailed((e) ->
            {
                new ErrorAlert("An exception occurred while loading the area.\n\n"
                    + MiscUtil.formatStackTrace(e)).showAndWait();
            })
        ).run();
    }

    private record ResourceSourceHolder(Game.ResourceSource source, String text)
    {
        @Override
        public String toString()
        {
            return text;
        }
    }
}
