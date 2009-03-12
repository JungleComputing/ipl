package ibis.ipl.registry.gossip;

import ibis.ipl.impl.IbisIdentifier;

import java.util.Comparator;
import java.util.UUID;

/**
 * Compares two IbisIdentifiers made by this registry by comparing
 * UUID's
 * 
 */
public class IbisComparator implements Comparator<IbisIdentifier> {

    public int compare(IbisIdentifier one, IbisIdentifier other) {

        UUID oneID = UUID.fromString(one.getID());
        UUID otherID = UUID.fromString(other.getID());

        return oneID.compareTo(otherID);

    }

}
