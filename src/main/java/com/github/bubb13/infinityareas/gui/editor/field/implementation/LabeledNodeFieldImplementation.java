
package com.github.bubb13.infinityareas.gui.editor.field.implementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.pane.LabeledNode;
import javafx.scene.Node;

public class LabeledNodeFieldImplementation<FieldEnumType extends Enum<?>> extends FieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final LabeledNode labeledNode;

    //////////////////////////////////
    // Package Private Constructors //
    //////////////////////////////////

    LabeledNodeFieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final String labelText)
    {
        super(fieldEnum, connector);
        labeledNode = new LabeledNode(labelText);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setNode(final Node node)
    {
        labeledNode.setNode(node);
    }

    @Override
    public Node getNode()
    {
        return labeledNode;
    }
}
