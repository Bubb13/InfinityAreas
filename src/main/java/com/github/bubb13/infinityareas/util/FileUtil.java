
package com.github.bubb13.infinityareas.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

public final class FileUtil
{
    public static void checkCreateFile(final Path path) throws IOException
    {
        // Create the file if it doesn't exist
        if (!Files.exists(path))
        {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        }

        // If the created path is anything other than a file, throw an error
        if (!Files.isRegularFile(path))
        {
            throw new FileNotFoundException("Not a file: \"" + path.toAbsolutePath() + "\"");
        }
    }

    public static Path resolvePathSafe(final Path rootPath, final String toResolve)
    {
        try
        {
            return rootPath.resolve(toResolve);
        }
        catch (final InvalidPathException e)
        {
            return null;
        }
    }

    public static FileNameAndExtension getFileNameAndExtension(final Path path)
    {
        final String fileName = path.getFileName().toString();
        final int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) return new FileNameAndExtension(fileName, "");
        final String name = fileName.substring(0, dotIndex);
        final String extension = fileName.substring(dotIndex + 1);
        return new FileNameAndExtension(name, extension);
    }

    public static String getFileName(final Path path)
    {
        final String fileName = path.getFileName().toString();
        final int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) return fileName;
        return fileName.substring(0, dotIndex);
    }

    public static String getExtension(final Path path)
    {
        final String fileName = path.getFileName().toString();
        final int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex == -1) return fileName;
        return fileName.substring(dotIndex + 1);
    }

    public static void forAllInPath(final Path path, final Consumer<Path> consumer) throws IOException
    {
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(path))
        {
            stream.forEach(consumer);
        }
    }

    public static ArrayList<Path> getAllInPath(final Path path) throws IOException
    {
        final ArrayList<Path> paths = new ArrayList<>();
        try (final DirectoryStream<Path> stream = Files.newDirectoryStream(path))
        {
            stream.forEach(paths::add);
        }
        return paths;
    }

    public static Path getRelativePath(final Path parentPath, final Path childPath)
    {
        if (childPath.startsWith(parentPath))
        {
            return childPath.subpath(parentPath.getNameCount(), childPath.getNameCount());
        }
        return childPath;
    }

    private FileUtil() {}

    /////////////
    // Classes //
    /////////////

    public record FileNameAndExtension(String name, String extension) {}
}
