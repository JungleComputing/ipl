package ibis.rmi;

import ibis.io.Replacer;
import ibis.rmi.server.RemoteStub;

public final class RMIReplacer implements Replacer {

    public Object replace(Object o) {

	if (o instanceof RemoteStub) {
	    return o;
	}
	Object r = RTS.getStub(o);
	if (r != null) return r;
	return o;
    }
} 
