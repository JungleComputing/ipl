package ibis.impl.nameServer.tcp;

import ibis.ipl.StaticProperties;

import ibis.util.*;

import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.EOFException;
import java.io.StreamCorruptedException;

import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;

import java.util.Enumeration;

class PortTypeNameServerClient implements Protocol {

	InetAddress server;
	int port;
	InetAddress localAddress;

	PortTypeNameServerClient(InetAddress localAddress, InetAddress server, int port) {
		this.server = server;
		this.port = port;
		this.localAddress = localAddress;
	} 

	public boolean newPortType(String name, StaticProperties p) throws IOException { 

		Socket s = null;
		ObjectOutputStream out;
		InputStream in;
		int result, type;

		s = IbisSocketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		out.writeByte(PORTTYPE_NEW);
		out.writeUTF(name);
		
		out.writeInt(p.size());
		
		// Send all properties.
		Enumeration e = p.keys();
		while (e.hasMoreElements()) { 
			String key = (String) e.nextElement();
			out.writeUTF(key);
			
			String value = p.find(key);
			out.writeUTF(value);
		} 
		
		out.flush();

		in = new DummyInputStream(s.getInputStream());
		result = in.read();

		IbisSocketFactory.close(in, out, s);

		switch (result) { 
		case PORTTYPE_ACCEPTED: return true;
		case PORTTYPE_REFUSED: return false;
		default:
			throw new StreamCorruptedException("PortTypeNameServer: got illegal opcode");
		}
	}
}
