package ibis.satin;

import ibis.ipl.*;
import java.util.*;

final class VictimTable implements Config {
	private Vector victims = new Vector();
	private HashMap victimsHash = new HashMap();
	private Satin satin;

	VictimTable(Satin s) {
		this.satin = s;
	}

	void add(IbisIdentifier ident, SendPort port) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		Victim v = new Victim();
		v.ident = ident;
		v.s = port;
		victims.add(v);
		victimsHash.put(ident, port);
	}

	Victim remove(IbisIdentifier ident) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		Victim v = new Victim();
		v.ident = ident;

		victimsHash.remove(ident);
		int i = victims.indexOf(v);
		if(i < 0) {
			return null;
		}
		return (Victim) victims.remove(i);
	}

	Victim remove(int i) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		if(i < 0 || i >= victims.size()) {
			return null;
		}

		Victim v = (Victim) victims.remove(i);
		victimsHash.remove(v.ident);
		return v;
	}
	
	int size() {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		return victims.size();
	}

	SendPort getPort(int i) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}
		if(i < 0 || i >= victims.size()) {
			return null;
		}
		return ((Victim) victims.get(i)).s;
	}

	IbisIdentifier getIdent(int i) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}			
		if(i < 0 || i >= victims.size()) {
			return null;
		}
		return ((Victim) victims.get(i)).ident;
	}

	SendPort getReplyPort(IbisIdentifier ident) {
		return (SendPort) victimsHash.get(ident);
	}

	Victim getRandomVictim() {
		Victim v;
		int index;
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		index = Math.abs(satin.random.nextInt()) % victims.size();

		v = ((Victim)victims.get(index));

		return v;
	}

	void print(java.io.PrintStream out) {
		if(ASSERTS) {
			Satin.assertLocked(satin);
		}

		out.println("victimtable on " + satin + ", size is " + victims.size());

		for(int i=0; i<victims.size(); i++) {
			out.println("   " + victims.get(i));
		}
	}
}
