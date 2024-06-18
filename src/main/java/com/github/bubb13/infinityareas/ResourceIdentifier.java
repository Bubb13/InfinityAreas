
package com.github.bubb13.infinityareas;

public record ResourceIdentifier(String resref, short numericType) implements Comparable<ResourceIdentifier>
{
    public ResourceIdentifier(final String resref, final short numericType)
    {
        this.resref = resref.toUpperCase();
        this.numericType = numericType;
    }

    public ResourceIdentifier(final String resref, final KeyFile.NumericResourceType numericType)
    {
        this(resref, numericType.getNumericType());
    }

    public ResourceIdentifier(final String resref, final KeyFile.ResourceType resourceType)
    {
        this(resref, resourceType.getNumericType());
    }

    public ResourceIdentifier(final KeyFile.FileEntry fileEntry)
    {
        this(fileEntry.getResref(), fileEntry.getResourceType());
    }

    @Override
    public int compareTo(final ResourceIdentifier other)
    {
        final int strCmp = resref.compareTo(other.resref);
        if (strCmp != 0) return strCmp;
        return Short.compare(numericType, other.numericType);
    }
}
