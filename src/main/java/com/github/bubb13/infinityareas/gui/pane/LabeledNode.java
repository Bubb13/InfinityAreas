
package com.github.bubb13.infinityareas.gui.pane;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LabeledNode extends HBox
{
    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LabeledNode(final String labelText, final Node node)
    {
        init(labelText, node);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(final String labelText, final Node node)
    {
        final Label label = new Label(labelText + ": ");

        final VBox labelVBox = new VBox();
        labelVBox.setAlignment(Pos.CENTER);
        labelVBox.getChildren().addAll(label);

        final VBox nodeVBox = new VBox();
        nodeVBox.setAlignment(Pos.CENTER);
        nodeVBox.getChildren().addAll(node);

        getChildren().addAll(labelVBox, nodeVBox);
    }
}
