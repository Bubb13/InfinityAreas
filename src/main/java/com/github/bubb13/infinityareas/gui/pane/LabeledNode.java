
package com.github.bubb13.infinityareas.gui.pane;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LabeledNode extends HBox
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Label label = new Label();
    private final VBox nodeVBox = new VBox();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LabeledNode(final String labelText, final Node node)
    {
        setLabel(labelText);
        setNode(node);
        init();
    }

    public LabeledNode(final String labelText)
    {
        setLabel(labelText);
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setLabel(final String labelText)
    {
        label.setText(labelText + ": ");
    }

    public void setNode(final Node node)
    {
        final var children = nodeVBox.getChildren();
        children.clear();
        children.addAll(node);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final VBox labelVBox = new VBox();
        labelVBox.setAlignment(Pos.CENTER);
        labelVBox.getChildren().addAll(label);

        nodeVBox.setAlignment(Pos.CENTER);

        getChildren().addAll(labelVBox, nodeVBox);
    }
}
