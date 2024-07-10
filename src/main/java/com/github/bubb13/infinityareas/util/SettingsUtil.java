
package com.github.bubb13.infinityareas.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Consumer;

public final class SettingsUtil
{
    ///////////////////////////
    // Public Static Methods //
    ///////////////////////////

    public static void attemptApplyInt(final JsonObject root, final String name, final Consumer<Integer> consumer)
    {
        final JsonElement element = root.get(name);
        if (element == null) return;

        int value;
        try
        {
            value = element.getAsInt();
        }
        catch (final Exception ignored)
        {
            root.remove(name);
            return;
        }

        consumer.accept(value);
    }

    public static void attemptApplyBoolean(final JsonObject root, final String name, final Consumer<Boolean> consumer)
    {
        final JsonElement element = root.get(name);
        if (element == null) return;

        boolean value;
        try
        {
            value = element.getAsBoolean();
        }
        catch (final Exception ignored)
        {
            root.remove(name);
            return;
        }

        consumer.accept(value);
    }

    //////////////////////////
    // Private Constructors //
    //////////////////////////

    private SettingsUtil() {}
}
