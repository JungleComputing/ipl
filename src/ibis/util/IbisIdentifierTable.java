package ibis.ipl.impl.generic;

import ibis.io.*;
import java.util.HashMap;
import ibis.ipl.*;

final class SendNode {
	IbisSerializationOutputStream outStream;
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
	IbisSerializationInputStream inStream;
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
//	int cachehits=0;

	// returns -handle for never sent before, or +handle.
	public synchronized int getHandle(IbisSerializationOutputStream stream,
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

	public synchronized void addIbis(IbisSerializationInputStream stream, int handle, IbisIdentifier ident) {
		ReceiveNode n = new ReceiveNode();
		n.inStream = stream;
		n.handle = handle;
		n.ident = ident;
		receiveTable.put(n, n);

// System.out.println("added ibis " + ident + " with handle " + n.handle + " to rectable");
	}

	public synchronized IbisIdentifier getIbis(IbisSerializationInputStream stream, int handle) {
		tmpReceiveNode.handle = handle;
		tmpReceiveNode.inStream = stream;

		ReceiveNode res = (ReceiveNode) receiveTable.get(tmpReceiveNode);
//		if(res == null) {
//			System.out.println("EEK");
//			System.exit(1);
//		}

// System.out.println("found cached ibis " + res.ident + " handle was " + handle + " in rectable");
//		cachehits++;
		return res.ident;
	}

//	protected void finalize() throws Throwable {
//		System.out.println("hits: " + cachehits);
//	}
}
