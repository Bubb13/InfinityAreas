
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.areapane.AreaPaneNormalEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.areapane.TrapRegionOptionsPane;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableActor;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // GUI
    private final CheckBox renderRegionsCheckbox = new CheckBox("Render Regions");

    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);

    private final StackPane rightPane = new StackPane();
    private Node curRightNode;

    private TrapRegionOptionsPane trapRegionOptionsPane = new TrapRegionOptionsPane(editor);

    private Area area;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public TrackedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetAreaTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void reset()
    {
        editor.enterEditMode(AreaPaneNormalEditMode.class);

        for (final Area.Actor actor : area.actors())
        {
            new RenderableActor(editor, actor);
        }

        for (final Area.Region region : area.regions())
        {
            if (region.getType() == 0 && region.getbTrapped() == 1)
            {
                new TrapRegion(region);
            }
        }
    }

    private void init()
    {
        ///////////////
        // Main HBox //
        ///////////////

            final HBox mainHBox = new HBox();

            ///////////////
            // Main VBox //
            ///////////////

            final VBox mainVBox = new VBox();
            mainVBox.setFocusTraversable(false);
            mainVBox.setPadding(new Insets(5, 0, 0, 10));

                //////////////////
                // Toolbar HBox //
                //////////////////

                final HBox toolbar = new HBox();
                toolbar.setPadding(new Insets(0, 0, 5, 0));

                final Button saveButton = new Button("Save");
                //saveButton.setOnAction((ignored) -> this.onSave());

                final Region padding1 = new Region();
                padding1.setPadding(new Insets(0, 0, 0, 5));

                final Button drawPolygonButton = new UnderlinedButton("Draw Polygon");
                drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

                final Region padding2 = new Region();
                padding2.setPadding(new Insets(0, 0, 0, 5));

                final Button bisectLine = new UnderlinedButton("Bisect Line");
                bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

                final Region padding3 = new Region();
                padding3.setPadding(new Insets(0, 0, 0, 5));

                final Button quickSelect = new UnderlinedButton("Quick Select");
                quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

                toolbar.getChildren().addAll(saveButton, padding1, drawPolygonButton,
                    padding2, bisectLine, padding3, quickSelect);

            VBox.setVgrow(zoomPane, Priority.ALWAYS);
            mainVBox.getChildren().addAll(toolbar, zoomPane);

            ////////////////////
            // Side Pane VBox //
            ////////////////////

            final VBox sidePaneVBox = new VBox();
            sidePaneVBox.setMinWidth(150);
            sidePaneVBox.setPadding(new Insets(5, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderRegionsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderRegionsChanged(newValue));

            sidePaneVBox.getChildren().addAll(renderRegionsCheckbox);

        changeRightNode(sidePaneVBox);
        mainHBox.getChildren().addAll(mainVBox, rightPane);
        getChildren().add(mainHBox);

        editor.registerEditMode(AreaPaneNormalEditMode.class, () -> new AreaPaneNormalEditMode(editor));
    }

    private void changeRightNode(final Node newNode)
    {
        if (newNode != curRightNode)
        {
            curRightNode = newNode;
            final ObservableList<Node> children = rightPane.getChildren();
            children.clear();
            children.add(newNode);
        }
    }

    private void onRenderRegionsChanged(final boolean newValue)
    {
        editor.requestDraw();
    }

    private void openTrapRegionPanel()
    {

    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class TrapRegion
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Area.Region region;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TrapRegion(final Area.Region region)
        {
            this.region = region;
            new TrapPolygon(region.getPolygon());
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        private class TrapPolygon extends RenderablePolygon<GenericPolygon>
        {
            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public TrapPolygon(final GenericPolygon polygon)
            {
                super(editor, polygon);
                setRenderFill(true);
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            @Override
            public boolean isEnabled()
            {
                return renderRegionsCheckbox.isSelected();
            }

            @Override
            public void onClicked(final MouseEvent mouseEvent)
            {
                trapRegionOptionsPane.setRegion(region);
                changeRightNode(trapRegionOptionsPane);
            }

            ///////////////////////
            // Protected Methods //
            ///////////////////////

            @Override
            protected Color getLineColor()
            {
                return Color.RED;
            }

            @Override
            protected void deleteBackingObject()
            {
                region.delete();
            }
        }
    }

    private class SetAreaTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Game.ResourceSource source;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SetAreaTask(final Game.ResourceSource source)
        {
            this.source = source;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void doTask() throws Exception
        {
            final TaskTrackerI tracker = getTracker();
            tracker.updateMessage("Processing area ...");
            tracker.updateProgress(0, 1);

            final Area area = new Area(source);
            area.load(getTracker());
            AreaPane.this.area = area;

            final WED.Graphics wedGraphics = area.newGraphics().getWedGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            editor.reset(image.getWidth(), image.getHeight());
            reset();

            waitForFxThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }
}
