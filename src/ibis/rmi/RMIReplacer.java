package ibis.rmi;

import ibis.ipl.Replacer;

import ibis.rmi.server.RemoteStub;
import ibis.rmi.Remote;

public final class RMIReplacer implements Replacer {

    public Object replace(Object o) {

	if (o instanceof RemoteStub) {
	    return o;
	}
	if (o instanceof Remote) {
	    Object r = RTS.getStub(o);
	    if (r != null) return r;
	}
	return o;
    }
} 
