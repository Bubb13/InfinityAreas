
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

public class TextFieldOptions extends AbstractFieldOptions<TextFieldOptions>
{
    private int characterLimit;

    public int getCharacterLimit()
    {
        return characterLimit;
    }

    public TextFieldOptions characterLimit(final int characterLimit)
    {
        this.characterLimit = characterLimit;
        return this;
    }
}
