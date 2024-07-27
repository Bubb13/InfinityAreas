
package com.github.bubb13.infinityareas.misc;

public class IntPointImpl implements IntPoint
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private int x;
    private int y;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public IntPointImpl(final int x, final int y)
    {
        this.x = x;
        this.y = y;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    @Override
    public int getX()
    {
        return x;
    }

    @Override
    public void setX(int x)
    {
        this.x = x;
    }

    @Override
    public int getY()
    {
        return y;
    }

    @Override
    public void setY(int y)
    {
        this.y = y;
    }
}
