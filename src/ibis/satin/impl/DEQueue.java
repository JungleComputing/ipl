/* $Id$ */

package ibis.satin.impl;

/** The base class of all double-ended queue implementations. */

abstract class DEQueue implements Config {

    abstract InvocationRecord getFromHead();

    abstract InvocationRecord getFromTail();

    abstract void addToHead(InvocationRecord o);

    // Does not work for Dijkstra queue.
    abstract void addToTail(InvocationRecord o);

    abstract void killChildrenOf(Stamp targetStamp);

    abstract void killSubtreeOf(ibis.ipl.IbisIdentifier targetOwner);

    abstract int size();

    abstract void print(java.io.PrintStream out);
}
