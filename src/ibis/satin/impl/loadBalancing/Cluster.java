/* $Id$ */

package ibis.satin.impl.loadBalancing;

import java.util.Vector;

final class Cluster {
    private String name; //all references to a cluster's name are the same

    private Vector<Victim> victims;

    protected Cluster(String name) {
        this.name = name;
        victims = new Vector<Victim>();
    }

    protected Cluster(Victim v) {
        this(v.getIdent().getLocation().getCluster());
        victims.add(v);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Cluster) {
            Cluster other = (Cluster) o;
            return other.name.equals(name);
        } else {
            return false;
        }
    }

    public boolean equals(Cluster other) {
        if (other == this) {
            return true;
        }
        return other.name.equals(name);
    }

    public int hashCode() {
        return name.hashCode();
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
