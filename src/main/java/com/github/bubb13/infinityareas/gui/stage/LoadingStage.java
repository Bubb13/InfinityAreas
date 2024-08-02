
package com.github.bubb13.infinityareas.gui.stage;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LoadingStage extends Stage
{
    /////////////////////
    // Instance Fields //
    /////////////////////

    private StringProperty messageProperty;
    private DoubleProperty progressProperty;

    //////////////////
    // Constructors //
    //////////////////

    public LoadingStage(final Task<?> task)
    {
        init();
        bindToTask(task);
    }

    public LoadingStage()
    {
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void bindToTask(final Task<?> task)
    {
        messageProperty.bind(task.messageProperty());
        progressProperty.bind(task.progressProperty());
    }

    public void bind(final StringProperty messageProperty, final DoubleProperty progressProperty)
    {
        this.messageProperty.bind(messageProperty);
        this.progressProperty.bind(progressProperty);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        GlobalState.pushModalStage(this);
        initStyle(StageStyle.UNDECORATED);

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

        JavaFXUtil.forceToFront(this);
    }

    @Override
    public void hide()
    {
        super.hide();
        GlobalState.popModalStage(this);
    }
}
