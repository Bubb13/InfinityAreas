
package com.github.bubb13.infinityareas.gui.dialog;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class WarningAlertTwoOptions extends Alert
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void openAndWait(
        final String warningMessage,
        final String okText, final Runnable okCallback,
        final String cancelText, final Runnable cancelCallback)
    {
        if (Platform.isFxApplicationThread())
        {
            doShowAndWait(warningMessage,
                okText, okCallback,
                cancelText, cancelCallback);
        }
        else
        {
            JavaFXUtil.waitForFxThreadToExecute(() -> doShowAndWait(warningMessage,
                okText, okCallback,
                cancelText, cancelCallback));
        }
    }

    ////////////////////////////
    // Private Static Methods //
    ////////////////////////////

    private static void doShowAndWait(
        final String warningMessage,
        final String okText, final Runnable okCallback,
        final String cancelText, final Runnable cancelCallback)
    {
        new WarningAlertTwoOptions(warningMessage,
            okText, okCallback,
            cancelText, cancelCallback
        ).showAndWait();
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private WarningAlertTwoOptions(
        final String warningMessage,
        final String okText, final Runnable okCallback,
        final String cancelText, final Runnable cancelCallback)
    {
        super(AlertType.WARNING);
        init(warningMessage,
            okText, okCallback,
            cancelText, cancelCallback
        );
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init(
        final String warningMessage,
        final String okText, final Runnable okCallback,
        final String cancelText, final Runnable cancelCallback)
    {
        this.setTitle("Warning");
        this.setHeaderText(null);
        this.setContentText(warningMessage);

        final VBox vbox = new VBox();
        final ObservableList<Node> vboxChildren = vbox.getChildren();

        final Label warningMessageLabel = new Label("Warning Message:");
        warningMessageLabel.setFont(Font.font(16));
        warningMessageLabel.setPadding(new Insets(10, 10, 10, 0));

        final TextArea messageArea = new TextArea(warningMessage);
        messageArea.setPrefHeight(200);
        messageArea.setWrapText(true);
        messageArea.setEditable(false);

        vboxChildren.addAll(warningMessageLabel, messageArea);

        // StackPane (not Pane) needed to make left inset functional
        final StackPane pane = new StackPane();
        pane.getChildren().addAll(vbox);
        pane.setPadding(new Insets(10, 10, 0, 10));

        final DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(pane);

        final ButtonType cancelButtonType = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        final ButtonType okButtonType = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().clear();
        dialogPane.getButtonTypes().addAll(okButtonType, cancelButtonType);

        final Button cancelButton = (Button)dialogPane.lookupButton(cancelButtonType);
        if (cancelCallback != null) cancelButton.setOnAction((ignored) -> cancelCallback.run());

        final Button okButton = (Button)dialogPane.lookupButton(okButtonType);
        if (okCallback != null) okButton.setOnAction((ignored) -> okCallback.run());

        final Stage stage = (Stage)dialogPane.getScene().getWindow();
        stage.setOnHiding((ignored) -> GlobalState.setFrontStage(null));
        GlobalState.setFrontStage(stage);
        JavaFXUtil.forceToFront(stage);
    }
}
