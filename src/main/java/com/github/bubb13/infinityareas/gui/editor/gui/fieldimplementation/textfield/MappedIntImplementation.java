
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.Arrays;

public class MappedIntImplementation
    <
    FieldEnumType extends Enum<?>,
    MappedEnumType extends MappedIntEnum
    >
    extends FieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final MappedIntFieldOptions<MappedEnumType> options;

    private int specialTypeIndex;

    private final ComboBox<TypeHolder> typeDropdown = new ComboBox<>();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public MappedIntImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector,
        final MappedIntFieldOptions<MappedEnumType> options)
    {
        super(fieldEnum, connector, options.getLabel());
        this.options = options;

        typeDropdown.getItems().addAll(
            Arrays.stream(options.getEnumValues()).map((type) -> new TypeHolder(type.getValue())).toList()
        );

        init();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final ObservableList<TypeHolder> typeHolders = typeDropdown.getItems();
        if (specialTypeIndex != -1)
        {
            typeHolders.remove(specialTypeIndex);
            specialTypeIndex = -1;
        }

        final int type = connector.getInt(fieldEnum);
        final int numTypeHolders = typeHolders.size();

        boolean selected = false;

        for (int i = 0; i < numTypeHolders; ++i)
        {
            final int typeHolderValue = typeHolders.get(i).value;

            if (type == typeHolderValue)
            {
                typeDropdown.getSelectionModel().select(i);
                selected = true;
                break;
            }
            else if (type < typeHolderValue)
            {
                typeHolders.add(i, new TypeHolder(type));
                typeDropdown.getSelectionModel().select(i);
                specialTypeIndex = i;
                selected = true;
                break;
            }
        }

        if (!selected)
        {
            typeHolders.add(new TypeHolder(type));
            typeDropdown.getSelectionModel().select(numTypeHolders);
            specialTypeIndex = numTypeHolders;
        }

        setNode(typeDropdown);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class TypeHolder
    {
        ///////////////////
        // Public Fields //
        ///////////////////

        public final MappedEnumType type;
        public final int value;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TypeHolder(final int value)
        {
            this.type = options.getEnumFromValue(value);
            this.value = value;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public String toString()
        {
            return String.format("%s (%d)", type.getLabel(), value);
        }
    }
}
