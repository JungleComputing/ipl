package ibis.satin;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPort;

final class Victim {
	IbisIdentifier ident;
	SendPort s;
	
	public boolean equals(Object o) {
		if(o == this) return true;
		if (o instanceof Victim) {
			Victim other = (Victim) o;
			return other.ident.equals(ident);
		}
		return false;
	}
	
	public boolean equals(Victim other) {
		if(other == this) return true;
		return other.ident.equals(ident);
	}
}
