
package com.github.bubb13.infinityareas.misc;

public class InstanceBiMap<A, B>
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final InstanceHashMap<A, B> aToB = new InstanceHashMap<>();
    private final InstanceHashMap<B, A> bToA = new InstanceHashMap<>();

    ////////////////////
    // Public Methods //
    ////////////////////

    public void put(final A a, final B b)
    {
        aToB.put(a, b);
        bToA.put(b, a);
    }

    public A getA(final B b)
    {
        return bToA.get(b);
    }

    public B getB(final A a)
    {
        return aToB.get(a);
    }

    public B removeA(final A a)
    {
        final B b = aToB.remove(a);
        bToA.remove(b);
        return b;
    }

    public A removeB(final B b)
    {
        final A a = bToA.remove(b);
        aToB.remove(a);
        return a;
    }
}
