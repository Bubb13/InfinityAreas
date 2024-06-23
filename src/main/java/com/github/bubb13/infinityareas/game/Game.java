
package com.github.bubb13.infinityareas.game;

import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.game.resource.BifFile;
import com.github.bubb13.infinityareas.game.resource.KeyFile;
import com.github.bubb13.infinityareas.game.resource.ResourceIdentifier;
import com.github.bubb13.infinityareas.util.FileUtil;
import com.github.bubb13.infinityareas.misc.IteratorToIterable;
import com.github.bubb13.infinityareas.util.JavaFXUtil;
import com.github.bubb13.infinityareas.util.MiscUtil;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

public class Game
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final Path gameRoot;
    private final KeyFile keyFile;
    private final GameResources resources = new GameResources();
    private final BifFile[] bifFiles;

    private String[] classicIniNames = new String[] {
        "baldur.ini", "icewind.ini", "icewind2.ini", "torment.ini"
    };
    private Path aliasHD0;
    private Path aliasCD1;
    private Path aliasCD2;
    private Path aliasCD3;
    private Path aliasCD4;
    private Path aliasCD5;
    private Path aliasCD6;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public Game(final KeyFile keyFile)
    {
        this.gameRoot = keyFile.getPath().getParent();
        this.keyFile = keyFile;
        this.bifFiles = new BifFile[keyFile.getNumBifEntries()];
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public JavaFXUtil.TaskManager.ManagedTask<Void> loadResourcesTask()
    {
        return new LoadResourcesTask();
    }

    public KeyFile getKeyFile()
    {
        return keyFile;
    }

    public Path getRoot()
    {
        return gameRoot;
    }

    public Iterable<Resource> getResourcesOfType(final short numericType)
    {
        return new IteratorToIterable<>(
            resources.resources.values().stream()
                .filter((bucket) -> bucket.getIdentifier().numericType() == numericType)
                .iterator()
        );
    }

    public Iterable<Resource> getResourcesOfType(final KeyFile.NumericResourceType numericType)
    {
        return getResourcesOfType(numericType.getNumericType());
    }

    public Iterable<Resource> getResourcesOfType(final KeyFile.ResourceType resourceType)
    {
        return getResourcesOfType(resourceType.getNumericType());
    }

    public Resource getResource(final ResourceIdentifier identifier)
    {
        return this.resources.getResource(identifier);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    ////////////////////
    // Public Classes //
    ////////////////////

    public static class Resource
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
        private final String relativePathStr;

        public BifSource(
            final ResourceIdentifier identifier, final short bifIndex, final KeyFile.ResourceLocator resourceLocator,
            final String rootString, final Path rootPath)
        {
            super(identifier, ResourceSourceType.BIF);
            this.bifIndex = bifIndex;
            this.resourceLocator = resourceLocator;
            this.relativePathStr = rootString + File.separator
                + FileUtil.getRelativePath(rootPath.toAbsolutePath(), bifFiles[bifIndex].getPath());
        }

        public short getBifIndex()
        {
            return bifIndex;
        }

        @Override
        public String getRelativePathStr()
        {
            return this.relativePathStr;
        }

        @Override
        public ByteBuffer demandFileData() throws Exception
        {
            if (this.getIdentifier().numericType() == KeyFile.NumericResourceType.TIS.getNumericType())
            {
                return bifFiles[bifIndex].demandTilesetData(resourceLocator);
            }
            return bifFiles[bifIndex].demandResourceData(resourceLocator);
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
            return ByteBuffer.wrap(Files.readAllBytes(path));
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

    /////////////////////
    // Private Classes //
    /////////////////////

    private class LoadResourcesTask extends JavaFXUtil.TaskManager.ManagedTask<Void>
    {
        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void call() throws Exception
        {
            parseClassicINI();
            scanBifs("<game>");
            scanFolder("<game>", gameRoot, gameRoot.resolve("override"));
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        private void parseClassicINI() throws Exception
        {
            Path iniPath = null;

            for (final String iniName : classicIniNames)
            {
                final Path testIniPath = gameRoot.resolve(iniName);
                if (Files.isRegularFile(testIniPath))
                {
                    iniPath = testIniPath;
                    break;
                }
            }

            if (iniPath == null)
            {
                return;
            }

            final INIConfiguration ini = new INIConfiguration();
            ini.setSeparatorUsedInInput("=");

            try (final FileReader reader = new FileReader(iniPath.toFile()))
            {
                ini.read(reader);
                final var alias = ini.configurationAt("Alias");
                aliasHD0 = parseINIPath(alias, "HD0:");
                aliasCD1 = parseINIPath(alias, "CD1:");
                aliasCD2 = parseINIPath(alias, "CD2:");
                aliasCD3 = parseINIPath(alias, "CD3:");
                aliasCD4 = parseINIPath(alias, "CD4:");
                aliasCD5 = parseINIPath(alias, "CD5:");
                aliasCD6 = parseINIPath(alias, "CD6:");
            }
        }

        private Path parseINIPath(final HierarchicalConfiguration<ImmutableNode> node, final String key)
        {
            final String pathStr = node.getString(key);
            return pathStr == null ? null : Paths.get(pathStr);
        }

        // TODO: The root scheme here is temporary and doesn't make sense -- chitin.key's resourceLocator
        //       describes where a BIF's root is!
        private void scanBifs(final String rootName)
        {
            updateProgress(0, keyFile.getNumFileEntries());
            updateMessage("Processing BIFS ...");

            int numFilesProcessed = 0;
            short i = -1;
            for (final KeyFile.BifEntry bifEntry : keyFile.bifEntries())
            {
                ++i;

                final String bifName = bifEntry.getName();
                Path bifRoot = null;
                Path bifPath = null;

                for (final KeyFile.BifEntry.Location possibleLocation : bifEntry.getPossibleLocations())
                {
                    final Path testBifRoot = switch (possibleLocation)
                    {
                        case HD0 -> aliasHD0 == null ? gameRoot : aliasHD0;
                        // TODO: Is this under HD0?
                        case CACHE -> (aliasHD0 == null ? gameRoot : aliasHD0).resolve("cache");
                        case CD1 -> aliasCD1;
                        case CD2 -> aliasCD2;
                        case CD3 -> aliasCD3;
                        case CD4 -> aliasCD4;
                        case CD5 -> aliasCD5;
                        case CD6 -> aliasCD6;
                        default -> throw new IllegalStateException(String.format(
                            "Unknown bif location for \"%s\"", bifName));
                    };
                    final Path testBifPath = FileUtil.resolvePathSafe(testBifRoot, bifName);

                    if (testBifPath == null)
                    {
                        throw new IllegalStateException(String.format(
                            "Attempted to access malformed bif path: \"%s%s%s\"",
                            testBifRoot, File.separator, bifName));
                    }

                    if (Files.isRegularFile(testBifPath))
                    {
                        bifRoot = testBifRoot;
                        bifPath = testBifPath;
                        break;
                    }
                }

                if (bifPath == null)
                {
                    throw new IllegalStateException(String.format("Failed to find bif: \"%s\"", bifName));
                }

                BifFile bifFile;
                try
                {
                    bifFile = new BifFile(bifPath);
                    bifFiles[i] = bifFile;
                }
                catch (final IOException e)
                {
                    // TODO - Shouldn't open ErrorAlert directly
                    ErrorAlert.openAndWait("Exception accessing bif: \"" + bifPath + "\"", e);
                    continue;
                }

                for (final KeyFile.FileEntry fileEntry : bifEntry.fileEntries())
                {
                    final ResourceIdentifier identifier = new ResourceIdentifier(fileEntry);
                    resources.addSource(
                        identifier,
                        new BifSource(identifier, i, fileEntry.getResourceLocator(), rootName, bifRoot)
                    );
                    updateProgress(++numFilesProcessed, keyFile.getNumFileEntries());
                }
            }
        }

        private void scanFolder(final String rootName, final Path rootPath, final Path path) throws Exception
        {
            if (!Files.isDirectory(path))
            {
                return;
            }

            updateMessage("Processing " + path.getFileName().toString() + " folder ...");

            final ArrayList<Path> paths = FileUtil.getAllInPath(path);
            final int limit = paths.size();
            updateProgress(0, limit);

            for (int i = 0; i < limit; ++i)
            {
                final Path subpath = paths.get(i);

                if (!Files.isRegularFile(subpath))
                {
                    updateProgress(i, limit);
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
                updateProgress(i, limit);
            }
            updateProgress(limit, limit);
        }
    }

    private static class GameResources
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final TreeMap<ResourceIdentifier, Resource> resources = new TreeMap<>();

        ////////////////////
        // Public Methods //
        ////////////////////

        public void addSource(final ResourceIdentifier identifier, final ResourceSource source)
        {
            final Resource resource = this.resources.computeIfAbsent(identifier,
                k -> new Resource(identifier));

            resource.addSource(source);
        }

        public Resource getResource(final ResourceIdentifier identifier)
        {
            return this.resources.get(identifier);
        }
    }
}
