
package com.github.bubb13.infinityareas.gui.control;

import com.sun.javafx.scene.control.VirtualScrollBar;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.control.skin.ListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class DynamicListView<T extends DynamicListView.Entry> extends ListView<T>
{
    ////////////
    // Fields //
    ////////////

    private Consumer<T> chooseConsumer;
    private boolean mouseOnlySelects;

    //////////////////
    // Constructors //
    //////////////////

    public DynamicListView(final ObservableList<T> items, boolean mouseOnlySelects)
    {
        super(items);
        //this.setSnapToPixel(true);
        this.setSkin(new DynamicListViewSkin(this));
        this.setCellFactory(new DynamicListViewCellFactory());
        this.mouseOnlySelects = mouseOnlySelects;
    }

    public DynamicListView(final ObservableList<T> items)
    {
        this(items, false);
        mouseOnlySelects = false;
    }

    /////////////
    // Methods //
    /////////////

    public void setOnChoose(final Consumer<T> consumer)
    {
        this.chooseConsumer = consumer;
    }

    // Only call after this has been added to a scene
    public void settle()
    {
        while (this.needsLayout())
        {
            this.applyCss();
            this.autosize();
            this.layout();
        }
    }

    private boolean needsLayout()
    {
        @SuppressWarnings("unchecked")
        final var skin = (DynamicListViewSkin)this.getSkin();
        return this.isNeedsLayout() || skin.needsLayout();
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public interface Entry
    {
        Color getRowColor(final boolean isEven);
        Color getTextColor(final boolean isEven);
        String getText();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class DynamicListViewSkin extends ListViewSkin<T>
    {
        // Ugly reflection to accurately calculate the desired width / height
        private final static Method getCellCountMethod;
        private final static Method getCellLengthMethod;
        private final static Method getMaxCellWidthMethod;
        private final static Field vbarField;
        private final static Field hbarField;
        static
        {
            try
            {
                getCellCountMethod = VirtualFlow.class.getDeclaredMethod("getCellCount");
                getCellCountMethod.setAccessible(true);

                getMaxCellWidthMethod = VirtualFlow.class.getDeclaredMethod("getMaxCellWidth", int.class);
                getMaxCellWidthMethod.setAccessible(true);

                getCellLengthMethod = VirtualFlow.class.getDeclaredMethod("getCellLength", int.class);
                getCellLengthMethod.setAccessible(true);

                vbarField = VirtualFlow.class.getDeclaredField("vbar");
                vbarField.setAccessible(true);

                hbarField = VirtualFlow.class.getDeclaredField("hbar");
                hbarField.setAccessible(true);
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        //////////////////
        // Constructors //
        //////////////////

        public DynamicListViewSkin(final ListView<T> control)
        {
            super(control);
        }

        /////////////
        // Methods //
        /////////////

        @Override
        public void install()
        {
            super.install();

            // To settle on the optimal dimensions once one of the scrollbars is eliminated
            final DynamicListView<?> skinnable = (DynamicListView<?>)this.getSkinnable();
            skinnable.widthProperty().addListener((observable, oldValue, newValue) -> skinnable.requestLayout());
            skinnable.heightProperty().addListener((observable, oldValue, newValue) -> skinnable.requestLayout());
            skinnable.focusedProperty().addListener(this::onFocusChanged);

            // Handle 'enter'
            skinnable.setOnKeyPressed(event ->
            {
                final Consumer<T> chooseConsumer = DynamicListView.this.chooseConsumer;
                if (event.getCode() == KeyCode.ENTER && chooseConsumer != null)
                {
                    chooseConsumer.accept(DynamicListView.this.getSelectionModel().getSelectedItem());
                }
            });
        }

        public boolean needsLayout()
        {
            return this.getVirtualFlow().isNeedsLayout();
        }

        @Override
        protected VirtualFlow<ListCell<T>> createVirtualFlow()
        {
            final VirtualFlow<ListCell<T>> flow = new VirtualFlow<>();
            //flow.setSnapToPixel(true);
            return flow;
        }

        @Override
        protected double computePrefWidth(
            final double height,
            final double topInset, final double rightInset,
            final double bottomInset, final double leftInset)
        {
            try
            {
                final VirtualFlow<?> flow = this.getVirtualFlow();

                final double maxCellWidth = (double)getMaxCellWidthMethod.invoke(flow, -1);

                final VirtualScrollBar vbar = (VirtualScrollBar)vbarField.get(flow);
                final double vbarWidth = vbar.isVisible() ? vbar.prefWidth(-1) : 0;

                return maxCellWidth + vbarWidth + 2;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected double computePrefHeight(
            final double width, final double topInset, final double rightInset,
            final double bottomInset, final double leftInset)
        {
            try
            {
                final VirtualFlow<?> flow = this.getVirtualFlow();

                final double prefLength = getPrefLength(flow);

                final VirtualScrollBar hbar = (VirtualScrollBar)hbarField.get(flow);
                final double hbarHeight = hbar.isVisible() ? hbar.prefHeight(-1) : 0;

                return prefLength + hbarHeight + 2;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        private void onFocusChanged(
            final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue)
        {
            final ListView<T> skinnable = this.getSkinnable();
            final MultipleSelectionModel<T> selectionModel = skinnable.getSelectionModel();
            if (newValue)
            {
                if (selectionModel.isEmpty())
                {
                    selectionModel.selectFirst();
                }
            }
        }

        private double getPrefLength(final VirtualFlow<?> flow)
        {
            try
            {
                double sum = 0.0;

                final int rows = (int)getCellCountMethod.invoke(flow);
                for (int i = 0; i < rows; i++) {
                    sum += (double)getCellLengthMethod.invoke(flow, i);
                }

                return sum;
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private class DynamicListViewCellSkin extends CellSkinBase<ListCell<T>>
    {
        //////////////////
        // Constructors //
        //////////////////

        public DynamicListViewCellSkin(final ListCell<T> control)
        {
            super(control);
        }

        /////////////
        // Methods //
        /////////////

        @Override
        protected double computePrefWidth(
            final double height, final double topInset, final double rightInset,
            final double bottomInset, final double leftInset)
        {
            return snapSizeX(super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset));
        }

        @Override
        protected double computePrefHeight(
            final double width, final double topInset, final double rightInset,
            final double bottomInset, final double leftInset)
        {
            return snapSizeY(super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset));
        }
    }

    private class DynamicListViewCellFactory
        implements Callback<ListView<T>, ListCell<T>>
    {
        /////////////
        // Methods //
        /////////////

        @Override
        public ListCell<T> call(final ListView<T> param)
        {
            final ListCell<T> listCell = new ListCell<>()
            {
                private void updateColor()
                {
                    final T item = this.getItem();

                    if (item != null)
                    {
                        if (isSelected())
                        {
                            setBackground(Background.fill(Color.web("#0096C9")));
                            setTextFill(Color.WHITE);
                        }
                        else
                        {
                            final boolean isEven = getIndex() % 2 == 0;
                            setBackground(Background.fill(item.getRowColor(isEven)));
                            setTextFill(item.getTextColor(isEven));
                        }
                    }
                    else
                    {
                        setBackground(Background.EMPTY);
                    }
                }

                @Override
                protected void updateItem(final T item, final boolean empty)
                {
                    super.updateItem(item, empty);
                    this.updateColor();

                    if (item != null)
                    {
                        setText(item.getText());
                    }
                    else
                    {
                        setText(null);
                    }
                }

                @Override
                public void updateSelected(boolean selected)
                {
                    super.updateSelected(selected);
                    updateColor();
                }
            };

            //listCell.setSnapToPixel(true);
            listCell.setSkin(new DynamicListViewCellSkin(listCell));

            // Handle left click
            listCell.setOnMouseClicked(event ->
            {
                getSelectionModel().select(listCell.getIndex());

                if (!DynamicListView.this.mouseOnlySelects && chooseConsumer != null)
                {
                    chooseConsumer.accept(listCell.getItem());
                }
            });

            return listCell;
        }
    }
}
