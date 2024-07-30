
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.pane.BitfieldPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;

import java.util.ArrayList;

public class ByteBitFieldImplementation<FieldEnumType extends Enum<?>, MappedEnumType extends MappedByteEnum>
    extends FieldImplementation<FieldEnumType>
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final byte NUM_BITS = 8;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final MappedByteBitFieldOptions<MappedEnumType> options;
    private final TitledPane mainPane = new TitledPane();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ByteBitFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector,
        final MappedByteBitFieldOptions<MappedEnumType> options)
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
        final byte value = connector.getByte(fieldEnum);

        for (byte i = 0, mask = 1; i < NUM_BITS; ++i, mask <<= 1)
        {
            final MappedByteEnum mappedEnumType = options.getEnumFromValue(mask);

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
            byte mask = 1;
            for (int i = 0; i < NUM_BITS; ++i, mask <<= 1)
            {
                final MappedByteEnum mappedEnumType = options.getEnumFromValue(mask);
                if (mappedEnumType != null)
                {
                    shownBitIndices.add(i);
                }
            }
        }
        return new BitfieldPane(NUM_BITS, options.getNumBitsPerColumn(), shownBitIndices);
    }

    private void onBitSelectionChanged(final Integer bitIndex, final Boolean newValue)
    {
        final int mask = 1 << bitIndex;
        if (newValue)
        {
            connector.setByte(fieldEnum, (byte)(connector.getByte(fieldEnum) | mask));
        }
        else
        {
            connector.setByte(fieldEnum, (byte)(connector.getByte(fieldEnum) & ~mask));
        }
    }
}
