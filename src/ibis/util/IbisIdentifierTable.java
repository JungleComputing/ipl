package ibis.util;

import ibis.ipl.IbisIdentifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
//import java.io.OutputStream;
//import java.io.InputStream;

final class SendNode {
	Object outStream;
	IbisIdentifier ident;
	int handle;

	public boolean equals(Object obj) {
		SendNode n = (SendNode) obj;
		if(ident.equals(n.ident) && outStream == n.outStream) return true;
		return false;
	}

	public int hashCode() {
		return ident.hashCode();
	}
}

final class ReceiveNode {
	Object inStream;
	IbisIdentifier ident;
	int handle;

	public boolean equals(Object obj) {
		ReceiveNode n = (ReceiveNode) obj;
			
		if(handle == n.handle && inStream == n.inStream) return true;
		return false;
	}

	public int hashCode() {
		return handle;
	}
}


final public class IbisIdentifierTable {
	HashMap sendTable = new HashMap();
	HashMap receiveTable = new HashMap();
	int handleCounter = 1;
	SendNode tmpSendNode = new SendNode();
	ReceiveNode tmpReceiveNode = new ReceiveNode();
	int cachehits=0;

	// returns -handle for never sent before, or +handle.
	public synchronized int getHandle(Object stream,
					  IbisIdentifier ident) {
		tmpSendNode.ident = ident;
		tmpSendNode.outStream = stream;

		SendNode n = (SendNode) sendTable.get(tmpSendNode);
		if(n == null) {
			n = new SendNode();
			n.outStream = stream;
			n.ident = ident;
			n.handle = handleCounter++;
			sendTable.put(n, n);
// System.out.println("added " + ident + " with handle " + n.handle + " to sendtable");
			return -n.handle;
		}

// System.out.println("found " + ident + " with handle " + n.handle + " in sendtable");
		return n.handle;
	}

	public synchronized void addIbis(Object stream, int handle, IbisIdentifier ident) {
		ReceiveNode n = new ReceiveNode();
		n.inStream = stream;
		n.handle = handle;
		n.ident = ident;
		receiveTable.put(n, n);

// System.out.println("added ibis " + ident + " with handle " + n.handle + " to rectable");
	}

	public synchronized void removeIbis(IbisIdentifier i) {
		Set s = receiveTable.keySet();
		Iterator si = s.iterator();
		Vector v = new Vector();
		while (si.hasNext()) {
			ReceiveNode n = (ReceiveNode) si.next();
			if (n.ident.equals(i)) {
				v.add(n);
			}
		}
		int len = v.size();
		while (len > 0) {
		    try {
			ReceiveNode n = (ReceiveNode) v.remove(0);
			receiveTable.remove(n);
		    } catch(ArrayIndexOutOfBoundsException e) {
		    }
		    len--;
		}
		
		s = sendTable.keySet();
		si = s.iterator();
		while (si.hasNext()) {
			SendNode n = (SendNode) si.next();
			if (n.ident.equals(i)) {
				v.add(n);
			}
		}
		len = v.size();
		while (len > 0) {
		    try {
			SendNode n = (SendNode) v.remove(0);
			sendTable.remove(n);
		    } catch(ArrayIndexOutOfBoundsException e) {
		    }
		    len--;
		}
	}

	public synchronized IbisIdentifier getIbis(Object stream, int handle) {
		tmpReceiveNode.handle = handle;
		tmpReceiveNode.inStream = stream;

		ReceiveNode res = (ReceiveNode) receiveTable.get(tmpReceiveNode);
//		if(res == null) {
//			System.out.println("EEK");
//			System.exit(1);
//		}

// System.out.println("found cached ibis " + res.ident + " handle was " + handle + " in rectable");
		cachehits++;
		return res.ident;
	}
	/*
	protected void finalize() throws Throwable {
		System.out.println("hits: " + cachehits);
	}
	*/
}
