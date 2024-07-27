
package com.github.bubb13.infinityareas.misc;

public class DoublePointImpl implements ReadableDoublePoint, WritableDoublePoint
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private double x;
    private double y;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public DoublePointImpl() {}

    public DoublePointImpl(final double x, final double y)
    {
        this.x = x;
        this.y = y;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public double getX()
    {
        return x;
    }

    @Override
    public void setX(double x)
    {
        this.x = x;
    }

    @Override
    public double getY()
    {
        return y;
    }

    @Override
    public void setY(double y)
    {
        this.y = y;
    }
}
