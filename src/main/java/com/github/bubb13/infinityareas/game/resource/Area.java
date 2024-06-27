
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;

    private ByteBuffer buffer;
    private WED wed;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Area(final Game.ResourceSource source)
    {
        this.source = source;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadAreaTask()
    {
        return new LoadAreaTask();
    }

    public int getOverlayCount()
    {
        return wed.getOverlays().size();
    }

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlaysTask(final int... overlayIndexes)
    {
        return wed.renderOverlaysTask(overlayIndexes);
    }

    public JavaFXUtil.TaskManager.ManagedTask<BufferedImage> renderOverlaysNewTask(final int... overlayIndexes)
    {
        return wed.renderOverlaysNewTask(overlayIndexes);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadAreaTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            buffer = source.demandFileData();
            parse();
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parse() throws Exception
        {
            updateProgress(0, 100);
            updateMessage("Processing area ...");

            position(8); final String wedResref = BufferUtil.readLUTF8(buffer, 8);

            final Game game = GlobalState.getGame();

            final ResourceIdentifier wedIdentifier = new ResourceIdentifier(wedResref, KeyFile.NumericResourceType.WED);
            final Game.Resource wedResource = game.getResource(wedIdentifier);

            if (wedResource == null)
            {
                throw new IllegalStateException("Unable to find source for WED resource \"" + wedResref + "\"");
            }

            final WED tempWed = new WED(wedResource.getPrimarySource());
            subtask(tempWed.loadWEDTask());
            wed = tempWed;
        }
    }
}
