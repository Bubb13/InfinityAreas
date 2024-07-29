
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

import java.util.Arrays;

public class MappedShortImplementation
    <
    FieldEnumType extends Enum<?>,
    MappedEnumType extends MappedShortEnum
    >
    extends LabeledNodeFieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final MappedShortFieldOptions<MappedEnumType> options;
    private final ComboBox<TypeHolder> typeDropdown = new ComboBox<>();
    private int specialTypeIndex = -1;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public MappedShortImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector,
        final MappedShortFieldOptions<MappedEnumType> options)
    {
        super(fieldEnum, connector, options.getLabel());
        this.options = options;

        typeDropdown.getItems().addAll(
            Arrays.stream(options.getEnumValues()).map((type) -> new TypeHolder(type.getValue())).toList()
        );

        typeDropdown.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> onNewValue(newValue.value));

        init();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onNewValue(final short newValue)
    {
        connector.setShort(fieldEnum, newValue);
    }

    private void init()
    {
        final ObservableList<TypeHolder> typeHolders = typeDropdown.getItems();
        if (specialTypeIndex != -1)
        {
            typeHolders.remove(specialTypeIndex);
            specialTypeIndex = -1;
        }

        final short type = connector.getShort(fieldEnum);
        final int numTypeHolders = typeHolders.size();

        boolean selected = false;

        for (int i = 0; i < numTypeHolders; ++i)
        {
            final short typeHolderValue = typeHolders.get(i).value;

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
        public final short value;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TypeHolder(final short value)
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
