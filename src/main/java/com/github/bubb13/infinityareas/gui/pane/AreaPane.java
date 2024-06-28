
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.control.ShrinkableImageView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.awt.image.BufferedImage;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ZoomPane zoomPane = new ZoomPane();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setImage(final BufferedImage image)
    {
        zoomPane.setImage(image);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        getChildren().add(zoomPane);
    }
}
