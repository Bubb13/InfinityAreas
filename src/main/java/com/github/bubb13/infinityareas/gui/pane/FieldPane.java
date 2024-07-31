
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.editor.field.FieldDefinition;
import com.github.bubb13.infinityareas.gui.editor.field.StructureDefinition;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.FieldImplementation;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;

public class FieldPane extends ScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final VBox mainVBox = new VBox(10);
    private final ArrayList<FieldImplementation<?>> fieldImplementations = new ArrayList<>();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public FieldPane()
    {
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public <FieldsEnum extends Enum<?>> void setStructure(
        final StructureDefinition<FieldsEnum> structureDefinition, final Connector<FieldsEnum> connector)
    {
        mainVBox.getChildren().clear();

        for (final FieldDefinition<FieldsEnum> fieldDefinition : structureDefinition.getFieldDefinitions())
        {
            final FieldImplementation<FieldsEnum> fieldImpl = fieldDefinition.getFieldType().createImplementation(
                fieldDefinition.getFieldEnum(), connector, fieldDefinition.getOptions());

            if (fieldImpl != null)
            {
                fieldImplementations.add(fieldImpl);
                mainVBox.getChildren().add(fieldImpl.getNode());
            }
        }
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    @Override
    protected double computeMinWidth(double height)
    {
        return computePrefWidth(-1);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        setFocusTraversable(false);
        focusedProperty().addListener((observable, oldValue, newValue) -> mainVBox.requestFocus());
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setMinViewportWidth(1); // Needed to assume vbar is shown during pref width calculation
        parentProperty().addListener((observable, oldValue, newValue) ->
        {
            if (newValue == null)
            {
                onDisconnect();
            }
        });

            ///////////////
            // Main VBox //
            ///////////////

            mainVBox.setPadding(new Insets(10, 10, 10, 10));
            mainVBox.setFocusTraversable(false);

        setContent(mainVBox);
    }

    public void onDisconnect()
    {
        for (final FieldImplementation<?> fieldImplementation : fieldImplementations)
        {
            fieldImplementation.disconnect();
        }
    }
}
