
package com.github.bubb13.infinityareas.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

public class QuadTree<ElementType>
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final int DEFAULT_DIVIDE_THRESHOLD = 10;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final Quadrant root;
    private final int divideElementThreshold;
    private final int noDivideWidthThreshold;
    private final HashMap<ElementHolder<ElementType>, ArrayList<SimpleLinkedList<ElementHolder<ElementType>>.Node>>
        elementHolderToNodes = new HashMap<>();

    private final ElementHolder<ElementType> tempHolder = new ElementHolder<>();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public QuadTree(
        final int topLeftX, final int topLeftY,
        final int bottomRightExclusiveX, final int bottomRightExclusiveY,
        final int minimumWidth, final int divideElementThreshold)
    {
        noDivideWidthThreshold = minimumWidth * 2;
        root = new Quadrant(topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY,
            calcNoDivideWidthThreshold(topLeftX, bottomRightExclusiveX)
        );
        this.divideElementThreshold = divideElementThreshold;
    }

    public QuadTree(
        final int topLeftX, final int topLeftY,
        final int bottomRightExclusiveX, final int bottomRightExclusiveY,
        final int minimumWidth)
    {
        this(topLeftX, topLeftY,
            bottomRightExclusiveX, bottomRightExclusiveY,
            minimumWidth, DEFAULT_DIVIDE_THRESHOLD);
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    /**
     * Adds the given element to the QuadTree.
     */
    public void add(
        final ElementType element,
        final int topLeftX, final int topLeftY, final int bottomRightExclusiveX, final int bottomRightExclusiveY)
    {
        remove(element);
        root.addElement(element, topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY);
    }

    public void add(final ElementType element, final Corners corners)
    {
        add(element,
            corners.topLeftX(), corners.topLeftY(),
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY()
        );
    }

    public void remove(final ElementType element)
    {
        tempHolder.element = element;
        final var nodesToRemove = elementHolderToNodes.get(tempHolder);
        if (nodesToRemove == null) return;

        for (final var node : nodesToRemove)
        {
            node.remove();
        }

        elementHolderToNodes.remove(tempHolder);
    }

    /**
     * Iterates the quadrants intersected by the given area, and executes the given
     * callback once for every unique element instance that is encountered.
     */
    public void iterateNear(
        final int topLeftX, final int topLeftY, final int bottomRightExclusiveX, final int bottomRightExclusiveY,
        final Consumer<ElementType> consumer)
    {
        final HashSet<ElementHolder<ElementType>> seen = new HashSet<>();
        root.iterate(topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY, seen, consumer);
    }

    public void iterateNear(final Corners corners, final Consumer<ElementType> consumer)
    {
        iterateNear(
            corners.topLeftX(), corners.topLeftY(),
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY(),
            consumer
        );
    }

    public IterateResult iterableNear(
        final int topLeftX, final int topLeftY, final int bottomRightExclusiveX, final int bottomRightExclusiveY)
    {
        final HashSet<ElementHolder<ElementType>> seen = new HashSet<>();
        root.iterate(topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY, seen, null);
        return new IterateResult(seen);
    }

    public IterateResult iterableNear(final Corners corners)
    {
        return iterableNear(
            corners.topLeftX(), corners.topLeftY(),
            corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY()
        );
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private boolean calcNoDivideWidthThreshold(final int topLeftX, final int bottomRightExclusiveX)
    {
        return bottomRightExclusiveX - topLeftX <= noDivideWidthThreshold;
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class Quadrant
    {
        // Essential
        private final int topLeftX;
        private final int topLeftY;
        private final int bottomRightExclusiveX;
        private final int bottomRightExclusiveY;

        private Quadrant topLeft;
        private Quadrant topRight;
        private Quadrant bottomLeft;
        private Quadrant bottomRight;
        private SimpleLinkedList<ElementHolder<ElementType>> leafElementHolders;

        // Cache
        private final boolean noDivide;
        private final boolean childrenNoDivide;
        private final int middleX;
        private final int middleY;
        private final boolean[] intersectedQuadrants = new boolean[QuadrantEnum.VALUES.length];

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public Quadrant(
            final int topLeftX, final int topLeftY,
            final int bottomRightExclusiveX, final int bottomRightExclusiveY,
            final boolean noDivide)
        {
            this.topLeftX = topLeftX;
            this.topLeftY = topLeftY;
            this.bottomRightExclusiveX = bottomRightExclusiveX;
            this.bottomRightExclusiveY = bottomRightExclusiveY;
            leafElementHolders = new SimpleLinkedList<>();

            // Cache whether this quadrant / its children can divide
            this.noDivide = noDivide;
            this.childrenNoDivide = calcNoDivideWidthThreshold(topLeftX, bottomRightExclusiveX);

            // Cache middle positions for quick quadrant decisions. Probably overzealous.
            middleX = topLeftX + (bottomRightExclusiveX - topLeftX) / 2;
            middleY = topLeftY + (bottomRightExclusiveY - topLeftY) / 2;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        /**
         * Adds the given element to the quadrant. If the quadrant is currently a leaf, and no
         * division rules are triggered, the element is simply added to the quadrant. If a division rule
         * is triggered, the quadrant splits into its four sub-quadrants, reassigns its elements to the
         * new sub-quadrants, and adds the element being added to its corresponding sub-quadrants.
         */
        public void addElement(
            final ElementType element,
            final int topLeftX, final int topLeftY, final int bottomRightExclusiveX, final int bottomRightExclusiveY)
        {
            addElement(new ElementHolder<>(topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY, element));
        }

        /**
         * If the quadrant is a leaf, simply iterates all elements held by the leaf. Otherwise, recursively iterates
         * all sub-quadrants intersected by the given area, and executes the given callback once for every unique
         * element instance that is encountered.
         */
        public void iterate(
            final int topLeftX, final int topLeftY, final int bottomRightExclusiveX, final int bottomRightExclusiveY,
            final HashSet<ElementHolder<ElementType>> seen, final Consumer<ElementType> consumer)
        {
            if (leafElementHolders == null)
            {
                // Not leaf, calculate the quadrants the given area intersects
                intersectQuadrants(topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY);

                // Iterate intersected quadrants
                for (int i = 1; i < intersectedQuadrants.length; ++i)
                {
                    if (!intersectedQuadrants[i]) continue;
                    final QuadrantEnum quadrantEnum = QuadrantEnum.VALUES[i];

                    // If null the quadrant hasn't been created yet (and has 0 elements)
                    final Quadrant quadrant = getQuadrantFromEnumError(quadrantEnum);
                    if (quadrant == null) continue;

                    // Calculate precise intersection
                    final Corners intersection = intersect(
                        topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY,
                        quadrant.topLeftX, quadrant.topLeftY,
                        quadrant.bottomRightExclusiveX, quadrant.bottomRightExclusiveY
                    );
                    assert intersection != null;

                    // Do iteration in quadrant
                    quadrant.iterate(intersection.topLeftX(), intersection.topLeftY(),
                        intersection.bottomRightExclusiveX(), intersection.bottomRightExclusiveY(),
                        seen, consumer);
                }
            }
            else
            {
                // Leaf (no sub-quadrants), iterate elements
                for (final ElementHolder<ElementType> elementHolder : leafElementHolders)
                {
                    if (seen.add(elementHolder) && consumer != null)
                    {
                        // Element not already seen, execute callback
                        consumer.accept(elementHolder.element);
                    }
                }
            }
        }

        /////////////////////
        // Private Methods //
        /////////////////////

//        private Quadrant getQuadrantFromEnum(final QuadrantEnum quadrantEnum)
//        {
//            return switch (quadrantEnum)
//            {
//                case TOP_LEFT -> topLeft;
//                case TOP_RIGHT -> topRight;
//                case BOTTOM_LEFT -> bottomLeft;
//                case BOTTOM_RIGHT -> bottomRight;
//                default -> null;
//            };
//        }

        /**
         * Create quadrant from enum, throwing error if passed QuadrantEnum.NONE
         */
        private Quadrant getQuadrantFromEnumError(final QuadrantEnum quadrantEnum)
        {
            return switch (quadrantEnum)
            {
                case TOP_LEFT -> topLeft;
                case TOP_RIGHT -> topRight;
                case BOTTOM_LEFT -> bottomLeft;
                case BOTTOM_RIGHT -> bottomRight;
                default -> throw new IllegalStateException();
            };
        }

        /**
         * Returns which quadrant the given (x,y) pair maps to.
         */
        private QuadrantEnum getQuadrant(final int x, final int y)
        {
            if (x < topLeftX || x >= bottomRightExclusiveX
                || y < topLeftY || y >= bottomRightExclusiveY)
            {
                return QuadrantEnum.NONE;
            }

            return x < middleX
                ? (y < middleY ? QuadrantEnum.TOP_LEFT : QuadrantEnum.BOTTOM_LEFT)
                : (y < middleY ? QuadrantEnum.TOP_RIGHT : QuadrantEnum.BOTTOM_RIGHT);
        }

        /**
         * Creates the top-left quadrant.
         */
        private Quadrant createTopLeftQuadrant()
        {
            topLeft = new Quadrant(topLeftX, topLeftY, middleX, middleY, childrenNoDivide);
            return topLeft;
        }

        /**
         * Creates the top-right quadrant.
         */
        private Quadrant createTopRightQuadrant()
        {
            topRight = new Quadrant(middleX, topLeftY, bottomRightExclusiveX, middleY, childrenNoDivide);
            return topRight;
        }

        /**
         * Creates the bottom-left quadrant.
         */
        private Quadrant createBottomLeftQuadrant()
        {
            bottomLeft = new Quadrant(topLeftX, middleY, middleX, bottomRightExclusiveY, childrenNoDivide);
            return bottomLeft;
        }

        /**
         * Creates the bottom-right quadrant.
         */
        private Quadrant createBottomRightQuadrant()
        {
            bottomRight = new Quadrant(middleX, middleY,
                bottomRightExclusiveX, bottomRightExclusiveY, childrenNoDivide
            );
            return bottomRight;
        }

//        private void createQuadrants()
//        {
//            createTopLeftQuadrant();
//            createTopRightQuadrant();
//            createBottomLeftQuadrant();
//            createBottomRightQuadrant();
//        }
//
//        private Quadrant createQuadrant(final QuadrantEnum quadrantEnum)
//        {
//            return switch (quadrantEnum)
//            {
//                case TOP_LEFT -> createTopLeftQuadrant();
//                case TOP_RIGHT -> createTopRightQuadrant();
//                case BOTTOM_LEFT -> createBottomLeftQuadrant();
//                case BOTTOM_RIGHT -> createBottomRightQuadrant();
//                default -> throw new IllegalStateException();
//            };
//        }

        /**
         * Gets or creates the given quadrant.
         */
        private Quadrant getOrCreateQuadrant(final QuadrantEnum quadrantEnum)
        {
            return switch (quadrantEnum)
            {
                case TOP_LEFT -> topLeft == null ? createTopLeftQuadrant() : topLeft;
                case TOP_RIGHT -> topRight == null ? createTopRightQuadrant() : topRight;
                case BOTTOM_LEFT -> bottomLeft == null ? createBottomLeftQuadrant() : bottomLeft;
                case BOTTOM_RIGHT -> bottomRight == null ? createBottomRightQuadrant() : bottomRight;
                default -> throw new IllegalStateException();
            };
        }

        /**
         * Determines which quadrants the given area intersects, and the stores the result in `intersectedQuadrants`.
         */
        private void intersectQuadrants(
            final int topLeftX, final int topLeftY,
            final int bottomRightExclusiveX, final int bottomRightExclusiveY)
        {
            for (int i = 0; i < QuadrantEnum.VALUES.length; ++i)
            {
                intersectedQuadrants[i] = false;
            }

            final Corners intersection = intersect(
                topLeftX, topLeftY, bottomRightExclusiveX, bottomRightExclusiveY,
                this.topLeftX, this.topLeftY, this.bottomRightExclusiveX, this.bottomRightExclusiveY);

            if (intersection == null) return;
            final int intersectTopLeftX = intersection.topLeftX();
            final int intersectTopLeftY = intersection.topLeftY();
            final int intersectBottomRightX = intersection.bottomRightExclusiveX() - 1;
            final int intersectBottomRightY = intersection.bottomRightExclusiveY() - 1;
            intersectedQuadrants[getQuadrant(intersectTopLeftX, intersectTopLeftY).ordinal()] = true;
            intersectedQuadrants[getQuadrant(intersectBottomRightX, intersectTopLeftY).ordinal()] = true;
            intersectedQuadrants[getQuadrant(intersectTopLeftX, intersectBottomRightY).ordinal()] = true;
            intersectedQuadrants[getQuadrant(intersectBottomRightX,
                intersectBottomRightY).ordinal()] = true;
        }

        /**
         * Adds the given element holder to each quadrant its area maps to. This should only be called on non-leafs.
         */
        private void addElementHolderToQuadrants(final ElementHolder<ElementType> elementHolder)
        {
            intersectQuadrants(elementHolder.topLeftX, elementHolder.topLeftY,
                elementHolder.bottomRightExclusiveX, elementHolder.bottomRightExclusiveY);

            for (int i = 1; i < intersectedQuadrants.length; ++i)
            {
                if (!intersectedQuadrants[i]) continue;
                final Quadrant quadrant = getOrCreateQuadrant(QuadrantEnum.VALUES[i]);
                quadrant.addElement(elementHolder);
            }
        }

        /**
         * Adds the given element holder to the quadrant. If the quadrant is currently a leaf, and no
         * division rules are triggered, the element is simply added to the quadrant. If a division rule
         * is triggered, the quadrant splits into its four sub-quadrants, reassigns its elements to the
         * new sub-quadrants, and adds the element being added to its corresponding sub-quadrants.
         */
        private void addElement(final ElementHolder<ElementType> elementHolder)
        {
            if (!noDivide && leafElementHolders != null && leafElementHolders.size() >= divideElementThreshold)
            {
                for (final ElementHolder<ElementType> leafElementHolder : leafElementHolders)
                {
                    addElementHolderToQuadrants(leafElementHolder);
                }

                leafElementHolders = null;
            }

            if (leafElementHolders == null)
            {
                addElementHolderToQuadrants(elementHolder);
            }
            else
            {
                final var nodes = elementHolderToNodes.computeIfAbsent(elementHolder, (ignored) -> new ArrayList<>());
                nodes.add(leafElementHolders.addTail(elementHolder));
            }
        }
    }

    /**
     * Calculates the intersection between two areas. Returns null if no intersection occurs.
     */
    private Corners intersect(
        final int topLeftX1, final int topLeftY1, final int bottomRightExclusiveX1, final int bottomRightExclusiveY1,
        final int topLeftX2, final int topLeftY2, final int bottomRightExclusiveX2, final int bottomRightExclusiveY2)
    {
        final int topLeftX = Math.max(topLeftX1, topLeftX2);
        final int bottomRightXExclusive = Math.min(bottomRightExclusiveX1, bottomRightExclusiveX2);
        if (topLeftX >= bottomRightXExclusive) return null;

        final int topLeftY = Math.max(topLeftY1, topLeftY2);
        final int bottomRightYExclusive = Math.min(bottomRightExclusiveY1, bottomRightExclusiveY2);
        if (topLeftY >= bottomRightYExclusive) return null;

        return new Corners(topLeftX, topLeftY, bottomRightXExclusive, bottomRightYExclusive);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class ElementHolder<ElementType>
    {
        private int topLeftX;
        private int topLeftY;
        private int bottomRightExclusiveX;
        private int bottomRightExclusiveY;
        private ElementType element;

        public ElementHolder(
            final int topLeftX, final int topLeftY,
            final int bottomRightExclusiveX, final int bottomRightExclusiveY,
            final ElementType element)
        {
            this.topLeftX = topLeftX;
            this.topLeftY = topLeftY;
            this.bottomRightExclusiveX = bottomRightExclusiveX;
            this.bottomRightExclusiveY = bottomRightExclusiveY;
            this.element = element;
        }

        public ElementHolder(final ElementType element)
        {
            this.element = element;
        }

        public ElementHolder() {}

        @Override
        public boolean equals(final Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ElementHolder<?> that = (ElementHolder<?>)o;
            return Objects.equals(element, that.element);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(element);
        }
    }

    private enum QuadrantEnum
    {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT;

        private static final QuadrantEnum[] VALUES = QuadrantEnum.values();
    }

    public class IterateResult implements Iterable<ElementType>
    {
        private final HashSet<ElementHolder<ElementType>> seen;

        private IterateResult(final HashSet<ElementHolder<ElementType>> seen)
        {
            this.seen = seen;
        }

        public Iterator<ElementType> iterator()
        {
            return new Iterator<>()
            {
                final Iterator<ElementHolder<ElementType>> iterator = seen.iterator();

                @Override
                public boolean hasNext()
                {
                    return iterator.hasNext();
                }

                @Override
                public ElementType next()
                {
                    return iterator.next().element;
                }
            };
        }
    }
}