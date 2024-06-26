
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.control.ShrinkableImageView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ImageView imageView;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPane()
    {
        super();
        imageView = new ShrinkableImageView();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setImage(final Image image)
    {
        imageView.setImage(image);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        imageView.fitWidthProperty().bind(widthProperty());
        imageView.fitHeightProperty().bind(heightProperty());
        imageView.setPreserveRatio(true);
        getChildren().add(imageView);
    }
}
