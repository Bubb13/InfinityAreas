
package com.github.bubb13.infinityareas.gui.control;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

public class UnderlinedButton extends Button
{
    public UnderlinedButton(final String text)
    {
        final Text firstText = new Text(text.substring(0, 1));
        firstText.setUnderline(true);

        final TextFlow textFlow;

        if (text.length() > 1)
        {
            final Text remainingText = new Text(text.substring(1));
            textFlow = new NonWrappingTextFlow(firstText, remainingText);
        }
        else
        {
            textFlow = new NonWrappingTextFlow(firstText);
        }

        this.setGraphic(textFlow);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class NonWrappingTextFlow extends TextFlow
    {
        public NonWrappingTextFlow()
        {
            super();
        }

        public NonWrappingTextFlow(final Node... children)
        {
            super(children);
        }

        @Override
        protected double computePrefHeight(double width)
        {
            return width < Region.USE_COMPUTED_SIZE
                ? super.computePrefHeight(Region.USE_COMPUTED_SIZE) + 1
                : super.computePrefHeight(width);
        }
    }
}
