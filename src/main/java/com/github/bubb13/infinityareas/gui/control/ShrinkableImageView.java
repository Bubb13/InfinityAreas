
package com.github.bubb13.infinityareas.gui.control;

import javafx.scene.image.ImageView;

public class ShrinkableImageView extends ImageView
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
}
