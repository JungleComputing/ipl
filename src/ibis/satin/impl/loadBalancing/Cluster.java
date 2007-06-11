/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.ipl.Location;
import java.util.Vector;

final class Cluster {
    private Location location; //all references to a cluster's location are the same

    private Vector<Victim> victims;

    protected Cluster(Location name) {
        this.location = name;
        victims = new Vector<Victim>();
    }

    protected Cluster(Victim v) {
        this(v.getIdent().location());
        victims.add(v);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Cluster) {
            Cluster other = (Cluster) o;
            return other.location.equals(location);
        } else {
            return false;
        }
    }

    public boolean equals(Cluster other) {
        if (other == this) {
            return true;
        }
        return other.location.equals(location);
    }

    public int hashCode() {
        return location.hashCode();
    }

    protected void add(Victim v) {
        victims.add(v);
    }

    protected boolean remove(Victim v) {
        return victims.remove(v);
    }

    protected Victim get(int index) {
        return victims.get(index);
    }

    protected int size() {
        return victims.size();
    }
}
