
package com.github.bubb13.infinityareas.gui.pane;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public class BitfieldPane extends GridPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    ////////////////////
    // Public Methods //
    ////////////////////

    public BitfieldPane(final int numBits, final int numPerColumn)
    {
        super(10, 5);
        if (numPerColumn <= 0) throw new IllegalArgumentException();
        init(numBits, numPerColumn);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(final int numBits, final int numPerColumn)
    {
        final ObservableList<Node> children = getChildren();

        for (int i = 0, columnCounter = 1, columnIndex = 0, rowIndex = 0; i < numBits; ++i)
        {
            LabeledCheckbox checkbox = new LabeledCheckbox("Bit " + i);
            GridPane.setColumnIndex(checkbox, columnIndex);
            GridPane.setRowIndex(checkbox, rowIndex);
            children.add(checkbox);

            if (columnCounter >= numPerColumn)
            {
                columnCounter = 1;
                ++columnIndex;
                rowIndex = 0;
            }
            else
            {
                ++columnCounter;
                ++rowIndex;
            }
        }
    }
}
