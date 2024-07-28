
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield;

public abstract class AbstractFieldOptions<ActualType extends AbstractFieldOptions<?>>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private String labelText;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    AbstractFieldOptions()
    {

    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public String getLabel()
    {
        return labelText;
    }

    public ActualType label(final String labelText)
    {
        this.labelText = labelText;
        //noinspection unchecked
        return (ActualType)this;
    }
}
