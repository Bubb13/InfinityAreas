
package com.github.bubb13.infinityareas.game;

import com.github.bubb13.infinityareas.game.resource.BifFile;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.misc.TaskTracker;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents an Infinity Engine game installation. Primarily facilitates the reading of game resources.
 */
public class Game
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Path gameRoot;
    private final KeyFile keyFile;
    private final GameResources resources = new GameResources();
    private BifFile[] bifFiles;
    private Type engineType;

    private final ArrayList<Path> aliasHD0 = new ArrayList<>();
    private final ArrayList<Path> aliasCache = new ArrayList<>(); // Not defined in INI, but derived from aliasHD0
    private final ArrayList<Path> aliasCD1 = new ArrayList<>();
    private final ArrayList<Path> aliasCD2 = new ArrayList<>();
    private final ArrayList<Path> aliasCD3 = new ArrayList<>();
    private final ArrayList<Path> aliasCD4 = new ArrayList<>();
    private final ArrayList<Path> aliasCD5 = new ArrayList<>();
    private final ArrayList<Path> aliasCD6 = new ArrayList<>();

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Game(final KeyFile keyFile)
    {
        this.gameRoot = keyFile.getPath().getParent();
        this.keyFile = keyFile;
    }

    ////////////////////
    // Public Methods //
    ////////////////////

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
                subtask(Game.this::loadInternal);
                return null;
            }
        };
    }

    public KeyFile getKeyFile()
    {
        return keyFile;
    }

    public Path getRoot()
    {
        return gameRoot;
    }

    public Iterable<Resource> getResources()
    {
        return MiscUtil.readOnlyIterable(resources.resources.values());
    }

    public Iterable<Resource> getResourcesOfType(final short numericType)
    {
        return resources.getResourcesOfType(numericType);
    }

    public Iterable<Resource> getResourcesOfType(final KeyFile.NumericResourceType numericType)
    {
        return resources.getResourcesOfType(numericType);
    }

    public Iterable<Resource> getResourcesOfType(final KeyFile.ResourceType resourceType)
    {
        return resources.getResourcesOfType(resourceType);
    }

    public Resource getResource(final ResourceIdentifier identifier)
    {
        return this.resources.getResource(identifier);
    }

    public Type getEngineType()
    {
        return engineType;
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class Resource implements Comparable<Resource>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ResourceIdentifier identifier;
        private final TreeSet<ResourceSource> sources = new TreeSet<>();

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public Resource(final ResourceIdentifier identifier)
        {
            this.identifier = identifier;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public ResourceIdentifier getIdentifier()
        {
            return identifier;
        }

        public void addSource(final ResourceSource source)
        {
            this.sources.add(source);
        }

        public Iterable<ResourceSource> sources()
        {
            return MiscUtil.readOnlyIterable(sources);
        }

        public ResourceSource getPrimarySource()
        {
            return sources.first();
        }

        @Override
        public int compareTo(final Resource other)
        {
            return identifier.compareTo(other.identifier);
        }
    }

    public enum ResourceSourceType
    {
        LOOSE_FILE,
        BIF
    }

    public abstract static class ResourceSource implements Comparable<ResourceSource>
    {
        private final ResourceIdentifier identifier;
        private final ResourceSourceType sourceType;

        public ResourceSource(final ResourceIdentifier identifier, final ResourceSourceType sourceType)
        {
            this.identifier = identifier;
            this.sourceType = sourceType;
        }

        public ResourceIdentifier getIdentifier()
        {
            return identifier;
        }

        public ResourceSourceType getSourceType()
        {
            return sourceType;
        }

        public KeyFile.NumericResourceType getNumericType()
        {
            return KeyFile.NumericResourceType.fromNumericType(identifier.numericType());
        }

        @Override
        public int compareTo(final ResourceSource other)
        {
            return Integer.compare(this.sourceType.ordinal(), other.sourceType.ordinal());
        }

        public abstract ByteBuffer demandFileData() throws Exception;
        public abstract String getRelativePathStr();
    }

    public class BifSource extends ResourceSource
    {
        private final short bifIndex;
        private final KeyFile.ResourceLocator resourceLocator;

        public BifSource(
            final ResourceIdentifier identifier, final short bifIndex, final KeyFile.ResourceLocator resourceLocator)
        {
            super(identifier, ResourceSourceType.BIF);
            this.bifIndex = bifIndex;
            this.resourceLocator = resourceLocator;
        }

        public short getBifIndex()
        {
            return bifIndex;
        }

        @Override
        public String getRelativePathStr()
        {
            final BifFile bifFile = bifFiles[bifIndex];
            if (bifFile == null)
            {
                return "<MISSING>";
            }
            return bifFile.getRelativePathStr();
        }

        @Override
        public ByteBuffer demandFileData() throws Exception
        {
            final BifFile bifFile = bifFiles[bifIndex];
            if (bifFile == null)
            {
                return null;
            }

            if (this.getIdentifier().numericType() == KeyFile.NumericResourceType.TIS.getNumericType())
            {
                return bifFile.demandTilesetData(resourceLocator);
            }
            return bifFile.demandResourceData(resourceLocator);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BifSource bifSource = (BifSource) o;
            return bifIndex == bifSource.bifIndex && Objects.equals(resourceLocator, bifSource.resourceLocator);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(bifIndex, resourceLocator);
        }
    }

    public static class LooseFileSource extends ResourceSource
    {
        private final Path path;
        private final String relativePathStr;

        public LooseFileSource(
            final ResourceIdentifier resourceIdentifier, final Path path, final String rootString, final Path rootPath)
        {
            super(resourceIdentifier, ResourceSourceType.LOOSE_FILE);
            this.path = path.toAbsolutePath();
            this.relativePathStr = rootString + File.separator
                + FileUtil.getRelativePath(rootPath.toAbsolutePath(), path);
        }

        public Path getPath()
        {
            return path;
        }

        @Override
        public String getRelativePathStr()
        {
            return this.relativePathStr;
        }

        @Override
        public ByteBuffer demandFileData() throws Exception
        {
            final ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(path));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return buffer;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LooseFileSource that = (LooseFileSource) o;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(path);
        }
    }

    public enum Type
    {
        SOA,
        TOB,
        IWD2,
        PST,
        BG1,
        TOTSC,
        IWD1,
        HOW,
        TOTLM,
        TUTU,
        TUTU_TOTSC,
        BGT,
        CA,
        IWD_IN_BG2,
        BG2EE,
        BGEE,
        IWDEE,
        PSTEE,
        EET
    }

    /////////////////////////////
    // START Loading Resources //
    /////////////////////////////

    private void loadInternal(final TaskTrackerI tracker) throws Exception
    {
        registerResources(tracker);
        detectEngineType();
        handleAliasPaths(tracker);
        readBifs(tracker);
    }

    private void registerResources(final TaskTrackerI tracker) throws Exception
    {
        resources.clear();
        registerBifResources(tracker);
        registerLooseResources(tracker);
    }

    private void registerBifResources(final TaskTrackerI tracker) throws Exception
    {
        tracker.updateMessage("Registering BIF sources ...");
        tracker.updateProgress(0, keyFile.getNumBifEntries());

        short i = 0;
        for (final KeyFile.BifEntry bifEntry : keyFile.bifEntries())
        {
            for (final KeyFile.FileEntry fileEntry : bifEntry.fileEntries())
            {
                final ResourceIdentifier identifier = new ResourceIdentifier(fileEntry);
                resources.addSource(
                    identifier,
                    new BifSource(identifier, i, fileEntry.getResourceLocator())
                );
            }
            tracker.updateProgress(++i, keyFile.getNumBifEntries());
        }
    }

    private void registerLooseResources(final TaskTrackerI tracker) throws Exception
    {
        scanFolder(tracker, "<game>", gameRoot,
            FileUtil.resolveCaseInsensitive(gameRoot, "override"));
    }

    private void scanFolder(
        final TaskTrackerI tracker, final String rootName, final Path rootPath, final Path path) throws Exception
    {
        if (path == null || !Files.isDirectory(path))
        {
            return;
        }

        tracker.updateMessage("Registering resources in folder: " + path.getFileName().toString());

        final ArrayList<Path> paths = FileUtil.getAllInPath(path);
        final int limit = paths.size();
        tracker.updateProgress(0, limit);

        for (int i = 0; i < limit; ++i)
        {
            final Path subpath = paths.get(i);

            if (!Files.isRegularFile(subpath))
            {
                tracker.updateProgress(i, limit);
                continue;
            }

            final var fileNameAndExtension = FileUtil.getFileNameAndExtension(subpath);
            final String fileName = fileNameAndExtension.name();
            final String extension = fileNameAndExtension.extension();
            final KeyFile.NumericResourceType resourceType = KeyFile.NumericResourceType.fromExtension(extension);

            if (fileName.length() <= 8 && resourceType != KeyFile.NumericResourceType.UNKNOWN)
            {
                final ResourceIdentifier identifier = new ResourceIdentifier(
                    fileName, resourceType.getNumericType()
                );
                resources.addSource(
                    identifier,
                    new LooseFileSource(identifier, subpath, rootName, rootPath)
                );
            }
            tracker.updateProgress(i, limit);
        }
        tracker.updateProgress(limit, limit);
    }

    private void detectEngineType()
    {
        final List<Type> types = detectGameTypes(false);
        if (types.size() != 1)
        {
            throw new IllegalStateException("Unknown engine type");
        }
        engineType = types.get(0);
    }

    // Credit: WeiDU [https://github.com/WeiDUorg/weidu/blob/devel/src/tppe.ml] PE_GameIs
    private List<Type> detectGameTypes(final boolean isGameCheck)
    {
        final boolean tutu     = isGameCheck && resources.hasResource(new ResourceIdentifier("fw0125",   KeyFile.NumericResourceType.ARE));
        final boolean bgt      = isGameCheck && resources.hasResource(new ResourceIdentifier("ar7200",   KeyFile.NumericResourceType.ARE));
        final boolean ca       = isGameCheck && resources.hasResource(new ResourceIdentifier("tc1300",   KeyFile.NumericResourceType.ARE));
        final boolean iwdinbg2 = isGameCheck && resources.hasResource(new ResourceIdentifier("ar9201",   KeyFile.NumericResourceType.ARE));
        final boolean eet      = isGameCheck && resources.hasResource(new ResourceIdentifier("eet.flag", KeyFile.NumericResourceType.UNKNOWN));
        final boolean bg2      =                resources.hasResource(new ResourceIdentifier("ar0083",   KeyFile.NumericResourceType.ARE));
        final boolean tob      =                resources.hasResource(new ResourceIdentifier("ar6111",   KeyFile.NumericResourceType.ARE));
        final boolean iwd2     =                resources.hasResource(new ResourceIdentifier("ar6050",   KeyFile.NumericResourceType.ARE));
        final boolean pst      =                resources.hasResource(new ResourceIdentifier("ar0104a",  KeyFile.NumericResourceType.ARE));
        final boolean bg1      =                resources.hasResource(new ResourceIdentifier("ar0125",   KeyFile.NumericResourceType.ARE));
        final boolean tosc     =                resources.hasResource(new ResourceIdentifier("ar2003",   KeyFile.NumericResourceType.ARE));
        final boolean iwd1     =                resources.hasResource(new ResourceIdentifier("ar2116",   KeyFile.NumericResourceType.ARE));
        final boolean how      =                resources.hasResource(new ResourceIdentifier("ar9109",   KeyFile.NumericResourceType.ARE));
        final boolean tolm     =                resources.hasResource(new ResourceIdentifier("ar9715",   KeyFile.NumericResourceType.ARE));
        final boolean ttsc     =                resources.hasResource(new ResourceIdentifier("fw2003",   KeyFile.NumericResourceType.ARE));
        final boolean bgee     =                resources.hasResource(new ResourceIdentifier("oh1000",   KeyFile.NumericResourceType.ARE));
        final boolean bg2ee    =                resources.hasResource(new ResourceIdentifier("oh6000",   KeyFile.NumericResourceType.ARE));
        final boolean iwdee    =                resources.hasResource(new ResourceIdentifier("howparty", KeyFile.NumericResourceType._2DA));
        final boolean pstee    =                resources.hasResource(new ResourceIdentifier("pstchar",  KeyFile.NumericResourceType._2DA));

        final ArrayList<Type> matchedTypes = new ArrayList<>();

        for (final Type gameType : Type.values())
        {
            switch (gameType)
            {
                case SOA        -> { if ( bg2 && !tutu && !tob && !ca && !iwdinbg2           ) matchedTypes.add(gameType); }
                case TOB        -> { if ( bg2 && !tutu &&  tob && !ca && !iwdinbg2 && !bg2ee ) matchedTypes.add(gameType); }
                case IWD2       -> { if ( iwd2                                               ) matchedTypes.add(gameType); }
                case PST        -> { if ( pst && !pstee                                      ) matchedTypes.add(gameType); }
                case BG1        -> { if ( bg1 && !tosc && !bg2                               ) matchedTypes.add(gameType); }
                case TOTSC      -> { if ( bg1 &&  tosc && !bg2 && !iwd1 && !bgee             ) matchedTypes.add(gameType); }
                case IWD1       -> { if ( iwd1 && !how && !tolm && !bg2                      ) matchedTypes.add(gameType); }
                case HOW        -> { if ( iwd1 &&  how && !tolm && !bg2                      ) matchedTypes.add(gameType); }
                case TOTLM      -> { if ( iwd1 &&  how &&  tolm && !bg2 && !iwdee            ) matchedTypes.add(gameType); }
                case TUTU       -> { if ( tutu && !ttsc                                      ) matchedTypes.add(gameType); }
                case TUTU_TOTSC -> { if ( tutu &&  ttsc                                      ) matchedTypes.add(gameType); }
                case BGT        -> { if ( bgt                                                ) matchedTypes.add(gameType); }
                case CA         -> { if ( ca                                                 ) matchedTypes.add(gameType); }
                case IWD_IN_BG2 -> { if ( bg2 && iwdinbg2                                    ) matchedTypes.add(gameType); }
                case BG2EE      -> { if ( bg2ee && !eet                                      ) matchedTypes.add(gameType); }
                case BGEE       -> { if ( bgee && !bg2ee && !eet                             ) matchedTypes.add(gameType); }
                case IWDEE      -> { if ( iwdee                                              ) matchedTypes.add(gameType); }
                case PSTEE      -> { if ( pstee                                              ) matchedTypes.add(gameType); }
                case EET        -> { if ( eet                                                ) matchedTypes.add(gameType); }
            }
        }

        return matchedTypes;
    }

    private void handleAliasPaths(final TaskTrackerI tracker) throws Exception
    {
        aliasHD0.clear();
        aliasCache.clear();
        aliasCD1.clear();
        aliasCD2.clear();
        aliasCD3.clear();
        aliasCD4.clear();
        aliasCD5.clear();
        aliasCD6.clear();

        final String classicININame = switch (engineType)
        {
            case
                BG1,   // oBG1
                TOTSC, // oBG1 + Tales of the Sword Coast
                SOA,   // oBG2
                TOB    // oBG2 + Throne of Bhaal
                -> "baldur.ini";
            case
                IWD1,  // oIWD
                HOW,   // oIWD + Heart of Winter
                TOTLM  // oIWD + Heart of Winter + Trials of the Luremaster
                -> "icewind.ini";

            case IWD2 -> "icewind2.ini"; // IWD2
            case PST  -> "torment.ini";  // oPST
            default   -> null;
        };

        if (classicININame == null)
        {
            aliasHD0.add(gameRoot); // EE games only use the game root
        }
        else
        {
            parseClassicINI(tracker, classicININame);
        }
    }

    private void parseClassicINI(final TaskTrackerI tracker, final String classicININame) throws Exception
    {
        tracker.updateMessage("Processing classic game ini ...");
        tracker.updateProgress(0, 1);

        final Path iniPath = FileUtil.resolveCaseInsensitiveElseError(gameRoot, classicININame,
            (errorPathStr) -> String.format("Invalid ini for classic game: \"%s\"", errorPathStr));

        if (!Files.isRegularFile(iniPath))
        {
            throw new IllegalStateException(String.format("Invalid ini for classic game: \"%s\"", iniPath));
        }

        final INIConfiguration ini = new INIConfiguration();
        ini.setSeparatorUsedInInput("=");

        try (final FileReader reader = new FileReader(iniPath.toFile()))
        {
            ini.read(reader);
            final var alias = ini.configurationAt("Alias");

            parseINIPath(alias, "HD0:", aliasHD0);
            aliasHD0.add(gameRoot); // Fall back to game root when HD0: isn't defined

            for (final Path path : aliasHD0)
            {
                final Path cachePath = FileUtil.resolveCaseInsensitive(path, "cache");
                if (cachePath != null)
                {
                    aliasCache.add(cachePath);
                }
            }

            parseINIPath(alias, "CD1:", aliasCD1);
            parseINIPath(alias, "CD2:", aliasCD2);
            parseINIPath(alias, "CD3:", aliasCD3);
            parseINIPath(alias, "CD4:", aliasCD4);
            parseINIPath(alias, "CD5:", aliasCD5);
            parseINIPath(alias, "CD6:", aliasCD6);
        }

        tracker.updateProgress(1, 1);
    }

    private void parseINIPath(
        final HierarchicalConfiguration<ImmutableNode> node, final String key, final Collection<Path> aliasPaths)
    {
        final String aliasStr = node.getString(key);
        if (aliasStr == null) return;

        // Classic versions sometimes use a semicolon to supply more than one path for an alias.
        // The engine uses the first matching path when it is looking for a resource.
        final String[] pathStrings = aliasStr.split(";");
        for (final String pathString : pathStrings)
        {
            final Path resolvedAlias = FileUtil.resolveCaseInsensitive(pathString);
            if (resolvedAlias != null)
            {
                aliasPaths.add(resolvedAlias);
            }
        }
    }

    private void readBifs(final TaskTrackerI tracker) throws Exception
    {
        tracker.updateMessage("Processing BIFS ...");
        tracker.updateProgress(0, keyFile.getNumBifEntries());

        bifFiles = new BifFile[keyFile.getNumBifEntries()];

        short i = -1;
        for (final KeyFile.BifEntry bifEntry : keyFile.bifEntries())
        {
            ++i;

            final String bifName = bifEntry.getName();
            final Path bifNamePath = FileUtil.pathFromUnnormalizedString(bifName);

            String bifRootName = null;
            Path bifRoot = null;
            Path bifPath = null;

            outer:
            for (final KeyFile.BifEntry.Location possibleLocation : bifEntry.getPossibleLocations())
            {
                //System.out.printf("BIF: \"%s\", possible location: %s\n", bifEntry.getName(), possibleLocation);
                final String testBifRootName;
                final ArrayList<Path> testBifRoots;

                switch (possibleLocation)
                {
                    case HD0 ->
                    {
                        testBifRootName = "<game>";
                        testBifRoots = aliasHD0;
                    }
                    case CACHE ->
                    {
                        testBifRootName = "<cache>";
                        testBifRoots = aliasCache;
                    }
                    case CD1 ->
                    {
                        testBifRootName = "<cd1>";
                        testBifRoots = aliasCD1;
                    }
                    case CD2 ->
                    {
                        testBifRootName = "<cd2>";
                        testBifRoots = aliasCD2;
                    }
                    case CD3 ->
                    {
                        testBifRootName = "<cd3>";
                        testBifRoots = aliasCD3;
                    }
                    case CD4 ->
                    {
                        testBifRootName = "<cd4>";
                        testBifRoots = aliasCD4;
                    }
                    case CD5 ->
                    {
                        testBifRootName = "<cd5>";
                        testBifRoots = aliasCD5;
                    }
                    case CD6 ->
                    {
                        testBifRootName = "<cd6>";
                        testBifRoots = aliasCD6;
                    }
                    default -> throw new IllegalStateException(String.format(
                        "Unknown bif location for \"%s\"", bifName));
                }

                for (final Path testBifRoot : testBifRoots)
                {
                    bifPath = checkBifRoot(testBifRoot, bifNamePath);
                    if (bifPath != null)
                    {
                        bifRootName = testBifRootName;
                        bifRoot = testBifRoot;
                        break outer;
                    }
                }
            }

            if (bifPath == null)
            {
                boolean ignore = false;

                if (engineType == Type.SOA || engineType == Type.TOB)
                {
                    final String bifOnlyName = bifNamePath.getFileName().toString();
                    if (bifOnlyName.equalsIgnoreCase("progtest.bif")
                        || bifOnlyName.equalsIgnoreCase("ProgTes2.bif")
                        || bifOnlyName.equalsIgnoreCase("DeSound.bif"))
                    {
                        ignore = true;
                    }
                }

                if (!ignore)
                {
                    ErrorAlert.openAndWait(String.format("Failed to find bif: \"%s\"", bifName));
                }
            }
            else
            {
                try
                {
                    bifFiles[i] = new BifFile(bifRootName, bifRoot, bifPath);
                }
                catch (final Exception e)
                {
                    ErrorAlert.openAndWait("Exception accessing bif: \"" + bifPath + "\"", e);
                }
            }

            tracker.updateProgress(i + 1, keyFile.getNumBifEntries());
        }
    }

    private Path checkBifRoot(final Path testBifRoot, final Path bifNamePath)
    {
        final Path testBifPath = FileUtil.resolveCaseInsensitive(testBifRoot, bifNamePath);

        if (testBifPath != null && Files.isRegularFile(testBifPath))
        {
            return testBifPath;
        }
        else if (engineType == Type.IWD1 || engineType == Type.HOW || engineType == Type.TOTLM)
        {
            final Path compressedBifNamePath = FileUtil.changePathExtension(bifNamePath, "cbf");
            final Path compressedBifPath = FileUtil.resolveCaseInsensitive(testBifRoot, compressedBifNamePath);

            if (compressedBifPath != null && Files.isRegularFile(compressedBifPath))
            {
                return compressedBifPath;
            }
        }

        return null;
    }

    ///////////////////////////
    // END Loading Resources //
    ///////////////////////////

    /////////////////////
    // Private Classes //
    /////////////////////

    private static class GameResources
    {
        ///////////////////////////
        // Private Static Fields //
        ///////////////////////////

        private static final Iterator<Resource> NULL_ITERATOR = new Iterator<>()
        {
            @Override
            public boolean hasNext()
            {
                return false;
            }

            @Override
            public Resource next()
            {
                return null;
            }
        };

        private static final Iterable<Resource> NULL_ITERABLE = () -> NULL_ITERATOR;

        ////////////////////
        // Private Fields //
        ////////////////////

        private final TreeMap<ResourceIdentifier, Resource> resources = new TreeMap<>();
        private final HashMap<Short, TreeSet<Resource>> typeBuckets = new HashMap<>();

        ////////////////////
        // Public Methods //
        ////////////////////

        public void clear()
        {
            resources.clear();
            typeBuckets.clear();
        }

        public void addSource(final ResourceIdentifier identifier, final ResourceSource source)
        {
            final Resource resource = this.resources.computeIfAbsent(identifier, (ignored) ->
            {
                final Resource newResource = new Resource(identifier);

                final TreeSet<Resource> typeBucket = typeBuckets.computeIfAbsent(
                    newResource.getIdentifier().numericType(), (ignored2) -> new TreeSet<>());

                typeBucket.add(newResource);
                return newResource;
            });
            resource.addSource(source);
        }

        public Resource getResource(final ResourceIdentifier identifier)
        {
            return this.resources.get(identifier);
        }

        public boolean hasResource(final ResourceIdentifier identifier)
        {
            return this.resources.containsKey(identifier);
        }

        public Iterable<Resource> getResources()
        {
            return MiscUtil.readOnlyIterable(resources.values());
        }

        public Iterable<Resource> getResourcesOfType(final short numericType)
        {
            final TreeSet<Resource> typeBucket = typeBuckets.get(numericType);

            if (typeBucket == null)
            {
                return NULL_ITERABLE;
            }
            else
            {
                return MiscUtil.readOnlyIterable(typeBucket);
            }
        }

        public Iterable<Resource> getResourcesOfType(final KeyFile.NumericResourceType numericType)
        {
            return getResourcesOfType(numericType.getNumericType());
        }

        public Iterable<Resource> getResourcesOfType(final KeyFile.ResourceType resourceType)
        {
            return getResourcesOfType(resourceType.getNumericType());
        }
    }
}
