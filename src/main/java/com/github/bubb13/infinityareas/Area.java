
package com.github.bubb13.infinityareas;

import java.nio.ByteBuffer;
import java.util.List;

public class Area
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Game.ResourceSource source;
    private ByteBuffer buffer;

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

            position(8); String wedResref = BufferUtil.readLUTF8(buffer, 8);

            final Game game = GlobalState.getGame();

            final ResourceIdentifier wedIdentifier = new ResourceIdentifier(wedResref, KeyFile.NumericResourceType.WED);
            final Game.Resource wedResource = game.getResource(wedIdentifier);

            if (wedResource == null)
            {
                throw new IllegalStateException("Unable to find source for WED resource \"" + wedResref + "\"");
            }

            final WED wed = new WED(wedResource.getPrimarySource());
            this.subtask(wed.loadWEDTask());

            final List<WED.Overlay> overlays = wed.getOverlays();

            if (!overlays.isEmpty())
            {
                final WED.Overlay overlay = overlays.get(0);

                final String tisResref = overlay.getTilesetResref();
                final Game.Resource tisResource = game.getResource(new ResourceIdentifier(
                    tisResref, KeyFile.NumericResourceType.TIS));

                if (tisResource == null)
                {
                    throw new IllegalStateException("Unable to find source for TIS resource \"" + wedResref + "\"");
                }

                final String wedNumeric = wedResref.substring(2);
                final TIS tis = new TIS(wedNumeric, tisResource.getPrimarySource());
                this.subtask(tis.loadTISTask());
            }
        }
    }
}
