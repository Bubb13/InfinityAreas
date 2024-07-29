
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.pane.BitfieldPane;
import javafx.scene.Node;

public class IntBitfieldImplementation<FieldEnumType extends Enum<?>>
    extends FieldImplementation<FieldEnumType>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final BitfieldPane bitfieldPane = new BitfieldPane(32, 8);
    private final FieldOptions options;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public IntBitfieldImplementation(
        final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final FieldOptions options)
    {
        super(fieldEnum, connector);
        this.options = options;
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public Node getNode()
    {
        return bitfieldPane;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
//        // Create the popup
//        Popup popup = new Popup();
//
//        // Create a VBox to hold the popup content
//        VBox popupContent = new VBox();
//        popupContent.setStyle("-fx-background-color: white; -fx-padding: 10;");
//        popupContent.getChildren().add(new Button("This is a popup"));
//
//        // Create and apply the drop shadow effect
//        DropShadow dropShadow = new DropShadow();
//        dropShadow.setOffsetX(5);
//        dropShadow.setOffsetY(5);
//        dropShadow.setColor(Color.BLACK);
//        popupContent.setEffect(dropShadow);
//
//        // Add the VBox to the popup
//        popup.getContent().add(popupContent);
    }
}
