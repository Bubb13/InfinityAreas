
package com.github.bubb13.infinityareas.gui.dialog;

import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import javafx.application.Platform;
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
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void openAndWait(final String errorMessage, final Throwable throwable)
    {
        if (Platform.isFxApplicationThread())
        {
            doShowAndWait(errorMessage, throwable);
        }
        else
        {
            JavaFXUtil.waitForGuiThreadToExecute(() -> doShowAndWait(errorMessage, throwable));
        }
    }

    public static void openAndWait(final String errorMessage)
    {
        openAndWait(errorMessage, null);
    }

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

    private static void doShowAndWait(final String errorMessage, final Throwable throwable)
    {
        new ErrorAlert(errorMessage, throwable).showAndWait();
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private ErrorAlert(final String errorMessage, final Throwable throwable)
    {
        super(AlertType.ERROR);
        init(errorMessage, throwable);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(final String errorMessage, final Throwable throwable)
    {
        this.setTitle("Error");
        this.setHeaderText(null);
        this.setContentText(errorMessage);

        final VBox vbox = new VBox();
        final ObservableList<Node> vboxChildren = vbox.getChildren();

        final Label errorMessageLabel = new Label("Error Message:");
        errorMessageLabel.setFont(Font.font(16));
        errorMessageLabel.setPadding(new Insets(10, 10, 10, 0));

        final TextArea messageArea = new TextArea(errorMessage);
        messageArea.setPrefHeight(0);
        messageArea.setWrapText(false);
        messageArea.setEditable(false);

        vboxChildren.addAll(errorMessageLabel, messageArea);

        if (throwable != null)
        {
            final Label traceMessageLabel = new Label("Stack Trace:");
            traceMessageLabel.setFont(Font.font(16));
            traceMessageLabel.setPadding(new Insets(10, 10, 10, 0));

            final String traceMessage = MiscUtil.formatStackTrace(throwable);
            System.err.println(traceMessage);

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
