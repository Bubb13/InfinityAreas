
package com.github.bubb13.infinityareas.gui.editor.gui;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.FieldImplementation;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class FieldPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final Editor editor;
    private final VBox mainVBox = new VBox(10);

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public FieldPane(final Editor editor)
    {
        super();
        this.editor = editor;
        init();
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
        ///////////////
        // Main VBox //
        ///////////////

        mainVBox.setFocusTraversable(false);
        setPadding(new Insets(5, 5, 5, 5));
        getChildren().addAll(mainVBox);
    }

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
                mainVBox.getChildren().add(fieldImpl);
            }
        }
    }
}
