
package com.github.bubb13.infinityareas.util;

public final class MathUtil
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static double extendLeft(final double startX, final double length, final double degree)
    {
        final double radianLeft = degreeToRadianLeft(degree);
        final double leftCosine = Math.cos(radianLeft);
        return startX + length * leftCosine;
    }

    public static double extendRight(final double startX, final double length, final double degree)
    {
        final double radianRight = degreeToRadianRight(degree);
        final double rightCosine = Math.cos(radianRight);
        return startX + length * rightCosine;
    }

    public static double extendUp(final double startY, final double length, final double degree)
    {
        final double radianUp = degreeToRadianUp(degree);
        final double upSine = Math.sin(radianUp);
        return startY - length * upSine;
    }

    public static double extendDown(final double startY, final double length, final double degree)
    {
        final double radianDown = degreeToRadianDown(degree);

        final double downSine = Math.sin(radianDown);

        return startY - length * downSine;
    }

    public static double extendCornerLeft(final double startX, final double length, final double degree)
    {
        final double radianLeft = degreeToRadianLeft(degree);
        final double radianParallelLeft = degreeToParallelRadianLeft(degree);

        final double leftCosine = Math.cos(radianLeft);
        final double leftParallelCosine = Math.cos(radianParallelLeft);

        return startX + length * leftCosine + length * leftParallelCosine;
    }

    public static double extendCornerRight(final double startX, final double length, final double degree)
    {
        final double radianRight = degreeToRadianRight(degree);
        final double radianParallelRight = degreeToParallelRadianRight(degree);

        final double rightCosine = Math.cos(radianRight);
        final double rightParallelCosine = Math.cos(radianParallelRight);

        return startX + length * rightCosine + length * rightParallelCosine;
    }

    public static double extendCornerUp(final double startY, final double length, final double degree)
    {
        final double radianUp = degreeToRadianUp(degree);
        final double radianParallelUp = degreeToParallelRadianUp(degree);

        final double upSine = Math.sin(radianUp);
        final double upParallelSine = Math.sin(radianParallelUp);

        return startY - length * upSine - length * upParallelSine;
    }

    public static double extendCornerDown(final double startY, final double length, final double degree)
    {
        final double radianDown = degreeToRadianDown(degree);
        final double radianParallelDown = degreeToParallelRadianDown(degree);

        final double downSine = Math.sin(radianDown);
        final double downParallelSine = Math.sin(radianParallelDown);

        return startY - length * downSine - length * downParallelSine;
    }

    public static double degreeToRadianLeft(double degree)
    {
        if (degree >= 270)
            degree -= 180;
        else if (degree <= 90)
            degree += 180;

        return Math.toRadians(degree);
    }

    public static double degreeToRadianRight(double degree)
    {
        if (degree <= 270)
            if (degree >= 180)
                degree -= 180;
            else if (degree >= 90)
                degree += 180;

        return Math.toRadians(degree);
    }

    public static double degreeToRadianUp(double degree)
    {
        if (degree >= 180)
            degree -= 180;

        return Math.toRadians(degree);
    }

    public static double degreeToRadianDown(double degree)
    {
        if (degree < 180)
            degree += 180;

        return Math.toRadians(degree);
    }

    public static double degreeToParallelRadianLeft(final double degree)
    {
        return Math.toRadians(degree >= 180 ? degree - 90 : degree + 90);
    }

    public static double degreeToParallelRadianRight(double degree)
    {
        if (degree >= 270)
            degree -= 270;
        else if (degree >= 180)
            degree += 90;
        else if (degree >= 90)
            degree -= 90;
        else
            degree += 270;

        return Math.toRadians(degree);
    }

    public static double degreeToParallelRadianUp(double degree)
    {
        if (degree >= 270)
            degree -= 270;
        else if (degree >= 90)
            degree -= 90;
        else
            degree += 90;

        return Math.toRadians(degree);
    }

    public static double degreeToParallelRadianDown(double degree)
    {
        if (degree >= 270)
            degree -= 90;
        else if (degree >= 90)
            degree += 90;
        else
            degree += 270;

        return Math.toRadians(degree);
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private MathUtil() {}
}
