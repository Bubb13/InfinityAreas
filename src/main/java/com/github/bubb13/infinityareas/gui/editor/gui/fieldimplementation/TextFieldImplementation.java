
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.scene.control.TextField;

public class TextFieldImplementation<FieldEnumType extends Enum<?>> extends FieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final TextField textField = new TextField();
    private final TextFieldOptions options;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TextFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final TextFieldOptions options)
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
        textField.setText(connector.getString(fieldEnum));
        textField.textProperty().addListener((observable, oldValue, newValue) -> onValueChanged(newValue));
        setNode(textField);
    }

    private void onValueChanged(String newValue)
    {
        final int characterLimit = options.getCharacterLimit();
        if (characterLimit > 0 && newValue.length() > characterLimit)
        {
            newValue = newValue.substring(0, characterLimit);
            textField.setText(newValue);
        }
        connector.setString(fieldEnum, newValue);
    }
}
