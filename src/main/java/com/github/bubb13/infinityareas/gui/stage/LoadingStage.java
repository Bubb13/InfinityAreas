
package com.github.bubb13.infinityareas.gui.stage;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class LoadingStage extends Stage
{
    /////////////////////
    // Instance Fields //
    /////////////////////

    private StringProperty messageProperty = new SimpleStringProperty();
    private DoubleProperty progressProperty = new SimpleDoubleProperty();

    //////////////////
    // Constructors //
    //////////////////

    public LoadingStage(final Task<?> task)
    {
        init();
        bindToTask(task);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void bindToTask(final Task<?> task)
    {
        messageProperty.bind(task.messageProperty());
        progressProperty.bind(task.progressProperty());
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        initStyle(StageStyle.UNDECORATED);
        initModality(Modality.WINDOW_MODAL);

        final Label message;
        message = new Label("Infinity Areas is loading ...");
        message.setFont(Font.font(16));
        this.messageProperty = message.textProperty();

        final HBox titleHBox = new HBox();
        titleHBox.setPadding(new Insets(0, 5, 5, 5));
        titleHBox.getChildren().addAll(message);
        titleHBox.setAlignment(Pos.CENTER);

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefSize(300, 20);
        this.progressProperty = progressBar.progressProperty();

        final VBox mainVBox = new VBox();
        mainVBox.setPadding(new Insets(10, 10, 10, 10));
        mainVBox.getChildren().addAll(titleHBox, progressBar);

        final StackPane layout = new StackPane();
        layout.setBackground(Background.fill(Color.WHITE));
        layout.getChildren().addAll(mainVBox);

        final Scene scene = new Scene(layout);

        this.setScene(scene);
        this.sizeToScene();
        this.setResizable(false);

        // Force the window to the top of the window stack
        // Note: Stage::toFront() doesn't work for an unknown reason
        this.addEventHandler(WindowEvent.WINDOW_SHOWN, new EventHandler<>()
        {
            @Override
            public void handle(WindowEvent window)
            {
                LoadingStage.this.removeEventHandler(WindowEvent.WINDOW_SHOWN, this);
                LoadingStage.this.setAlwaysOnTop(true);
            }
        });
    }
}
