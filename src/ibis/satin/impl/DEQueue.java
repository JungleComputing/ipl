package ibis.satin.impl;

/** The base class of all double-ended queue implementations. */

abstract class DEQueue implements Config {

    abstract InvocationRecord getFromHead();

    abstract InvocationRecord getFromTail();

    abstract void addToHead(InvocationRecord o);

    abstract void addToTail(InvocationRecord o); //note: does not work for

    // Dijkstra
    abstract void killChildrenOf(int targetStamp,
            ibis.ipl.IbisIdentifier targetOwner);

    abstract void killSubtreeOf(ibis.ipl.IbisIdentifier targetOwner);

    abstract int size();

    abstract void print(java.io.PrintStream out);
}