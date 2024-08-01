
package com.github.bubb13.infinityareas.misc;

import javafx.geometry.Point2D;

public class DoubleCorners
{
    private double topLeftX;
    private double topLeftY;
    private double bottomRightExclusiveX;
    private double bottomRightExclusiveY;

    public DoubleCorners(
        final double topLeftX, final double topLeftY,
        final double bottomRightExclusiveX, final double bottomRightExclusiveY)
    {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightExclusiveX = bottomRightExclusiveX;
        this.bottomRightExclusiveY = bottomRightExclusiveY;
    }

    public DoubleCorners()
    {
        this(0, 0, 0, 0);
    }

    public double topLeftX()
    {
        return topLeftX;
    }

    public void setTopLeftX(final double topLeftX)
    {
        this.topLeftX = topLeftX;
    }

    public double topLeftY()
    {
        return topLeftY;
    }

    public void setTopLeftY(final double topLeftY)
    {
        this.topLeftY = topLeftY;
    }

    public double bottomRightExclusiveX()
    {
        return bottomRightExclusiveX;
    }

    public void setBottomRightExclusiveX(final double bottomRightExclusiveX)
    {
        this.bottomRightExclusiveX = bottomRightExclusiveX;
    }

    public double bottomRightExclusiveY()
    {
        return bottomRightExclusiveY;
    }

    public void setBottomRightExclusiveY(final double bottomRightExclusiveY)
    {
        this.bottomRightExclusiveY = bottomRightExclusiveY;
    }

    /**
     * Calculates the intersection between two Corners. Returns null if no intersection occurs.
     */
    public DoubleCorners intersect(final DoubleCorners otherCorners)
    {
        final double topLeftX1 = this.topLeftX();
        final double topLeftY1 = this.topLeftY();
        final double bottomRightExclusiveX1 = this.bottomRightExclusiveX();
        final double bottomRightExclusiveY1 = this.bottomRightExclusiveY();

        final double topLeftX2 = otherCorners.topLeftX();
        final double topLeftY2 = otherCorners.topLeftY();
        final double bottomRightExclusiveX2 = otherCorners.bottomRightExclusiveX();
        final double bottomRightExclusiveY2 = otherCorners.bottomRightExclusiveY();

        final double topLeftX = Math.max(topLeftX1, topLeftX2);
        final double bottomRightXExclusive = Math.min(bottomRightExclusiveX1, bottomRightExclusiveX2);
        if (topLeftX >= bottomRightXExclusive) return null;

        final double topLeftY = Math.max(topLeftY1, topLeftY2);
        final double bottomRightYExclusive = Math.min(bottomRightExclusiveY1, bottomRightExclusiveY2);
        if (topLeftY >= bottomRightYExclusive) return null;

        return new DoubleCorners(topLeftX, topLeftY, bottomRightXExclusive, bottomRightYExclusive);
    }

    public boolean contains(final Point2D point, final double fudgeAmount)
    {
        return point.getX() >= topLeftX - fudgeAmount && point.getX() < bottomRightExclusiveX + fudgeAmount
            && point.getY() >= topLeftY - fudgeAmount && point.getY()< bottomRightExclusiveY + fudgeAmount;
    }

    public boolean contains(final Point2D point)
    {
        return contains(point, 0);
    }

    public Point2D getCenter()
    {
        return new Point2D((topLeftX + bottomRightExclusiveX) / 2, (topLeftY + bottomRightExclusiveY) / 2);
    }
}
