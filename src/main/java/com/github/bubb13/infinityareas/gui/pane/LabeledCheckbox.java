
package com.github.bubb13.infinityareas.gui.pane;

import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LabeledCheckbox extends HBox
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final CheckBox checkBox = new CheckBox();
    private final Label label = new Label();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public LabeledCheckbox(final String labelText)
    {
        super(5);
        setLabel(labelText);
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public CheckBox getCheckbox()
    {
        return checkBox;
    }

    public void setLabel(final String labelText)
    {
        label.setText(labelText);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final VBox labelVBox = new VBox();
        labelVBox.setAlignment(Pos.CENTER);
        labelVBox.getChildren().addAll(label);

        final VBox nodeVBox = new VBox();
        nodeVBox.setAlignment(Pos.CENTER);
        nodeVBox.getChildren().addAll(checkBox);

        getChildren().addAll(nodeVBox, labelVBox);
    }
}
