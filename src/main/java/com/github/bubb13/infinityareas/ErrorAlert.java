
package com.github.bubb13.infinityareas;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

public class ErrorAlert extends Alert
{
    public ErrorAlert(final String errorMessage)
    {
        super(AlertType.ERROR);
        init(errorMessage);
    }

    private void init(final String errorMessage)
    {
        this.setTitle("Error");
        this.setHeaderText(null);
        this.setContentText(errorMessage);

        final TextArea textArea = new TextArea(errorMessage);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        // StackPane (not Pane) needed to make left inset functional
        final StackPane pane = new StackPane();
        pane.getChildren().addAll(textArea);
        pane.setPadding(new Insets(10, 10, 0, 10));

        final DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(pane);
    }
}
