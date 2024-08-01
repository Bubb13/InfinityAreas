
package com.github.bubb13.infinityareas.misc;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;

import java.util.ArrayList;

public class CanvasCache
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final ArrayList<Canvas> cachedCanvases = new ArrayList<>();
    private int curCachedCanvasesWidth;
    private int curCachedCanvasesHeight;
    private int nextAvailableCachedCanvasIndex;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public CanvasCache(final int width, final int height)
    {
        this.curCachedCanvasesWidth = width;
        this.curCachedCanvasesHeight = height;

        new AnimationTimer()
        {
            @Override
            public void handle(final long now)
            {
                onRenderPulse();
            }
        }.start();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public Canvas getCacheCanvas()
    {
        Canvas canvas;

        if (cachedCanvases.size() == nextAvailableCachedCanvasIndex)
        {
            canvas = new Canvas(curCachedCanvasesWidth, curCachedCanvasesHeight);
            cachedCanvases.add(canvas);
        }
        else
        {
            canvas = cachedCanvases.get(nextAvailableCachedCanvasIndex);
        }

        ++nextAvailableCachedCanvasIndex;
        return canvas;
    }

    public void changeCanvasDimensions(final int width, final int height)
    {
        if (width > curCachedCanvasesWidth || height > curCachedCanvasesHeight)
        {
            if (width > curCachedCanvasesWidth) curCachedCanvasesWidth = width;
            if (height > curCachedCanvasesHeight) curCachedCanvasesHeight = height;

            cachedCanvases.clear();
            nextAvailableCachedCanvasIndex = 0;
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void onRenderPulse()
    {
        nextAvailableCachedCanvasIndex = 0;
    }
}
