
package com.github.bubb13.infinityareas;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class ErrorAlert extends Alert
{
    public ErrorAlert(final String errorMessage)
    {
        super(AlertType.ERROR);
        init(errorMessage, null);
    }

    public ErrorAlert(final String errorMessage, final Throwable throwable)
    {
        super(AlertType.ERROR);
        final String traceMessage = MiscUtil.formatStackTrace(throwable);
        System.err.println(traceMessage);
        init(errorMessage, traceMessage);
    }

    private void init(final String errorMessage, final String traceMessage)
    {
        this.setTitle("Error");
        this.setHeaderText(null);
        this.setContentText(errorMessage);

        final VBox vbox = new VBox();
        final ObservableList<Node> vboxChildren = vbox.getChildren();

        final Label errorMessageLabel = new Label("Error message:");
        errorMessageLabel.setFont(Font.font(16));
        errorMessageLabel.setPadding(new Insets(10, 10, 10, 0));

        final TextArea messageArea = new TextArea(errorMessage);
        messageArea.setPrefHeight(0);

        vboxChildren.addAll(errorMessageLabel, messageArea);

        if (traceMessage != null)
        {
            final Label traceMessageLabel = new Label("Stack traceback:");
            traceMessageLabel.setFont(Font.font(16));
            traceMessageLabel.setPadding(new Insets(10, 10, 10, 0));

            final TextArea traceArea = new TextArea(traceMessage);
            traceArea.setEditable(false);
            vboxChildren.addAll(traceMessageLabel, traceArea);
        }

        // StackPane (not Pane) needed to make left inset functional
        final StackPane pane = new StackPane();
        pane.getChildren().addAll(vbox);
        pane.setPadding(new Insets(10, 10, 0, 10));

        final DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(pane);
    }
}
