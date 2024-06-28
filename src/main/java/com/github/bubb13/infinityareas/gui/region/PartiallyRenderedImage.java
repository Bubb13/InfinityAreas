
package com.github.bubb13.infinityareas.gui.region;

import com.github.bubb13.infinityareas.gui.misc.VisibleNotifiable;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class PartiallyRenderedImage extends Region implements VisibleNotifiable
{
    private final Canvas canvas = new Canvas();
    private BufferedImage image;
    private double zoomFactor = 1;

    public PartiallyRenderedImage()
    {
        getChildren().add(canvas);
    }

    public void setImage(final BufferedImage image)
    {
        this.image = image;
        requestLayout();
    }

    public void setZoomFactor(double zoomFactor)
    {
        this.zoomFactor = zoomFactor;
        requestLayout();
    }

    public BufferedImage getImage()
    {
        return image;
    }

    @Override
    protected double computePrefWidth(double height)
    {
        return image == null ? 0 : image.getWidth() * zoomFactor;
    }

    @Override
    protected double computePrefHeight(double width)
    {
        return image == null ? 0 : image.getHeight() * zoomFactor;
    }

    @Override
    protected void layoutChildren()
    {
        super.layoutChildren();
        draw();
    }

    private void draw()
    {
        final Bounds layout = canvas.getBoundsInParent();
        final int renderX = (int)layout.getMinX();
        final int renderY = (int)layout.getMinY();
        final int renderW = (int)layout.getWidth();
        final int renderH = (int)layout.getHeight();

        if (renderX < 0 || renderY < 0 || renderW <= 0 || renderH <= 0)
        {
            return;
        }

        final WritableRaster srcRaster = image.getRaster();
        final BufferedImage toDraw = new BufferedImage(renderW, renderH, BufferedImage.TYPE_INT_ARGB);
        final WritableRaster toDrawRaster = toDraw.getRaster();

        for (int y = 0; y < renderH; ++y)
        {
            for (int x = 0; x < renderW; ++x)
            {
                final int[] test = new int[1];
                final int srcX = (int)((renderX + x) / zoomFactor);
                final int srcY = (int)((renderY + y) / zoomFactor);
                srcRaster.getDataElements(srcX, srcY, test);
                toDrawRaster.setDataElements(x, y, test);
            }
        }

        final GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphics.drawImage(SwingFXUtils.toFXImage(toDraw, null), 0, 0);
    }

    @Override
    public void notifyVisible(final Bounds bounds)
    {
        if (bounds.isEmpty())
        {
            return;
        }

        final double minX = bounds.getMinX();
        final double minY = bounds.getMinY();
        final int snappedMinX = (int)minX;
        final int snappedMinY = (int)minY;
        final double extraW = minX - snappedMinX;
        final double extraH = minY - snappedMinY;
        final double snappedWidth = (int)Math.ceil(bounds.getWidth() + extraW);
        final double snappedHeight = (int)Math.ceil(bounds.getHeight() + extraH);

        canvas.relocate(snappedMinX, snappedMinY);
        canvas.setWidth(snappedWidth);
        canvas.setHeight(snappedHeight);
    }
}
