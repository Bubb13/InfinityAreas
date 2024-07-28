
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.scene.control.TextField;

public class TextFieldImplementation<EnumType extends Enum<?>> extends FieldImplementation<EnumType>
{
    private final TextField textField = new TextField();
    private final TextFieldOptions options;

    public TextFieldImplementation(
        final EnumType fieldEnum, final Connector<EnumType> connector, final TextFieldOptions options)
    {
        super(fieldEnum, connector, options.getLabel());
        setNode(textField);

        this.options = options;

        textField.setText(connector.getString(fieldEnum));

        textField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            final int characterLimit = options.getCharacterLimit();
            if (characterLimit > 0 && newValue.length() > characterLimit)
            {
                newValue = newValue.substring(0, characterLimit);
                textField.setText(newValue);
            }

            connector.setString(fieldEnum, newValue);
        });
    }
}
