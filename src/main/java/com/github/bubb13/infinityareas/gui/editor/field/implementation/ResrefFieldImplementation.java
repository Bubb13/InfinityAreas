
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

public class ResrefFieldImplementation<FieldEnumType extends Enum<?>>
    extends LabeledNodeFieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ComboBox<ResourceHolder> dropdown = new ComboBox<>();
    private final ResrefFieldOptions options;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ResrefFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final ResrefFieldOptions options)
    {
        super(fieldEnum, connector, options.getLabel());
        this.options = options;
        init();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final ObservableList<ResourceHolder> dropdownItems = dropdown.getItems();
        final KeyFile.NumericResourceType[] resourceTypes = options.getResourceTypes();

        final String currentFieldValue = connector.getString(fieldEnum);

        int i = -1;

        dropdownItems.add(new ResourceHolder("<BLANK>", null));
        ++i;

        int selectIndex = 0;
        for (final KeyFile.NumericResourceType resourceType : resourceTypes)
        {
            for (final Game.Resource resource : GlobalState.getGame().getResourcesOfType(resourceType))
            {
                if (resource.getPrimarySource() != null)
                {
                    ++i;

                    dropdownItems.add(new ResourceHolder(
                        resource.getIdentifier().resref() + "." + resourceType.getExtension(),
                        resource));

                    if (selectIndex == 0 && resource.getIdentifier().resref().equalsIgnoreCase(currentFieldValue))
                    {
                        selectIndex = i;
                    }
                }
            }
        }

        dropdown.getSelectionModel().select(selectIndex);

        dropdown.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> onValueChanged(newValue));

        setNode(dropdown);
    }

    private void onValueChanged(ResourceHolder newValue)
    {
        final Game.Resource resource = newValue.resource();
        connector.setString(fieldEnum, resource == null ? "" : resource.getIdentifier().resref());
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private record ResourceHolder(String label, Game.Resource resource)
    {
        @Override
        public String toString()
        {
            return label;
        }
    }
}
