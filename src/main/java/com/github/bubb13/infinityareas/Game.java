
package com.github.bubb13.infinityareas;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
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
                + FileUtils.getRelativePath(rootPath.toAbsolutePath(), bifFiles[bifIndex].getPath());
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
                + FileUtils.getRelativePath(rootPath.toAbsolutePath(), path);
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
            scanBifs("<game>", gameRoot);
            scanFolder("<game>", gameRoot, gameRoot.resolve("override"));
            return null;
        }

        /////////////////////
        // Private Methods //
        /////////////////////

        // TODO: The root scheme here is temporary and doesn't make sense -- chitin.key's resourceLocator
        //       describes where a BIF's root is!
        private void scanBifs(final String rootName, final Path rootPath)
        {
            updateProgress(0, keyFile.getNumFileEntries());
            updateMessage("Processing BIFS ...");

            int numFilesProcessed = 0;
            short i = -1;
            for (final KeyFile.BifEntry bifEntry : keyFile.bifEntries())
            {
                ++i;

                final Path bifPath = FileUtils.resolvePathSafe(rootPath, bifEntry.getName());
                if (bifPath == null)
                {
                    new ErrorAlert("Attempted to access malformed bif path: \""
                        + rootPath.toAbsolutePath() + "\"" + bifEntry.getName() + "\"").showAndWait();

                    continue;
                }

                BifFile bifFile;
                try
                {
                    bifFile = new BifFile(bifPath);
                    bifFiles[i] = bifFile;
                }
                catch (final IOException e)
                {
                    new ErrorAlert("Exception accessing bif: \"" + bifPath + "\"").showAndWait();
                    continue;
                }

                for (final KeyFile.FileEntry fileEntry : bifEntry.fileEntries())
                {
                    final ResourceIdentifier identifier = new ResourceIdentifier(fileEntry);
                    resources.addSource(
                        identifier,
                        new BifSource(identifier, i, fileEntry.getResourceLocator(), rootName, rootPath)
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

            final ArrayList<Path> paths = FileUtils.getAllInPath(path);
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

                final var fileNameAndExtension = FileUtils.getFileNameAndExtension(subpath);
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
