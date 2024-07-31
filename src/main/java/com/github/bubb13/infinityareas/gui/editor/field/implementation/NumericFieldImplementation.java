
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.awt.Toolkit;
import java.util.function.BiConsumer;

public abstract class NumericFieldImplementation<FieldEnumType extends Enum<?>>
    extends LabeledNodeFieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final TextField textField = new TextField();
    private final NumericFieldOptions options;
    private String beforeEditValue;

    //////////////////////
    // Protected Fields //
    //////////////////////

    protected BiConsumer<?, ?> connectedValueChangedListener;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public NumericFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final NumericFieldOptions options)
    {
        super(fieldEnum, connector, options.getLabel());
        this.options = options;
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public void disconnect()
    {
        connector.removeListener(fieldEnum, connectedValueChangedListener);
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

    abstract protected long getConnectedValue();
    abstract protected void setConnectedValue(final long newValue);
    abstract protected void addConnectedValueChangedListener();

    protected void onConnectedValueChanged(final long oldValue, final long newValue)
    {
        textField.setText(String.valueOf(newValue));
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        addConnectedValueChangedListener();
        final long currentConnectedValue = getConnectedValue();
        textField.setText(String.valueOf(currentConnectedValue));

        textField.focusedProperty().addListener(
            (observable, oldValue, newValue) ->
            {
                if (newValue) onFocusGained();
                else onFocusLost();
            });

        textField.setOnKeyPressed(this::onKeyPressed);

        setNode(textField);
    }

    private void onKeyPressed(final KeyEvent event)
    {
        if (event.getCode() == KeyCode.ENTER)
        {
            textField.getParent().requestFocus();
        }
    }

    private void onFocusGained()
    {
        beforeEditValue = textField.getText();
    }

    private void onFocusLost()
    {
        final String currentFieldValue = textField.getText().trim();

        if (currentFieldValue.isEmpty())
        {
            textField.setText(beforeEditValue);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        long newValueLong;
        try
        {
            newValueLong = Long.parseLong(currentFieldValue);
        }
        catch (NumberFormatException e)
        {
            textField.setText(beforeEditValue);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (newValueLong < options.getMinValue() || newValueLong > options.getMaxValue())
        {
            textField.setText(beforeEditValue);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        textField.setText(currentFieldValue);
        setConnectedValue(newValueLong);
    }
}
