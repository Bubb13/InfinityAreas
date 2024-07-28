
package com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation;

import com.github.bubb13.infinityareas.game.resource.KeyFile;

public class ResrefFieldOptions extends AbstractFieldOptions<ResrefFieldOptions>
{
    private KeyFile.NumericResourceType[] resourceTypes;

    public KeyFile.NumericResourceType[] getResourceTypes()
    {
        return resourceTypes;
    }

    public ResrefFieldOptions resourceTypes(final KeyFile.NumericResourceType... resourceTypes)
    {
        this.resourceTypes = resourceTypes;
        return this;
    }
}
