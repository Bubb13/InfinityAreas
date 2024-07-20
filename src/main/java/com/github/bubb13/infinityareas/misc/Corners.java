
package com.github.bubb13.infinityareas.misc;

import java.awt.Point;

public class Corners
{
    int topLeftX;
    int topLeftY;
    int bottomRightExclusiveX;
    int bottomRightExclusiveY;

    public Corners(
        final int topLeftX, final int topLeftY,
        final int bottomRightExclusiveX, final int bottomRightExclusiveY)
    {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightExclusiveX = bottomRightExclusiveX;
        this.bottomRightExclusiveY = bottomRightExclusiveY;
    }

    public Corners()
    {
        this(0, 0, 0, 0);
    }

    public int topLeftX()
    {
        return topLeftX;
    }

    public void setTopLeftX(int topLeftX)
    {
        this.topLeftX = topLeftX;
    }

    public int topLeftY()
    {
        return topLeftY;
    }

    public void setTopLeftY(int topLeftY)
    {
        this.topLeftY = topLeftY;
    }

    public int bottomRightExclusiveX()
    {
        return bottomRightExclusiveX;
    }

    public void setBottomRightExclusiveX(int bottomRightExclusiveX)
    {
        this.bottomRightExclusiveX = bottomRightExclusiveX;
    }

    public int bottomRightExclusiveY()
    {
        return bottomRightExclusiveY;
    }

    public void setBottomRightExclusiveY(int bottomRightExclusiveY)
    {
        this.bottomRightExclusiveY = bottomRightExclusiveY;
    }

    /**
     * Calculates the intersection between two Corners. Returns null if no intersection occurs.
     */
    public Corners intersect(final Corners otherCorners)
    {
        final int topLeftX1 = this.topLeftX();
        final int topLeftY1 = this.topLeftY();
        final int bottomRightExclusiveX1 = this.bottomRightExclusiveX();
        final int bottomRightExclusiveY1 = this.bottomRightExclusiveY();

        final int topLeftX2 = otherCorners.topLeftX();
        final int topLeftY2 = otherCorners.topLeftY();
        final int bottomRightExclusiveX2 = otherCorners.bottomRightExclusiveX();
        final int bottomRightExclusiveY2 = otherCorners.bottomRightExclusiveY();

        final int topLeftX = Math.max(topLeftX1, topLeftX2);
        final int bottomRightXExclusive = Math.min(bottomRightExclusiveX1, bottomRightExclusiveX2);
        if (topLeftX >= bottomRightXExclusive) return null;

        final int topLeftY = Math.max(topLeftY1, topLeftY2);
        final int bottomRightYExclusive = Math.min(bottomRightExclusiveY1, bottomRightExclusiveY2);
        if (topLeftY >= bottomRightYExclusive) return null;

        return new Corners(topLeftX, topLeftY, bottomRightXExclusive, bottomRightYExclusive);
    }

    public boolean contains(final Point point, final int fudgeAmount)
    {
        return point.x >= topLeftX - fudgeAmount && point.x < bottomRightExclusiveX + fudgeAmount
            && point.y >= topLeftY - fudgeAmount && point.y < bottomRightExclusiveY + fudgeAmount;
    }

    public boolean contains(final Point point)
    {
        return contains(point, 0);
    }
}
