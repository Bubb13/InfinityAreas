
package com.github.bubb13.infinityareas.gui.editor.editmode.areapane;

import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.pane.LabeledNode;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;

public class TrapRegionOptionsPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // Data
    private final Editor editor;
    private Area.Region region;

    private int specialTypeIndex = -1;

    // GUI
    private final TextField nameField = new TextField();
    private final ComboBox<TypeHolder> typeDropdown = new ComboBox<>();
    {
        typeDropdown.getItems().addAll(Arrays.stream(Type.values()).map((type) -> new TypeHolder(type.value)).toList());
    }

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TrapRegionOptionsPane(final Editor editor)
    {
        super();
        this.editor = editor;
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setRegion(final Area.Region region)
    {
        this.region = region;

        nameField.setText(region.getName());

        final ObservableList<TypeHolder> typeHolders = typeDropdown.getItems();
        if (specialTypeIndex != -1)
        {
            typeHolders.remove(specialTypeIndex);
            specialTypeIndex = -1;
        }

        final int type = region.getType();
        final int numTypeHolders = typeHolders.size();

        boolean selected = false;

        for (int i = 0; i < numTypeHolders; ++i)
        {
            final int typeHolderValue = typeHolders.get(i).value;

            if (type == typeHolderValue)
            {
                typeDropdown.getSelectionModel().select(i);
                selected = true;
                break;
            }
            else if (type < typeHolderValue)
            {
                typeHolders.add(i, new TypeHolder(type));
                typeDropdown.getSelectionModel().select(i);
                specialTypeIndex = i;
                selected = true;
                break;
            }
        }

        if (!selected)
        {
            typeHolders.add(new TypeHolder(type));
            typeDropdown.getSelectionModel().select(numTypeHolders);
            specialTypeIndex = numTypeHolders;
        }
    }

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

        final VBox mainVBox = new VBox(10);
        mainVBox.setFocusTraversable(false);

        // String name
        // short type
        // GenericPolygon polygon
        // int triggerValue
        // int cursorIndex               // CURSORS.BAM
        // String destAreaResref         // For type=2
        // String entranceNameInDestArea // For type=2
        // int flags
        // int infoStrref                // For type=1
        // short trapDetectionDifficulty
        // short trapDisarmDifficulty
        // short bTrapped                // 0 = No, 1 = Yes
        // short bTrapDetected           // 0 = No, 1 = Yes
        // short trapLaunchPointX
        // short trapLaunchPointY
        // String keyResref              // For type=?
        // String scriptResref           // For type=?
        // short activationPointX
        // short activationPointY

        ////////////////
        // Name Field //
        ////////////////

        final HBox nameFieldHBox = new LabeledNode("Name", nameField);

        ///////////////////
        // Type Dropdown //
        ///////////////////

        final HBox typeDropdownHBox = new LabeledNode("Type", typeDropdown);

        mainVBox.getChildren().addAll(nameFieldHBox, typeDropdownHBox);

        setPadding(new Insets(5, 5, 5, 5));
        getChildren().addAll(mainVBox);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class TypeHolder
    {
        ///////////////////
        // Public Fields //
        ///////////////////

        public final Type type;
        public final int value;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TypeHolder(final int value)
        {
            this.type = Type.fromValue(value);
            this.value = value;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public String toString()
        {
            return String.format("%s (%d)", type, value);
        }
    }

    private enum Type
    {
        PROXIMITY_TRIGGER(0, "Proximity trigger"),
        INFO_POINT(1, "Info point"),
        TRAVEL_REGION(2, "Travel region");

        public final int value;
        public final String label;

        //////////////////
        // Constructors //
        //////////////////

        Type(final Integer value, final String label)
        {
            this.value = value;
            this.label = label;
        }

        ///////////////////////////
        // Public Static Methods //
        ///////////////////////////

        public static Type fromValue(final int value)
        {
            return switch (value)
            {
                case 0 -> PROXIMITY_TRIGGER;
                case 1 -> INFO_POINT;
                case 2 -> TRAVEL_REGION;
                default -> null;
            };
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public String toString()
        {
            return label;
        }
    }
}
