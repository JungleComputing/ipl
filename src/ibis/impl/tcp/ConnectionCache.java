package ibis.ipl.impl.tcp;

import java.util.HashMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class ConnectionCache {
	private HashMap peers = new HashMap();

	private synchronized Peer getPeer(TcpIbisIdentifier ibis) {
		Peer p = (Peer) peers.get(ibis);

		if (p == null) {
			p = new Peer(ibis);
			peers.put(ibis, p);
		}

		return p;
	}

	Connection newConnection(TcpIbisIdentifier ibis, Socket s, InputStream in, OutputStream out) {
		Peer p = getPeer(ibis);
		return p.newConnection(s, in, out);
	}

	void releaseOutput(TcpIbisIdentifier ibis, OutputStream out) {
		Peer p = getPeer(ibis);
		p.releaseOutput(out);
	}

	void releaseInput(TcpIbisIdentifier ibis, InputStream in) {
		Peer p = getPeer(ibis);
		p.releaseInput(in);
	}


	void addFreeInput(TcpIbisIdentifier ibis, Connection c) { 
		Peer p = getPeer(ibis);
		p.addFreeInput(c);
	} 
	
	void addFreeOutput(TcpIbisIdentifier ibis, Connection c) { 
		Peer p = getPeer(ibis);
		p.addFreeOutput(c);
	} 

	void addUsed(TcpIbisIdentifier ibis, Connection c) { 
		Peer p = getPeer(ibis);
		p.addUsed(c);
	}
	
	Connection findFreeInput(TcpIbisIdentifier ibis) {
		Peer p = getPeer(ibis);
		return p.findFreeInput();
	}

	Connection findFreeOutput(TcpIbisIdentifier ibis, int local_id, int remote_id) { 
		Peer p = getPeer(ibis);
		return p.findFreeOutput(local_id, remote_id);
	}
}
