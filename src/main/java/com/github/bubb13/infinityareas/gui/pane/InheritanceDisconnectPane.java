
package com.github.bubb13.infinityareas.gui.pane;

import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;

public class InheritanceDisconnectPane extends Pane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Parent root;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public InheritanceDisconnectPane(final Parent root)
    {
        this.root = root;
        init();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected double computeMinWidth(double height)
    {
        return root.minWidth(height);
    }

    @Override
    protected double computeMinHeight(double width)
    {
        return root.minHeight(width);
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return root.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return root.prefHeight(width);
    }

    @Override
    protected double computeMaxWidth(double height)
    {
        return root.maxWidth(height);
    }

    @Override
    protected double computeMaxHeight(double width)
    {
        return root.maxHeight(width);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final SubScene subScene = new SubScene(root, 0, 0);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        getChildren().add(subScene);
    }
}
