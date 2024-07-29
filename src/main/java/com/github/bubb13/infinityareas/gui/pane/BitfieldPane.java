
package com.github.bubb13.infinityareas.gui.pane;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

import java.util.function.BiConsumer;

public class BitfieldPane extends GridPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final LabeledCheckbox[] checkboxes;
    private BiConsumer<Integer, Boolean> bitSelectionChangedListener;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public BitfieldPane(final int numBits, final int numPerColumn, final Iterable<Integer> shownBitIndices)
    {
        super(10, 5);
        if (numPerColumn <= 0) throw new IllegalArgumentException();
        checkboxes = new LabeledCheckbox[numBits];
        init(numBits, numPerColumn, shownBitIndices);
    }

    public BitfieldPane(final int numBits, final int numPerColumn)
    {
        this(numBits, numPerColumn, null);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setBitLabel(final int bitIndex, final String label)
    {
        checkboxes[bitIndex].setLabel(label);
    }

    public void setBitChecked(final int bitIndex, final boolean value)
    {
        checkboxes[bitIndex].getCheckbox().setSelected(value);
    }

    public void setBitSelectionChangedListener(final BiConsumer<Integer, Boolean> listener)
    {
        bitSelectionChangedListener = listener;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(final int numBits, final int numPerColumn, final Iterable<Integer> shownBitIndices)
    {
        final ObservableList<Node> children = getChildren();

        for (int i = 0, columnCounter = 1, columnIndex = 0, rowIndex = 0; i < numBits; ++i)
        {
            if (shownBitIndices != null)
            {
                boolean found = false;

                for (int checkIndex : shownBitIndices)
                {
                    if (checkIndex == i)
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    continue;
                }
            }

            LabeledCheckbox checkbox = new LabeledCheckbox("Bit " + i);
            checkbox.getCheckbox().selectedProperty().addListener(new CheckboxListener(i));
            GridPane.setColumnIndex(checkbox, columnIndex);
            GridPane.setRowIndex(checkbox, rowIndex);
            checkboxes[i] = checkbox;
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

    private void onBitSelectionChanged(final Integer bitIndex, final Boolean newValue)
    {
        if (bitSelectionChangedListener != null)
        {
            bitSelectionChangedListener.accept(bitIndex, newValue);
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class CheckboxListener implements ChangeListener<Boolean>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Integer bitIndex;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public CheckboxListener(final Integer bitIndex)
        {
            this.bitIndex = bitIndex;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public void changed(
            final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue)
        {
            onBitSelectionChanged(bitIndex, newValue);
        }
    }
}
