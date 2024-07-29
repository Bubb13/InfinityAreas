
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import javafx.scene.Node;

public abstract class FieldImplementation<EnumType extends Enum<?>>
{
    //////////////////////
    // Protected Fields //
    //////////////////////

    protected final EnumType fieldEnum;
    protected final Connector<EnumType> connector;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    FieldImplementation(final EnumType fieldEnum, final Connector<EnumType> connector)
    {
        this.fieldEnum = fieldEnum;
        this.connector = connector;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public abstract Node getNode();

    public void disconnect() {}
}
