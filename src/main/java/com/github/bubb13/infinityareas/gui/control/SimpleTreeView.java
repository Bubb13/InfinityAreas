
package com.github.bubb13.infinityareas.gui.control;

import com.sun.javafx.scene.control.behavior.BehaviorBase;
import com.sun.javafx.scene.control.behavior.CellBehaviorBase;
import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.FocusModel;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.skin.CellSkinBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class SimpleTreeView<T> extends TreeView<T>
{
    private Consumer<T> onActivate;

    public SimpleTreeView(final TreeItem<T> root)
    {
        super(root);
        this.setCellFactory(new SimpleTreeViewCellFactory<>());

        this.setOnKeyPressed(this::onKeyPressed);
    }

    public void setOnActivate(final Consumer<T> onActivate)
    {
        this.onActivate = onActivate;
    }

    private void onKeyPressed(final KeyEvent event)
    {
        if (event.getCode() == KeyCode.ENTER)
        {
            final SelectionModel<TreeItem<T>> selectionModel = this.getSelectionModel();
            final TreeItem<T> item = selectionModel.getSelectedItem();

            if (item == null)
            {
                return;
            }

            if (item.isLeaf())
            {
                if (onActivate != null)
                {
                    onActivate.accept(item.getValue());
                }
            }
            else
            {
                item.setExpanded(!item.isExpanded());
            }
        }
    }

    ///////////////////////////////
    // SimpleTreeViewCellFactory //
    ///////////////////////////////

    private static class SimpleTreeViewCellFactory<T> implements Callback<TreeView<T>, TreeCell<T>>
    {
        @Override
        public TreeCell<T> call(TreeView<T> param)
        {
            final TreeCell<T> treeCell = new TreeCell<>()
            {
                @Override
                protected void updateItem(T item, boolean empty)
                {
                    super.updateItem(item, empty);

                    if (item == null)
                    {
                        this.setText(null);
                    }
                    else
                    {
                        this.setText(item.toString());
                    }
                }
            };

            treeCell.setSkin(new SimpleTreeCellSkin<>(treeCell));
            return treeCell;
        }
    }

    ////////////////////////
    // SimpleTreeCellSkin //
    ////////////////////////

    /**
     * Default skin implementation for the {@link TreeCell} control.
     *
     * @see TreeCell
     * @since 9
     */
    private static class SimpleTreeCellSkin<T> extends CellSkinBase<TreeCell<T>>
    {
        /* *************************************************************************
         *                                                                         *
         * Static fields                                                           *
         *                                                                         *
         **************************************************************************/

        /*
         * This is rather hacky - but it is a quick workaround to resolve the
         * issue that we don't know maximum width of a disclosure node for a given
         * TreeView. If we don't know the maximum width, we have no way to ensure
         * consistent indentation for a given TreeView.
         *
         * To work around this, we create a single WeakHashMap to store a max
         * disclosureNode width per TreeView. We use WeakHashMap to help prevent
         * any memory leaks.
         *
         * RT-19656 identifies a related issue, which is that we may not provide
         * indentation to any TreeItems because we have not yet encountered a cell
         * which has a disclosureNode. Once we scroll and encounter one, indentation
         * happens in a displeasing way.
         */
        private static final Map<TreeView<?>, Double> maxDisclosureWidthMap = new WeakHashMap<>();

        /* *************************************************************************
         *                                                                         *
         * Private fields                                                          *
         *                                                                         *
         **************************************************************************/

        private boolean disclosureNodeDirty = true;
        private TreeItem<?> treeItem;
        private final BehaviorBase<TreeCell<T>> behavior;

        /* *************************************************************************
         *                                                                         *
         * Constructors                                                            *
         *                                                                         *
         **************************************************************************/

        /**
         * Creates a new TreeCellSkin instance, installing the necessary child
         * nodes into the Control children list, as
         * well as the necessary input mappings for handling key, mouse, etc events.
         *
         * @param control The control that this skin should be installed onto.
         */
        public SimpleTreeCellSkin(TreeCell<T> control)
        {
            super(control);

            // install default input map for the TreeCell control
            behavior = new SimpleTreeCellBehavior<>(control);
//        control.setInputMap(behavior.getInputMap());

            updateTreeItem();

            registerChangeListener(control.treeItemProperty(), e ->
            {
                updateTreeItem();
                disclosureNodeDirty = true;
                getSkinnable().requestLayout();
            });
            registerChangeListener(control.textProperty(), e -> getSkinnable().requestLayout());
        }

        /* *************************************************************************
         *                                                                         *
         * Properties                                                              *
         *                                                                         *
         **************************************************************************/

        /**
         * The amount of space to multiply by the treeItem.level to get the left
         * margin for this tree cell. This is settable from CSS
         */
        private DoubleProperty indent = null;

        public final void setIndent(double value)
        {
            indentProperty().set(value);
        }

        public final double getIndent()
        {
            return indent == null ? 10.0 : indent.get();
        }

        public final DoubleProperty indentProperty()
        {
            if (indent == null)
            {
                indent = new StyleableDoubleProperty(10.0)
                {
                    @Override
                    public Object getBean()
                    {
                        return SimpleTreeCellSkin.this;
                    }

                    @Override
                    public String getName()
                    {
                        return "indent";
                    }

                    @Override
                    public CssMetaData<TreeCell<?>, Number> getCssMetaData()
                    {
                        return StyleableProperties.INDENT;
                    }
                };
            }
            return indent;
        }

        /* *************************************************************************
         *                                                                         *
         * Public API                                                              *
         *                                                                         *
         **************************************************************************/

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose()
        {
            super.dispose();

            if (behavior != null)
            {
                behavior.dispose();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void updateChildren()
        {
            super.updateChildren();
            updateDisclosureNode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void layoutChildren(double x, final double y,
                                      double w, final double h)
        {
            // RT-25876: can not null-check here as this prevents empty rows from
            // being cleaned out.
            // if (treeItem == null) return;

            TreeView<T> tree = getSkinnable().getTreeView();
            if (tree == null) return;

            if (disclosureNodeDirty)
            {
                updateDisclosureNode();
                disclosureNodeDirty = false;
            }

            Node disclosureNode = getSkinnable().getDisclosureNode();

            int level = tree.getTreeItemLevel(treeItem);
            if (!tree.isShowRoot()) level--;
            double leftMargin = getIndent() * level;

            x += leftMargin;

            // position the disclosure node so that it is at the proper indent
            boolean disclosureVisible = disclosureNode != null && treeItem != null && !treeItem.isLeaf();

            final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
                maxDisclosureWidthMap.get(tree) : 18;   // RT-19656: default width of default disclosure node
            double disclosureWidth = defaultDisclosureWidth;

            if (disclosureVisible)
            {
                if (disclosureNode == null || disclosureNode.getScene() == null)
                {
                    updateChildren();
                }

                if (disclosureNode != null)
                {
                    disclosureWidth = disclosureNode.prefWidth(h);
                    if (disclosureWidth > defaultDisclosureWidth)
                    {
                        maxDisclosureWidthMap.put(tree, disclosureWidth);
                    }

                    double ph = disclosureNode.prefHeight(disclosureWidth);

                    disclosureNode.resize(disclosureWidth, ph);
                    positionInArea(disclosureNode, x, y,
                        disclosureWidth, ph, /*baseline ignored*/0,
                        HPos.CENTER, VPos.CENTER);
                }
            }

            // determine starting point of the graphic or cell node, and the
            // remaining width available to them
            final int padding = treeItem != null && treeItem.getGraphic() == null ? 0 : 3;
            x += disclosureWidth + padding;
            w -= (leftMargin + disclosureWidth + padding);

            // Rather ugly fix for RT-38519, where graphics are disappearing in
            // certain circumstances
            Node graphic = getSkinnable().getGraphic();
            if (graphic != null && !getChildren().contains(graphic))
            {
                getChildren().add(graphic);
            }

            layoutLabelInArea(x, y, w, h);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset)
        {
            double fixedCellSize = getFixedCellSize();
            if (fixedCellSize > 0)
            {
                return fixedCellSize;
            }

            double pref = super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset);
            Node d = getSkinnable().getDisclosureNode();
            return (d == null) ? pref : Math.max(d.minHeight(-1), pref);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset)
        {
            double fixedCellSize = getFixedCellSize();
            if (fixedCellSize > 0)
            {
                return fixedCellSize;
            }

            final TreeCell<T> cell = getSkinnable();

            final double pref = super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset);
            final Node d = cell.getDisclosureNode();
            final double prefHeight = (d == null) ? pref : Math.max(d.prefHeight(-1), pref);

            // RT-30212: TreeCell does not honor minSize of cells.
            // snapSize for RT-36460
            return snapSizeY(Math.max(cell.getMinHeight(), prefHeight));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset, double leftInset)
        {
            double fixedCellSize = getFixedCellSize();
            if (fixedCellSize > 0)
            {
                return fixedCellSize;
            }

            return super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset)
        {
            double labelWidth = super.computePrefWidth(height, topInset, rightInset, bottomInset, leftInset);

            double pw = snappedLeftInset() + snappedRightInset();

            TreeView<T> tree = getSkinnable().getTreeView();
            if (tree == null) return pw;

            if (treeItem == null) return pw;

            pw = labelWidth;

            // determine the amount of indentation
            int level = tree.getTreeItemLevel(treeItem);
            if (!tree.isShowRoot()) level--;
            pw += getIndent() * level;

            // include the disclosure node width
            Node disclosureNode = getSkinnable().getDisclosureNode();
            double disclosureNodePrefWidth = disclosureNode == null ? 0 : disclosureNode.prefWidth(-1);
            final double defaultDisclosureWidth = maxDisclosureWidthMap.containsKey(tree) ?
                maxDisclosureWidthMap.get(tree) : 0;
            pw += Math.max(defaultDisclosureWidth, disclosureNodePrefWidth);

            return pw;
        }

        private double getFixedCellSize()
        {
            TreeView<?> treeView = getSkinnable().getTreeView();
            return treeView != null ? treeView.getFixedCellSize() : Region.USE_COMPUTED_SIZE;
        }

        /* *************************************************************************
         *                                                                         *
         * Private implementation                                                  *
         *                                                                         *
         **************************************************************************/

        private void updateTreeItem()
        {
            treeItem = getSkinnable().getTreeItem();
        }

        private void updateDisclosureNode()
        {
            if (getSkinnable().isEmpty()) return;

            Node disclosureNode = getSkinnable().getDisclosureNode();
            if (disclosureNode == null) return;

            boolean disclosureVisible = treeItem != null && !treeItem.isLeaf();
            disclosureNode.setVisible(disclosureVisible);

            if (!disclosureVisible)
            {
                getChildren().remove(disclosureNode);
            }
            else if (disclosureNode.getParent() == null)
            {
                getChildren().add(disclosureNode);
                disclosureNode.toFront();
            }
            else
            {
                disclosureNode.toBack();
            }

            // RT-26625: [TreeView, TreeTableView] can lose arrows while scrolling
            // RT-28668: Ensemble tree arrow disappears
            if (disclosureNode.getScene() != null)
            {
                disclosureNode.applyCss();
            }
        }

        /* *************************************************************************
         *                                                                         *
         *                         Stylesheet Handling                             *
         *                                                                         *
         **************************************************************************/

        private static class StyleableProperties
        {
            private static final CssMetaData<TreeCell<?>, Number> INDENT =
                new CssMetaData<>("-fx-indent",
                    SizeConverter.getInstance(), 10.0)
                {

                    @Override
                    public boolean isSettable(TreeCell<?> n)
                    {
                        DoubleProperty p = ((SimpleTreeCellSkin<?>) n.getSkin()).indentProperty();
                        return p == null || !p.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(TreeCell<?> n)
                    {
                        final SimpleTreeCellSkin<?> skin = (SimpleTreeCellSkin<?>) n.getSkin();
                        return (StyleableProperty<Number>) skin.indentProperty();
                    }
                };

            private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

            static
            {
                final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<>(CellSkinBase.getClassCssMetaData());
                styleables.add(INDENT);
                STYLEABLES = Collections.unmodifiableList(styleables);
            }
        }

        /**
         * Returns the CssMetaData associated with this class, which may include the
         * CssMetaData of its superclasses.
         *
         * @return the CssMetaData associated with this class, which may include the
         * CssMetaData of its superclasses
         */
        public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData()
        {
            return StyleableProperties.STYLEABLES;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public List<CssMetaData<? extends Styleable, ?>> getCssMetaData()
        {
            return getClassCssMetaData();
        }
    }

    ////////////////////////////
    // SimpleTreeCellBehavior //
    ////////////////////////////

    private static class SimpleTreeCellBehavior<T> extends CellBehaviorBase<TreeCell<T>>
    {
        /***************************************************************************
         *                                                                         *
         * Constructors                                                            *
         *                                                                         *
         **************************************************************************/

        public SimpleTreeCellBehavior(final TreeCell<T> control)
        {
            super(control);
        }

        /***************************************************************************
         *                                                                         *
         * Private implementation                                                  *
         *                                                                         *
         **************************************************************************/

        @Override
        protected MultipleSelectionModel<TreeItem<T>> getSelectionModel()
        {
            return getCellContainer().getSelectionModel();
        }

        @Override
        protected FocusModel<TreeItem<T>> getFocusModel()
        {
            return getCellContainer().getFocusModel();
        }

        @Override
        protected TreeView<T> getCellContainer()
        {
            return getNode().getTreeView();
        }

        @Override
        protected void edit(TreeCell<T> cell)
        {
            TreeItem<T> treeItem = cell == null ? null : cell.getTreeItem();
            getCellContainer().edit(treeItem);
        }

        @Override
        protected void handleClicks(MouseButton button, int clickCount, boolean isAlreadySelected)
        {
            // handle editing, which only occurs with the primary mouse button
            TreeItem<T> treeItem = getNode().getTreeItem();
            if (button == MouseButton.PRIMARY)
            {
//                if (clickCount == 1 && isAlreadySelected)
//                {
//                    edit(getNode());
//                }
//                else if (clickCount == 1)
//                {
//                    // cancel editing
//                    edit(null);
//                }
//                else if (clickCount == 2 && treeItem.isLeaf())
//                {
//                    // attempt to edit
//                    edit(getNode());
//                }
//                else if (clickCount % 2 == 0)
//                {
//                    // try to expand/collapse branch tree item
//                    treeItem.setExpanded(!treeItem.isExpanded());
//                }

                if (treeItem.isLeaf())
                {
                    final SimpleTreeView<T> treeView = (SimpleTreeView<T>) this.getNode().getTreeView();
                    final Consumer<T> onActivate = treeView.onActivate;
                    if (onActivate != null)
                    {
                        onActivate.accept(treeItem.getValue());
                    }
                }
                else
                {
                    treeItem.setExpanded(!treeItem.isExpanded());
                }
            }
        }

        @Override
        protected boolean handleDisclosureNode(double x, double y)
        {
//            TreeCell<T> treeCell = getNode();
//            Node disclosureNode = treeCell.getDisclosureNode();
//            if (disclosureNode != null)
//            {
//                if (disclosureNode.getBoundsInParent().contains(x, y))
//                {
//                    if (treeCell.getTreeItem() != null)
//                    {
//                        treeCell.getTreeItem().setExpanded(!treeCell.getTreeItem().isExpanded());
//                    }
//                    return true;
//                }
//            }
            return false;
        }
    }
}
