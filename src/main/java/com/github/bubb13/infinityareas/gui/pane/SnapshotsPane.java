
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.renderable.AbstractRenderable;
import com.github.bubb13.infinityareas.misc.InstanceHashMap;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class SnapshotsPane extends ScrollPane
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final int GAP = 50;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Editor editor;
    private final ObservableList<Editor.Snapshot> snapshots;
    private final InstanceHashMap<Editor.Snapshot, SnapshotHolder> snapshotToHolder = new InstanceHashMap<>();

    private final VBox mainVBox = new VBox(10)
    {
        @Override
        protected double computePrefWidth(double height)
        {
            return Editor.MAX_SNAPSHOT_SIDE + GAP;
        }
    };

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public SnapshotsPane(final Editor editor, final ObservableList<Editor.Snapshot> snapshots)
    {
        this.editor = editor;
        this.snapshots = snapshots;
        init();
    }

    ///////////////////////
    // Protected Methods //
    ///////////////////////

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
        snapshots.addListener((ListChangeListener<Editor.Snapshot>) c ->
        {
            while (c.next())
            {
                if (c.wasAdded())
                {
                    c.getAddedSubList().forEach(this::addSnapshot);
                }
                else if (c.wasRemoved())
                {
                    c.getRemoved().forEach(this::removeSnapshot);
                }
            }
        });

        setFocusTraversable(false);
        focusedProperty().addListener((observable, oldValue, newValue) -> mainVBox.requestFocus());
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setMinViewportWidth(1); // Needed to assume vbar is shown during pref width calculation

            ///////////////
            // Main VBox //
            ///////////////

            mainVBox.setPadding(new Insets(10, 10, 10, 10));
            mainVBox.setFocusTraversable(false);

                ////////////////////////////
                // Unhide All Button HBox //
                ////////////////////////////

                final HBox unhideAllHBox = new HBox();

                    ///////////////////////
                    // Unhide All Button //
                    ///////////////////////

                        final Button unhideAllButton = new Button("Unhide All Of Below");
                        unhideAllButton.prefWidthProperty().bind(unhideAllHBox.widthProperty());
                        unhideAllButton.setOnAction((ignored) -> this.onUnhideAll());

                unhideAllHBox.getChildren().addAll(unhideAllButton);

            mainVBox.getChildren().addAll(unhideAllHBox);

        setContent(mainVBox);
    }

    private void addSnapshot(final Editor.Snapshot snapshot)
    {
        final SnapshotHolder snapshotHolder = new SnapshotHolder(snapshot);
        snapshotToHolder.put(snapshot, snapshotHolder);
        mainVBox.getChildren().add(snapshotHolder);
        setVvalue(0);
    }

    private void removeSnapshot(final Editor.Snapshot snapshot)
    {
        mainVBox.getChildren().remove(snapshotToHolder.get(snapshot));
        snapshotToHolder.remove(snapshot);
        setVvalue(0);
    }

    private void onUnhideAll()
    {
        for (final Editor.Snapshot snapshot : snapshots)
        {
            final AbstractRenderable renderable = snapshot.renderable();
            if (renderable.isHidden())
            {
                renderable.setHidden(false);
            }
        }
        editor.requestDraw();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class SnapshotHolder extends HBox
    {
        ///////////////////////////
        // Private Static Fields //
        ///////////////////////////

        private static final Border VISIBLE_BORDER = Border.stroke(Color.BLACK);
        private static final Border HIDDEN_BORDER = Border.stroke(Color.MAGENTA);

        ////////////////////
        // Private Fields //
        ////////////////////

        private final Editor.Snapshot snapshot;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SnapshotHolder(final Editor.Snapshot snapshot)
        {
            this.snapshot = snapshot;
            init();
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void init()
        {
            final VBox vbox = new VBox();
            vbox.setPadding(new Insets(10, 10, 10, 10));
            vbox.getChildren().add(snapshot.canvas());

            setAlignment(Pos.CENTER);
            setOnMouseClicked(this::onMouseClicked);
            updateBorder(snapshot.renderable().isHidden());
            getChildren().add(vbox);
        }

        private void onMouseClicked(final MouseEvent event)
        {
            if (event.getButton() != MouseButton.PRIMARY)
            {
                return;
            }

            final AbstractRenderable renderable = snapshot.renderable();
            final boolean newHiddenValue = !renderable.isHidden();
            renderable.setHidden(newHiddenValue);
            updateBorder(newHiddenValue);
            editor.requestDraw();
        }

        private void updateBorder(final boolean isHidden)
        {
            setBorder(isHidden ? HIDDEN_BORDER : VISIBLE_BORDER);
        }
    }
}
