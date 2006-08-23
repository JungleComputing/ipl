/* $Id$ */

import ibis.satin.WriteMethodsInterface;

public interface TranspositionTableIntr extends WriteMethodsInterface {
    void sharedStore(int index, Tag tag, short value, short bestChild,
        byte depth, boolean lowerBound);
}
