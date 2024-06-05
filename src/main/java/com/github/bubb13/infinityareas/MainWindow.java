
package com.github.bubb13.infinityareas;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainWindow extends Application
{
    public static void main(final String[] args)
    {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage)
    {
        final VBox masterVBox = new VBox();
        masterVBox.setStyle("-fx-background-color: #C0C0C0");

        final StackPane rootPane = new StackPane();
        rootPane.getChildren().addAll(masterVBox);

        final Scene scene = new Scene(rootPane, rootPane.getPrefWidth(), rootPane.getPrefHeight());

        primaryStage.setTitle("InfinityAreas");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.setResizable(false);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception
    {
        super.stop();
    }
}
