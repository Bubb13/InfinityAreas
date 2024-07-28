
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.pane.LabeledNode;

public abstract class FieldImplementation<EnumType extends Enum<?>> extends LabeledNode
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final EnumType fieldEnum;
    protected final Connector<EnumType> connector;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    FieldImplementation(final EnumType fieldEnum, final Connector<EnumType> connector, final String labelText)
    {
        super(labelText);
        this.fieldEnum = fieldEnum;
        this.connector = connector;
    }
}
