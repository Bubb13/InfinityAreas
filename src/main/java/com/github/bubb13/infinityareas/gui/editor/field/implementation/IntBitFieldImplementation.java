
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.pane.BitfieldPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;

import java.util.ArrayList;

public class IntBitFieldImplementation<FieldEnumType extends Enum<?>, MappedEnumType extends MappedIntEnum>
    extends FieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final MappedIntBitFieldOptions<MappedEnumType> options;
    private final TitledPane mainPane = new TitledPane();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public IntBitFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector,
        final MappedIntBitFieldOptions<MappedEnumType> options)
    {
        super(fieldEnum, connector);
        this.options = options;
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public Node getNode()
    {
        return mainPane;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final BitfieldPane bitfieldPane = initBitfieldPane();

        final boolean showUnmappedBits = options.getShowUnmappedBits();
        final int value = connector.getInt(fieldEnum);

        for (int i = 0, mask = 1; i < 32; ++i, mask <<= 1)
        {
            final MappedIntEnum mappedEnumType = options.getEnumFromValue(mask);

            if (mappedEnumType != null)
            {
                final String label = String.format("%s (%d)", mappedEnumType.getLabel(), i);
                bitfieldPane.setBitLabel(i, label);
            }
            else if (!showUnmappedBits)
            {
                continue;
            }

            if ((value & mask) != 0)
            {
                bitfieldPane.setBitChecked(i, true);
            }
        }

        bitfieldPane.setBitSelectionChangedListener(this::onBitSelectionChanged);
        bitfieldPane.setPadding(new Insets(10, 10, 10, 10));

        mainPane.setText(options.getLabel());
        mainPane.setContent(bitfieldPane);
    }

    private BitfieldPane initBitfieldPane()
    {
        ArrayList<Integer> shownBitIndices = null;
        if (!options.getShowUnmappedBits())
        {
            shownBitIndices = new ArrayList<>();
            int mask = 1;
            for (int i = 0; i < 32; ++i, mask <<= 1)
            {
                final MappedIntEnum mappedEnumType = options.getEnumFromValue(mask);
                if (mappedEnumType != null)
                {
                    shownBitIndices.add(i);
                }
            }
        }
        return new BitfieldPane(32, options.getNumBitsPerColumn(), shownBitIndices);
    }

    private void onBitSelectionChanged(final Integer bitIndex, final Boolean newValue)
    {
        final int mask = 1 << bitIndex;
        if (newValue)
        {
            connector.setInt(fieldEnum, connector.getInt(fieldEnum) | mask);
        }
        else
        {
            connector.setInt(fieldEnum, connector.getInt(fieldEnum) & ~mask);
        }
    }
}
