
package com.github.bubb13.infinityareas.game.resource;

import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.BufferUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeyFile
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Path path;
    private final ArrayList<BifEntry> bifEntries = new ArrayList<>();
    private final TreeMap<ResourceIdentifier, FileEntry> fileEntries = new TreeMap<>();
    private ByteBuffer buffer;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public KeyFile(final Path path)
    {
        this.path = path.toAbsolutePath();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    //-------//
    // Tasks //
    //-------//

    public void load(final TaskTrackerI tracker) throws Exception
    {
        tracker.subtask(this::loadInternal);
    }

    public void load() throws Exception
    {
        loadInternal(TaskTracker.DUMMY);
    }

    public TrackedTask<Void> loadTask()
    {
        return new TrackedTask<>()
        {
            @Override
            protected Void doTask() throws Exception
            {
                subtask(KeyFile.this::loadInternal);
                return null;
            }
        };
    }

    //---------//
    // General //
    //---------//

    public Path getPath()
    {
        return path;
    }

    public BifEntry getBifEntry(final short index)
    {
        return bifEntries.get(index);
    }

    public short getNumBifEntries()
    {
        return (short)bifEntries.size();
    }

    public Iterable<BifEntry> bifEntries()
    {
        return MiscUtil.readOnlyIterable(bifEntries);
    }

    public FileEntry getFileEntry(final String resref, final short numericType)
    {
        return fileEntries.get(new ResourceIdentifier(resref, numericType));
    }

    public FileEntry getFileEntry(final String resref, final NumericResourceType numericResourceType)
    {
        return getFileEntry(resref, numericResourceType.getNumericType());
    }

    public FileEntry getFileEntry(final String resref, final ResourceType resourceType)
    {
        return getFileEntry(resref, resourceType.getNumericType());
    }

    public FileEntry getFileEntry(final String resref, final String extension)
    {
        return getFileEntry(resref, NumericResourceType.fromExtension(extension));
    }

    public int getNumFileEntries()
    {
        return fileEntries.size();
    }

    public Iterable<FileEntry> fileEntries()
    {
        return MiscUtil.readOnlyIterable(fileEntries.values());
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws IOException
    {
        tracker.updateMessage("Parsing key file ...");
        tracker.updateProgress(0, 1);

        bifEntries.clear();
        fileEntries.clear();

        if (!Files.isRegularFile(path))
        {
            throw new FileNotFoundException("Key file does not exist: \"" + path + "\"");
        }

        buffer = ByteBuffer.wrap(Files.readAllBytes(path));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        parse(tracker);
    }

    private void parse(final TaskTrackerI tracker)
    {
        position(0x0); final String signature = BufferUtil.readUTF8(buffer, 4);
        if (!signature.equals("KEY "))
        {
            throw new IllegalStateException("Invalid key signature: \"" + signature + "\"");
        }

        position(0x4); final String version = BufferUtil.readUTF8(buffer, 4);
        if (!version.equals("V1  "))
        {
            throw new IllegalStateException("Invalid key version: \"" + version + "\"");
        }

        position(0x8);  final int numBifEntries = buffer.getInt();
        position(0xC);  final int numResourceEntries = buffer.getInt();
        position(0x10); final int bifEntriesOffset = buffer.getInt();
        position(0x14); final int resourceEntriesOffset = buffer.getInt();

        parseBifEntries(tracker, bifEntriesOffset, numBifEntries);
        parseFileEntries(tracker, resourceEntriesOffset, numResourceEntries);
    }

    private void parseBifEntries(final TaskTrackerI tracker, final int offset, final int count)
    {
        int curBase = offset;
        for (int i = 0; i < count; i++)
        {
            position(curBase);       final int bifFileLength = buffer.getInt();
            position(curBase + 0x4); final int offsetToBifName = buffer.getInt();
            position(curBase + 0x8); final short lengthOfBifFileName = buffer.getShort();
            position(curBase + 0xA); final short bifLocationBitfield = buffer.getShort();

            position(offsetToBifName);
            final String bifName = BufferUtil.readUTF8(buffer, lengthOfBifFileName - 1);

            bifEntries.add(new BifEntry(bifName, bifLocationBitfield, bifFileLength));
            curBase += 0xC; // Next entry
        }
    }

    private void parseFileEntries(final TaskTrackerI tracker, final int offset, final int count)
    {
        int curBase = offset;
        for (int i = 0; i < count; i++)
        {
            position(curBase);       final String resref = BufferUtil.readLUTF8(buffer, 8);
            position(curBase + 0x8); final short resourceType = buffer.getShort();
            position(curBase + 0xA); final int resourceLocator = buffer.getInt();

            final ResourceIdentifier resourceIdentifier = new ResourceIdentifier(resref, resourceType);
            final FileEntry fileEntry = new FileEntry(resref, resourceType, resourceLocator);

            // Add to main list
            fileEntries.put(resourceIdentifier, fileEntry);

            // Add to per-bif list
            final int bifIndex = fileEntry.getResourceLocator().getBifIndex();
            bifEntries.get(bifIndex).fileEntries.add(fileEntry);

            curBase += 0xE; // Next entry
        }
    }

    private void position(final int pos)
    {
        buffer.position(pos);
    }

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class BifEntry
    {
        // In file structure
        private final String name;
        private final short locationBitfield;
        private final int fileLength;

        // Derived
        private final ArrayList<Location> possibleLocations;
        private final ArrayList<FileEntry> fileEntries = new ArrayList<>();

        /**
         * Some bif names start with '\' or '/', which can represent the root of the filesystem.
         * It is important to sanitize these instances so that Infinity Areas doesn't accidentally
         * resolve a bif path to the wrong location.
         */
        private final static Pattern badBifName = Pattern.compile("^[\\\\/]+(.*)$");

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public BifEntry(String name, final short locationBitfield, final int fileLength)
        {
            // Sanitize bad bif names
            final Matcher badBifNameMatcher = badBifName.matcher(name);
            if (badBifNameMatcher.matches())
            {
                name = badBifNameMatcher.group(1);
            }

            this.name = name;
            this.locationBitfield = locationBitfield;
            this.fileLength = fileLength;

            this.possibleLocations = Location.fromBitfield(locationBitfield);
            //System.out.printf("BIF: \"%s\", locationBitfield: 0x%X\n", name, locationBitfield);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public String getName()
        {
            return name;
        }

        public short getLocationBitfield()
        {
            return locationBitfield;
        }

        public int getFileLength()
        {
            return fileLength;
        }

        public Iterable<Location> getPossibleLocations()
        {
            return MiscUtil.readOnlyIterable(possibleLocations);
        }

        public Iterable<FileEntry> fileEntries()
        {
            return MiscUtil.readOnlyIterable(fileEntries.iterator());
        }

        ////////////////////
        // Public Classes //
        ////////////////////

        public enum Location
        {
            HD0,
            CACHE,
            CD1,
            CD2,
            CD3,
            CD4,
            CD5,
            CD6,
            UNKNOWN;

            public static ArrayList<Location> fromBitfield(final short bitfield)
            {
                final ArrayList<Location> result = new ArrayList<>();

                if ((bitfield & 0x1) != 0) result.add(Location.HD0);
                if ((bitfield & 0x2) != 0) result.add(Location.CACHE);
                if ((bitfield & 0x4) != 0) result.add(Location.CD1);
                if ((bitfield & 0x8) != 0) result.add(Location.CD2);
                if ((bitfield & 0x10) != 0) result.add(Location.CD3);
                if ((bitfield & 0x20) != 0) result.add(Location.CD4);
                if ((bitfield & 0x40) != 0) result.add(Location.CD5);
                if ((bitfield & 0x80) != 0) result.add(Location.CD6);
                if ((bitfield & 0x100) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x100");
                if ((bitfield & 0x200) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x200");
                if ((bitfield & 0x400) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x400");
                if ((bitfield & 0x800) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x800");
                if ((bitfield & 0x1000) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x1000");
                if ((bitfield & 0x2000) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x2000");
                if ((bitfield & 0x4000) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x4000");
                if ((bitfield & 0x8000) != 0)
                    throw new IllegalStateException("Unknown bif locator bit set: 0x8000");

                return result;
            }
        }
    }

    public static class FileEntry
    {
        private final String resref;
        private final ResourceType resourceType;
        private final ResourceLocator resourceLocator;

        public FileEntry(final String resref, final short resourceType, final int resourceLocator)
        {
            this.resref = resref;
            this.resourceType = new ResourceType(resourceType);
            this.resourceLocator = new ResourceLocator(resourceLocator);
        }

        public String getResref()
        {
            return resref;
        }

        public ResourceType getResourceType()
        {
            return resourceType;
        }

        public ResourceLocator getResourceLocator()
        {
            return resourceLocator;
        }
    }

    public enum NumericResourceType
    {
        UNKNOWN(-1, ""),
        _2DA(0x3F4, "2DA"),  // CResText
        ARE(0x3F2, "ARE"),   // CResArea
        BAM(0x3E8, "BAM"),   // CResCell
        BCS(0x3EF, "BCS"),   // CResText
        BIO(0x3FE, "BIO"),   // CResBIO
        BMP(0x1, "BMP"),     // CResBitmap
        BS(0x3F9, "BS"),     // CResText
        CHR(0x3FA, "CHR"),   // CResCHR
        CHU(0x3EA, "CHU"),   // CResUI
        CRE(0x3F1, "CRE"),   // CResCRE
        DLG(0x3F3, "DLG"),   // CResDLG
        EFF(0x3F8, "EFF"),   // CResEffect
        GAM(0x3F5, "GAM"),   // CResGame
        GLSL(0x405, "GLSL"), // CResText
        GUI(0x402, "GUI"),   // CResText
        IDS(0x3F0, "IDS"),   // CResText
        INI(0x802, "INI"),   // CRes
        ITM(0x3ED, "ITM"),   // CResItem
        LUA(0x409, "LUA"),   // CResText
        MENU(0x408, "MENU"), // CResText
        MOS(0x3EC, "MOS"),   // CResMosaic
        MVE(0x2, "MVE"),     // CRes
        PLT(0x6, "PLT"),     // CResPLT
        PNG(0x40B, "PNG"),   // CResPng
        PRO(0x3FD, "PRO"),   // CResBinary
        PVRZ(0x404, "PVRZ"), // CResPVR
        SPL(0x3EE, "SPL"),   // CResSpell
        SQL(0x403, "SQL"),   // CResText
        STO(0x3F6, "STO"),   // CResStore
        TGA(0x3, "TGA"),     // CRes
        TIS(0x3EB, "TIS"),   // CResTileSet
        TOH(0x407, "TOH"),   // CRes
        TOT(0x406, "TOT"),   // CRes
        TTF(0x40A, "TTF"),   // CResFont
        VEF(0x3FC, "VEF"),   // CResBinary
        VVC(0x3FB, "VVC"),   // CResBinary
        WAV(0x4, "WAV"),     // CResWave
        WBM(0x3FF, "WBM"),   // CResWebm
        WED(0x3E9, "WED"),   // CResWED
        WFX(0x5, "WFX"),     // CResBinary
        WMP(0x3F7, "WMP");   // CResWorldMap

        private final short numericType;
        private final String extension;

        NumericResourceType(final int numericType, final String extension)
        {
            this.numericType = (short)numericType;
            this.extension = extension;
        }

        public short getNumericType()
        {
            return numericType;
        }

        public String getExtension()
        {
            return extension;
        }

        public static NumericResourceType fromNumericType(final short numericType)
        {
            return switch (numericType)
            {
                case 0x3F4 -> _2DA; // CResText
                case 0x3F2 -> ARE;  // CResArea
                case 0x3E8 -> BAM;  // CResCell
                case 0x3EF -> BCS;  // CResText
                case 0x3FE -> BIO;  // CResBIO
                case 0x1 -> BMP;    // CResBitmap
                case 0x3F9 -> BS;   // CResText
                case 0x3FA -> CHR;  // CResCHR
                case 0x3EA -> CHU;  // CResUI
                case 0x3F1 -> CRE;  // CResCRE
                case 0x3F3 -> DLG;  // CResDLG
                case 0x3F8 -> EFF;  // CResEffect
                case 0x3F5 -> GAM;  // CResGame
                case 0x405 -> GLSL; // CResText
                case 0x402 -> GUI;  // CResText
                case 0x3F0 -> IDS;  // CResText
                case 0x802 -> INI;  // CRes
                case 0x3ED -> ITM;  // CResItem
                case 0x409 -> LUA;  // CResText
                case 0x408 -> MENU; // CResText
                case 0x3EC -> MOS;  // CResMosaic
                case 0x2 -> MVE;    // CRes
                case 0x6 -> PLT;    // CResPLT
                case 0x40B -> PNG;  // CResPng
                case 0x3FD -> PRO;  // CResBinary
                case 0x404 -> PVRZ; // CResPVR
                case 0x3EE -> SPL;  // CResSpell
                case 0x403 -> SQL;  // CResText
                case 0x3F6 -> STO;  // CResStore
                case 0x3 -> TGA;    // CRes
                case 0x3EB -> TIS;  // CResTileSet
                case 0x407 -> TOH;  // CRes
                case 0x406 -> TOT;  // CRes
                case 0x40A -> TTF;  // CResFont
                case 0x3FC -> VEF;  // CResBinary
                case 0x3FB -> VVC;  // CResBinary
                case 0x4 -> WAV;    // CResWave
                case 0x3FF -> WBM;  // CResWebm
                case 0x3E9 -> WED;  // CResWED
                case 0x5 -> WFX;    // CResBinary
                case 0x3F7 -> WMP;  // CResWorldMap
                default -> UNKNOWN;
            };
        }

        public static NumericResourceType fromExtension(final String extension)
        {
            return switch (extension.toUpperCase())
            {
                case "2DA" -> _2DA;  // CResText
                case "ARE" -> ARE;   // CResArea
                case "BAM" -> BAM;   // CResCell
                case "BCS" -> BCS;   // CResText
                case "BIO" -> BIO;   // CResBIO
                case "BMP" -> BMP;   // CResBitmap
                case "BS" -> BS;     // CResText
                case "CHR" -> CHR;   // CResCHR
                case "CHU" -> CHU;   // CResUI
                case "CRE" -> CRE;   // CResCRE
                case "DLG" -> DLG;   // CResDLG
                case "EFF" -> EFF;   // CResEffect
                case "GAM" -> GAM;   // CResGame
                case "GLSL" -> GLSL; // CResText
                case "GUI" -> GUI;   // CResText
                case "IDS" -> IDS;   // CResText
                case "INI" -> INI;   // CRes
                case "ITM" -> ITM;   // CResItem
                case "LUA" -> LUA;   // CResText
                case "MENU" -> MENU; // CResText
                case "MOS" -> MOS;   // CResMosaic
                case "MVE" -> MVE;   // CRes
                case "PLT" -> PLT;   // CResPLT
                case "PNG" -> PNG;   // CResPng
                case "PRO" -> PRO;   // CResBinary
                case "PVRZ" -> PVRZ; // CResPVR
                case "SPL" -> SPL;   // CResSpell
                case "SQL" -> SQL;   // CResText
                case "STO" -> STO;   // CResStore
                case "TGA" -> TGA;   // CRes
                case "TIS" -> TIS;   // CResTileSet
                case "TOH" -> TOH;   // CRes
                case "TOT" -> TOT;   // CRes
                case "TTF" -> TTF;   // CResFont
                case "VEF" -> VEF;   // CResBinary
                case "VVC" -> VVC;   // CResBinary
                case "WAV" -> WAV;   // CResWave
                case "WBM" -> WBM;   // CResWebm
                case "WED" -> WED;   // CResWED
                case "WFX" -> WFX;   // CResBinary
                case "WMP" -> WMP;   // CResWorldMap
                default -> UNKNOWN;
            };
        }
    }

    public static class ResourceType
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        final NumericResourceType numericResourceType;
        final short numericType;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public ResourceType(final short numericType)
        {
            this(NumericResourceType.fromNumericType(numericType), numericType);
        }

        //////////////////////////
        // Private Constructors //
        //////////////////////////

        private ResourceType(final NumericResourceType numericResourceType, final short numericType)
        {
            this.numericResourceType = numericResourceType;
            this.numericType = numericType;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public NumericResourceType getEnum()
        {
            return numericResourceType;
        }

        public short getNumericType()
        {
            return numericType;
        }
    }

    public static class ResourceLocator
    {
        private final short resourceIndex; // BIT00-13
        private final byte tilesetIndex;   // BIT14-19
        private final short bifIndex;      // BIT20-31

        public ResourceLocator(final int resourceLocator)
        {
            this.resourceIndex = (short)(resourceLocator & 0x3FFF);
            this.tilesetIndex = (byte)((resourceLocator >> 14) & 0x3F);
            this.bifIndex = (short)((resourceLocator >> 20) & 0xFFF);
        }

        public short getResourceIndex()
        {
            return resourceIndex;
        }

        public byte getTilesetIndex()
        {
            return tilesetIndex;
        }

        public short getBifIndex()
        {
            return bifIndex;
        }
    }
}
