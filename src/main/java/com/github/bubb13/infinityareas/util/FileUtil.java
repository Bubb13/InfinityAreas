
package com.github.bubb13.infinityareas.util;

import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.dialog.WarningAlertTwoOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

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

    public static boolean hasFileConflict(final Path path)
    {
        if (Files.exists(path))
        {
            if (Files.isRegularFile(path))
            {
                final boolean[] canceled = new boolean[1];

                WarningAlertTwoOptions.openAndWait(
                    String.format("\"%s\" already exists, and will be overwritten. Continue?", path.getFileName()),
                    "Yes", null,
                    "No", () -> canceled[0] = true);

                return canceled[0];
            }
            else
            {
                ErrorAlert.openAndWait(String.format(
                    "Failed to save: conflict with existing \"%s\".", path.getFileName())
                );
                return true;
            }
        }
        return false;
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

    public static Path changePathExtension(final Path path, final String newExtension)
    {
        final String newFileName = FileUtil.getFileNameNoExtension(path) + "." + newExtension;
        final Path parent = path.getParent();
        return parent == null ? Paths.get(newFileName) : parent.resolve(newFileName);
    }

    public static String getFileNameNoExtension(final Path path)
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

    public static Path resolveCaseInsensitive(final Path rootPath, final Path toResolveAsCaseInsensitive)
    {
        Path builtPath = rootPath;

        outer:
        for (final Path toResolvePart : toResolveAsCaseInsensitive)
        {
            final String toResolvePartStr = toResolvePart.toString();
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(builtPath))
            {
                for (final Path testPath : stream)
                {
                    final Path testPathFileName = testPath.getFileName();
                    if (testPathFileName.toString().equalsIgnoreCase(toResolvePartStr))
                    {
                        builtPath = builtPath.resolve(testPathFileName);
                        continue outer;
                    }
                }
                return null;
            }
            catch (final IOException ignored)
            {
                return null;
            }
        }
        return builtPath;
    }

    public static Path resolveCaseInsensitive(final Path rootPath, final String toResolveAsCaseInsensitive)
    {
        return resolveCaseInsensitive(rootPath, Paths.get(toResolveAsCaseInsensitive));
    }

    public static Path resolveCaseInsensitive(final Path toResolveAsCaseInsensitive)
    {
        final Path root = toResolveAsCaseInsensitive.getRoot();
        return root == null ? null : resolveCaseInsensitive(root, toResolveAsCaseInsensitive);
    }

    public static Path resolveCaseInsensitive(final String toResolveAsCaseInsensitive)
    {
        return resolveCaseInsensitive(Paths.get(toResolveAsCaseInsensitive));
    }

    public static Path resolveCaseInsensitiveElseError(
        final Path rootPath, final Path toResolveAsCaseInsensitive, final Function<String, String> errorMessageFunc)
        throws FileNotFoundException
    {
        final Path toReturn = resolveCaseInsensitive(rootPath, toResolveAsCaseInsensitive);
        if (toReturn == null)
        {
            throw new FileNotFoundException(
                errorMessageFunc.apply(rootPath.resolve(toResolveAsCaseInsensitive).toString()));
        }
        return toReturn;
    }

    public static Path resolveCaseInsensitiveElseError(
        final Path rootPath, final String toResolveAsCaseInsensitive, final Function<String, String> errorMessageFunc)
        throws FileNotFoundException
    {
        final Path toReturn = resolveCaseInsensitive(rootPath, toResolveAsCaseInsensitive);
        if (toReturn == null)
        {
            throw new FileNotFoundException(
                errorMessageFunc.apply(rootPath.resolve(toResolveAsCaseInsensitive).toString()));
        }
        return toReturn;
    }

    public static Path resolveCaseInsensitiveElseError(
        final Path toResolveAsCaseInsensitive, final Function<String, String> errorMessageFunc)
        throws FileNotFoundException
    {
        final Path rootPath = toResolveAsCaseInsensitive.getRoot();
        if (rootPath == null)
        {
            throw new FileNotFoundException(
                errorMessageFunc.apply(toResolveAsCaseInsensitive.toString()));
        }
        return resolveCaseInsensitiveElseError(rootPath, toResolveAsCaseInsensitive, errorMessageFunc);
    }

    public static Path resolveCaseInsensitiveElseError(
        final String toResolveAsCaseInsensitive, final Function<String, String> errorMessageFunc)
        throws FileNotFoundException
    {
        return resolveCaseInsensitiveElseError(Paths.get(toResolveAsCaseInsensitive), errorMessageFunc);
    }

    public static Path resolveCaseInsensitiveDefault(final Path rootPath, final Path toResolveAsCaseInsensitive)
    {
        final Path toReturn = resolveCaseInsensitive(rootPath, toResolveAsCaseInsensitive);
        if (toReturn == null)
        {
            return rootPath.resolve(toResolveAsCaseInsensitive);
        }
        return toReturn;
    }

    public static Path resolveCaseInsensitiveDefault(final Path rootPath, final String toResolveAsCaseInsensitive)
    {
        final Path toReturn = resolveCaseInsensitive(rootPath, toResolveAsCaseInsensitive);
        if (toReturn == null)
        {
            return rootPath.resolve(toResolveAsCaseInsensitive);
        }
        return toReturn;
    }

    public static Path resolveCaseInsensitiveDefault(final Path toResolveAsCaseInsensitive)
    {
        final Path rootPath = toResolveAsCaseInsensitive.getRoot();
        if (rootPath == null)
        {
            return toResolveAsCaseInsensitive;
        }
        return resolveCaseInsensitiveDefault(rootPath, toResolveAsCaseInsensitive);
    }

    public static Path resolveCaseInsensitiveDefault(final String toResolveAsCaseInsensitive)
    {
        return resolveCaseInsensitiveDefault(Paths.get(toResolveAsCaseInsensitive));
    }

    public static Path pathFromUnnormalizedString(String unnormalizedPathStr)
    {
        if (File.separatorChar == '/') unnormalizedPathStr = unnormalizedPathStr.replace('\\', '/');
        return Paths.get(unnormalizedPathStr);
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
