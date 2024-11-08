
package com.github.bubb13.infinityareas.gui.stage;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.gui.control.DynamicListView;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.util.FileUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class GamePickerStage extends Stage
{
    ///////////////////
    // Static Fields //
    ///////////////////

    private static final int MIN_WIDTH = 175;

    private static final SuggestedDirectory[] SUGGESTED_GAME_DIRECTORIES = new SuggestedDirectory[]
    {
        new SuggestedDirectory("Program Files (x86)/Steam/steamapps/common/Baldur's Gate Enhanced Edition", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/Steam/steamapps/common/Baldur's Gate II Enhanced Edition", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/Steam/steamapps/common/Icewind Dale Enhanced Edition", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/Steam/steamapps/common/Project P", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/GOG Galaxy/Games/Baldurs Gate", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/GOG Galaxy/Games/Baldurs Gate 2", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/GOG Galaxy/Games/Icewind Dale Complete", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/GOG Galaxy/Games/Icewind Dale 2", SuggestMode.PER_DRIVE),
        new SuggestedDirectory("Program Files (x86)/GOG Galaxy/Games/Planescape Torment", SuggestMode.PER_DRIVE),
    };

    /////////////////////
    // Instance Fields //
    /////////////////////

    private final ObservableList<SuggestedPathEntry> suggestedPaths = FXCollections
        .synchronizedObservableList(FXCollections.observableArrayList());

    private KeyFile pickedKeyFile;

    //////////////////
    // Constructors //
    //////////////////

    public GamePickerStage()
    {
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public KeyFile getPickedKeyFile()
    {
        return pickedKeyFile;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        findSuggestedGameDirectories();

        initStyle(StageStyle.DECORATED);
        initModality(Modality.WINDOW_MODAL);

        final Label title = new Label("Select a game directory");
        title.setFont(Font.font(16));

        final HBox titleHBox = new HBox();
        titleHBox.setPadding(new Insets(0, 5, 0, 5));
        titleHBox.getChildren().addAll(title);
        titleHBox.setAlignment(Pos.CENTER);

        final DynamicListView<SuggestedPathEntry> suggestedPathsView = new DynamicListView<>(suggestedPaths);
        //suggestedPathsView.setFocusTraversable(false);
        suggestedPathsView.setOnChoose((suggestedPathEntry) -> onSuggestedChosen(suggestedPathEntry.getPath()));

        final Button button = new Button();
        button.setText("Other");
        button.onActionProperty().set(this::onOtherButtonPressed);

        final HBox otherButtonHBox = new HBox();
        otherButtonHBox.setPadding(new Insets(5, 5, 0, 5));
        otherButtonHBox.getChildren().addAll(button);
        otherButtonHBox.setAlignment(Pos.CENTER);

        final VBox mainVBox = new VBox();
        mainVBox.setPadding(new Insets(5, 5, 5, 5));
        mainVBox.getChildren().addAll(titleHBox, suggestedPathsView, otherButtonHBox);
        //mainVBox.setFillWidth(false);
        //mainVBox.setSnapToPixel(false);

        final StackPane layout = new StackPane();
        layout.setBackground(Background.fill(Color.WHITE));
        layout.getChildren().addAll(mainVBox);

        final Scene scene = new Scene(layout);
        suggestedPathsView.settle();

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
                GamePickerStage.this.removeEventHandler(WindowEvent.WINDOW_SHOWN, this);
                GamePickerStage.this.setAlwaysOnTop(true);
                GamePickerStage.this.setAlwaysOnTop(false);
            }
        });

        this.setTitle("Infinity Areas");
    }

    private void tryLoadKeyFile(final Path path)
    {
        final KeyFile tempKeyFile = new KeyFile(path);
        tempKeyFile.loadTask()
            .trackWith(new LoadingStageTracker())
            .onSucceededFx(() ->
            {
                pickedKeyFile = tempKeyFile;
                this.close();
            })
            .onFailedFx((e) -> ErrorAlert.openAndWait("Exception when reading key file.", e))
            .start();
    }

    private void onSuggestedChosen(final Path path)
    {
        try
        {
            tryLoadKeyFile(FileUtil.resolveCaseInsensitiveElseError(path, "chitin.key",
                (errorPathStr) -> String.format("Key file does not exist: \"%s\"", errorPathStr)));
        }
        catch (final Exception e)
        {
            // Show error message
            ErrorAlert.openAndWait("Exception when reading key file: " + e.getMessage());
        }
    }

    private void onOtherButtonPressed(final ActionEvent event)
    {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Game Directory (chitin.key)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "Infinity Engine Key File",
            "*.key", // Silly case permutations for case-sensitive file systems
            "*.keY",
            "*.kEy",
            "*.kEY",
            "*.Key",
            "*.KeY",
            "*.KEy",
            "*.KEY"));

        GlobalState.pushModalStage(null);
        final File selectedFile = fileChooser.showOpenDialog(this);
        GlobalState.popModalStage(null);

        if (selectedFile != null)
        {
            final Path path = selectedFile.toPath();
            if (path.getFileName().toString().equalsIgnoreCase("chitin.key"))
            {
                tryLoadKeyFile(path);
            }
            else
            {
                ErrorAlert.openAndWait("Not a key file. Please navigate to chitin.key.");
            }
        }
    }

    private void checkSuggestedGameDirectory(final Path path)
    {
        try
        {
            final Path chitinPath = FileUtil.resolveCaseInsensitive(path, "chitin.key");
            if (chitinPath != null && Files.isRegularFile(chitinPath))
            {
                suggestedPaths.add(new SuggestedPathEntry(path));
            }
        }
        catch (final Exception ignored) {}
    }

    private void checkSuggestedGameDirectory(final String pathStr)
    {
        try
        {
            checkSuggestedGameDirectory(Path.of(pathStr));
        }
        catch (final Exception ignored) {}
    }

    private void checkSuggestedGameDirectory(final Path root, final String pathStr)
    {
        try
        {
            checkSuggestedGameDirectory(root.resolve(pathStr));
        }
        catch (final Exception ignored) {}
    }

    private void findSuggestedGameDirectories()
    {
        for (final SuggestedDirectory suggestedGameDirectory : SUGGESTED_GAME_DIRECTORIES)
        {
            if (suggestedGameDirectory.mode == SuggestMode.PER_DRIVE)
            {
                for (final Path driveRoot : FileSystems.getDefault().getRootDirectories())
                {
                    checkSuggestedGameDirectory(driveRoot, suggestedGameDirectory.path);
                }
            }
            else
            {
                checkSuggestedGameDirectory(suggestedGameDirectory.path);
            }
        }

        suggestedPaths.sort(Comparator.comparing(SuggestedPathEntry::getText));
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private enum SuggestMode
    {
        NORMAL, PER_DRIVE
    }

    private record SuggestedDirectory(String path, SuggestMode mode)
    {
        public SuggestedDirectory(final String path)
        {
            this(path, SuggestMode.NORMAL);
        }
    }

    private static class SuggestedPathEntry implements DynamicListView.Entry
    {
        private static final Color evenColor = Color.rgb(0xE6, 0xE6, 0xE6);
        private static final Color oddColor = Color.rgb(0xF2, 0xF2, 0xF2);
        private final Path path;
        private final String pathString;

        public SuggestedPathEntry(final Path path)
        {
            this.path = path.toAbsolutePath();
            this.pathString = path.toString();
        }

        @Override
        public Color getRowColor(final boolean isEven)
        {
            return isEven ? evenColor : oddColor;
        }

        @Override
        public Color getTextColor(final boolean isEven)
        {
            return Color.BLACK;
        }

        @Override
        public String getText()
        {
            return pathString;
        }

        public Path getPath()
        {
            return path;
        }
    }
}
