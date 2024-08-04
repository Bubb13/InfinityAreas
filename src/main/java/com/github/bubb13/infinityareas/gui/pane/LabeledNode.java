
package com.github.bubb13.infinityareas.gui.pane;

import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
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
    private final ChangeListener<Boolean> nodeDisableListener =
        (observable, oldValue, newValue) -> label.setDisable(newValue);

    private Node node;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LabeledNode(final String labelText, final Node node, final Pos labelPosition)
    {
        setLabel(labelText);
        setNode(node);
        init(labelPosition);
    }

    public LabeledNode(final String labelText, final Node node)
    {
        setLabel(labelText);
        setNode(node);
        init(Pos.CENTER);
    }

    public LabeledNode(final String labelText)
    {
        setLabel(labelText);
        init(Pos.CENTER);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setLabel(final String labelText)
    {
        label.setText(labelText + ": ");
    }

    public void setNode(final Node newNode)
    {
        if (node != null)
        {
            node.disableProperty().removeListener(nodeDisableListener);
        }
        node = newNode;
        node.disableProperty().addListener(nodeDisableListener);

        final ObservableList<Node> children = nodeVBox.getChildren();
        children.clear();
        children.addAll(node);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(final Pos labelPos)
    {
        final VBox labelVBox = new VBox();
        labelVBox.setAlignment(labelPos);
        labelVBox.getChildren().addAll(label);

        nodeVBox.setAlignment(Pos.CENTER);

        getChildren().addAll(labelVBox, nodeVBox);
    }
}
