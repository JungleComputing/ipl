package ibis.impl.nameServer.tcp;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

class ElectionClient implements Protocol {

	InetAddress server;
	int port;
	InetAddress localAddress = null;

	ElectionClient(InetAddress localAddress, InetAddress server, int port) {
		this.server = server;
		this.port = port;
		this.localAddress = localAddress;
	} 

	Object elect(String election, Object candidate) throws IOException, ClassNotFoundException { 

		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		Object result = null;

		// Election: candidate==null means caller is not a candidate.
		// Note: at least one caller must have a candidate.
		while (result == null) {
		    s = NameServerClient.socketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		    DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		    out = new ObjectOutputStream(new BufferedOutputStream(dos));

		    out.writeByte(ELECTION);
		    out.writeUTF(election);
		    out.writeObject(candidate);
		    out.flush();

		    DummyInputStream di = new DummyInputStream(s.getInputStream());
		    in  = new ObjectInputStream(new BufferedInputStream(di));

		    result = in.readObject();
		    NameServerClient.socketFactory.close(in, out, s);
		    if (result == null) {
			try {Thread.sleep(1000);} catch (Exception ee) { /* ignore */ }
		    }
		}

		return result;
	}
	
	Object reelect(String election, Object candidate, Object formerRuler) throws IOException, ClassNotFoundException { 

		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		Object result = null;

		while (result == null) {
		    s = NameServerClient.socketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		    
		    DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		    out = new ObjectOutputStream(new BufferedOutputStream(dos));

		    out.writeByte(REELECTION);
		    out.writeUTF(election);
		    out.writeObject(candidate);
		    out.writeObject(formerRuler);
		    out.flush();

		    DummyInputStream di = new DummyInputStream(s.getInputStream());
		    in  = new ObjectInputStream(new BufferedInputStream(di));

		    result = in.readObject();
		    NameServerClient.socketFactory.close(in, out, s);
		    if (result == null) {
			try {Thread.sleep(1000);} catch (Exception ee) { /* ignore */ }
		    }
		}

		return result;
	}
}
