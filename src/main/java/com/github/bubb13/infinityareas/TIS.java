
package com.github.bubb13.infinityareas;

import java.nio.ByteBuffer;

public class TIS
{
    //////////////////////////
    // Public Static Fields //
    //////////////////////////

    public static final int HEADER_SIZE = 0x18;

    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    public static final int PVRZ_TILE_DATA_ENTRY_SIZE = 0xC;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final String wedNumeric;
    private final Game.ResourceSource source;
    private ByteBuffer buffer;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public TIS(final String wedNumeric, final Game.ResourceSource source)
    {
        this.wedNumeric = wedNumeric;
        this.source = source;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadTISTask()
    {
        return new LoadTISTask();
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class PVRZPage
    {
        private int page;
        private int texCoordX;
        private int texCoordY;

        public PVRZPage(int page, int texCoordX, int texCoordY)
        {
            this.page = page;
            this.texCoordX = texCoordX;
            this.texCoordY = texCoordY;
        }
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadTISTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
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
            updateMessage("Processing TIS ...");

            position(0x0); final String signature = BufferUtil.readUTF8(buffer, 4);
            if (!signature.equals("TIS "))
            {
                throw new IllegalStateException("Invalid TIS signature: \"" + signature + "\"");
            }

            position(0x4); final String version = BufferUtil.readUTF8(buffer, 4);
            if (!version.equals("V1  "))
            {
                throw new IllegalStateException("Invalid TIS version: \"" + version + "\"");
            }

            position(0x8); final int numTiles = buffer.getInt();
            position(0xC); final int lengthOfTileBlockData = buffer.getInt();
            position(0x10); final int sizeOfHeader = buffer.getInt();
            position(0x14); final int tileSideLength = buffer.getInt();

            System.out.printf("numTiles: %d\n", numTiles);
            System.out.printf("lengthOfTileBlockData: %d\n", lengthOfTileBlockData);
            System.out.printf("sizeOfHeader: %d\n", sizeOfHeader);
            System.out.printf("tileSideLength: %d\n", tileSideLength);

            if (lengthOfTileBlockData == 0xC)
            {
                parsePVRZTileData(sizeOfHeader, numTiles);
            }
            else
            {
                throw new IllegalStateException("Unimplemented");
            }
        }

        private void parsePVRZTileData(final int offset, final int count) throws Exception
        {
            final Game game = GlobalState.getGame();
            final char firstChar = source.getIdentifier().resref().charAt(0);
            final String pvrzPrefix = firstChar + wedNumeric;
            System.out.printf("pvrzPrefix: \"%s\"\n", pvrzPrefix);

            int curBase = offset;
            for (int i = 0; i < count; ++i, curBase += PVRZ_TILE_DATA_ENTRY_SIZE)
            {
                position(curBase); final int pvrzPage = buffer.getInt();

                if (pvrzPage == -1)
                {
                    // Special: Completely black
                    continue;
                }

                position(curBase + 0x4); final int coordinateX = buffer.getInt();
                position(curBase + 0x8); final int coordinateY = buffer.getInt();

                final String formattedPage = String.format("%02d", pvrzPage);
                System.out.println("formattedPage: " + formattedPage);

                final String pvrzResref = pvrzPrefix + String.format("%02d", pvrzPage);
                System.out.println("pvrzResref: " + pvrzResref);

                final Game.Resource pvrzResource = game.getResource(new ResourceIdentifier(
                    pvrzResref, KeyFile.NumericResourceType.PVRZ));

                if (pvrzResource == null)
                {
                    throw new IllegalStateException("Unable to find source for PVRZ resource \"" + pvrzResref + "\"");
                }

                final PVRZ pvrz = new PVRZ(pvrzResource.getPrimarySource());
                this.subtask(pvrz.loadPVRZTask());

                // TODO
                if (true)
                {
                    break;
                }
            }
        }
    }
}
