
package com.github.bubb13.infinityareas.gui.control;

import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class ColorButton extends Button
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Rectangle rectangle = new Rectangle();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ColorButton(final String text)
    {
        rectangle.setWidth(10);
        rectangle.setHeight(10);
        setGraphic(new RectangleHolder(rectangle));
        setText(text);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setColor(final Color color)
    {
        rectangle.setFill(color);
    }

    public void setColor(final int color)
    {
        final int b = color & 0xFF;
        final int g = (color >>> 8) & 0xFF;
        final int r = (color >>> 16) & 0xFF;
        final int a = (color >>> 24) & 0xFF;
        setColor(Color.rgb(r, g, b, (double)a / 255));
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class RectangleHolder extends Region
    {
        public RectangleHolder(final Rectangle rectangle)
        {
            getChildren().add(rectangle);
        }
    }
}
