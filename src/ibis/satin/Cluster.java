package ibis.satin;

import ibis.ipl.*;
import java.util.*;

final class Cluster {
	String name; //all references to a cluster's name are the same
	Vector victims;

	public Cluster(String name) {
		this.name = name;
		victims = new Vector();
	}

	public Cluster(Victim v) {
		this(v.ident.cluster());
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
		} else {
			return other.name.equals(name);
		}
	}

	public void add(Victim v) {
		victims.add(v);
	}

	public boolean remove(Victim v) {
		return victims.remove(v);
	}

	public Victim get(int index) {
		return (Victim) victims.get(index);
	}

	public int size() {
		return victims.size();
	}
}
