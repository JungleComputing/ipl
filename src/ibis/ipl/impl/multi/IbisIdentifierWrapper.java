package ibis.ipl.impl.multi;

import ibis.ipl.IbisIdentifier;

final class IbisIdentifierWrapper implements Comparable<IbisIdentifierWrapper> {
    final IbisIdentifier id;
    final String ibisName;

    IbisIdentifierWrapper(String ibisName, IbisIdentifier id) {
        this.id = id;
        this.ibisName = ibisName;
    }

    public int compareTo(IbisIdentifierWrapper w) {
        int compare = this.ibisName.compareTo(w.ibisName);
        if (compare == 0) {
            return id.compareTo(w.id);
        }
        else {
            return compare;
        }
    }
}
