
package com.github.bubb13.infinityareas.util;

import com.github.bubb13.infinityareas.misc.IteratorToIterable;
import com.github.bubb13.infinityareas.misc.ReadOnlyIterator;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;

public final class MiscUtil
{
    public static Path findInfinityAreasRoot() throws URISyntaxException
    {
        final Path codePath = Paths.get(MiscUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        if (codePath.toString().endsWith(".jar"))
        {
            // .jar file parent directory
            return codePath.getParent();
        }
        else
        {
            // Working directory
            return Paths.get("").toAbsolutePath();
        }
    }

    public static <T> Iterable<T> readOnlyIterable(final Iterator<? extends T> iterator)
    {
        return new IteratorToIterable<>(new ReadOnlyIterator<>(iterator));
    }

    public static <T> Iterable<T> readOnlyIterable(final Iterable<? extends T> iterable)
    {
        return readOnlyIterable(iterable.iterator());
    }

    public static String formatStackTrace(final Throwable e)
    {
        final StringBuilder builder = new StringBuilder();

        final Throwable cause = e.getCause();
        builder.append(cause == null ? e : cause).append("\n");

        final StackTraceElement[] stackTraceElements = cause == null ? e.getStackTrace() : cause.getStackTrace();

        final String stackTrace = Arrays.stream(stackTraceElements)
            .map((element) -> "    " + element.toString())
            .collect(Collectors.joining("\n"));

        return builder.append(stackTrace).toString();
    }

    public static int packBytesIntoInt(final byte b3, final byte b2, final byte b1, final byte b0)
    {
        return ((int) b3 & 0xFF) << 24 | ((int) b2 & 0xFF) << 16 | ((int) b1 & 0xFF) << 8 | ((int) b0 & 0xFF);
    }

    public static short toUnsignedByte(final byte val)
    {
        return (short)(val & 0xFF);
    }

    public static int toUnsignedShort(final short val)
    {
        return val & 0xFFFF;
    }

    public static void printHierarchy(final Parent parent, final String indent, final String special)
    {
        System.out.println(indent + special + parent.getClass().getName());
        for (final Node child : parent.getChildrenUnmodifiable())
        {
            if (child instanceof Parent childParent)
            {
                printHierarchy(childParent, indent + "    ", "");
            }
            else
            {
                System.out.println(indent + "    " + child.getClass().getName());
            }
        }

        if (parent instanceof ScrollPane scrollPane)
        {
            final Node content = scrollPane.getContent();
            if (content instanceof Parent contentParent)
            {
                printHierarchy(contentParent, indent + "    ", "[CONTENT]: ");
            }
            else
            {
                System.out.println(indent + "    " + content.getClass().getName());
            }
        }
    }

    public static void printHierarchy(final Parent parent)
    {
        printHierarchy(parent, "", "");
    }

    public static int divideRoundUp(final int n, final int d)
    {
        final int temp = n / d;
        return n % d == 0 ? temp : temp + 1;
    }

    public static int multiplyByRatioRoundUp(final int n, final int r1, final int r2)
    {
        return new BigDecimal(n)
            .multiply(new BigDecimal(r1))
            .divide(new BigDecimal(r2), RoundingMode.CEILING)
            .intValueExact();
    }

    public static double distance(final int x1, final int y1, final int x2, final int y2)
    {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static double calculateAngle(final double x1, final double y1, final double x2, final double y2)
    {
        double degrees = Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
        return degrees < 0 ? degrees + 360 : degrees;
    }

    public static Rectangle2D getRectangleFromCornerPointsExpand(final Point2D p1, final Point2D p2, final int expand)
    {
        final double p1X = p1.getX();
        final double p1Y = p1.getY();
        final double p2X = p2.getX();
        final double p2Y = p2.getY();
        final double left = Math.min(p1X, p2X) - expand;
        final double top = Math.min(p1Y, p2Y) - expand;
        final double right = Math.max(p1X, p2X) + expand;
        final double bottom = Math.max(p1Y, p2Y) + expand;
        return new Rectangle2D(left, top, right - left, bottom - top);
    }

    public static Rectangle2D getRectangleFromCornerPoints(final Point2D p1, final Point2D p2)
    {
        return getRectangleFromCornerPointsExpand(p1, p2, 0);
    }

    public static double snapToNearest(final double value, final double multiple)
    {
        return Math.round(value / multiple) * multiple;
    }

    public static double snapToNearest(final double value, final double lower, final double upper)
    {
        final double distLower = Math.abs(value - lower);
        final double distUpper = Math.abs(value - upper);
        return distLower < distUpper ? lower : upper;
    }

    private MiscUtil() {}
}
