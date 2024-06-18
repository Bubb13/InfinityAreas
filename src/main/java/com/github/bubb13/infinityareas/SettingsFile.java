
package com.github.bubb13.infinityareas;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SettingsFile
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private final JsonObject root;

    public SettingsFile(final Path path) throws IOException
    {
        this.path = path;
        FileUtils.checkCreateFile(path);

        JsonElement element = null;
        try
        {
            // Read the settings file as JSON
            element = JsonParser.parseString(Files.readString(path));
        }
        catch (JsonSyntaxException ignored) {}

        if (element != null && element.isJsonObject())
        {
            // The settings file was a valid JSON object
            root = element.getAsJsonObject();
        }
        else
        {
            // The settings file was not a JSON object, reset it
            root = new JsonObject();
            writeToDisk();
        }
    }

    public JsonObject getRoot()
    {
        return root;
    }

    public void writeToDisk() throws IOException
    {
        Files.writeString(path, GSON.toJson(root));
    }
}
