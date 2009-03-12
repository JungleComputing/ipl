package ibis.ipl.registry.central;

import ibis.ipl.impl.IbisIdentifier;

import java.util.Comparator;

/**
 * Compares two IbisIdentifiers made by this registry by numerically sorting
 * ID's
 * 
 */
public class IbisComparator implements Comparator<IbisIdentifier> {

    public int compare(IbisIdentifier one, IbisIdentifier other) {
        try {

            int oneID = Integer.parseInt(one.getID());
            int otherID = Integer.parseInt(other.getID());

            return oneID - otherID;

        } catch (NumberFormatException e) {
            // IGNORE
        }
        return one.getID().compareTo(other.getID());
    }

}
