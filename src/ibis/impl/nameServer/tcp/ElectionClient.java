package ibis.impl.nameServer.tcp;

import ibis.util.*;

import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

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

		s = IbisSocketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		out.writeByte(ELECTION);
		out.writeUTF(election);
		out.writeObject(candidate);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));

		result = in.readObject();
		IbisSocketFactory.close(in, out, s);

		return result;
	}
}
