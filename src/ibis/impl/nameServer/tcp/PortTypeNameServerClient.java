package ibis.impl.nameServer.tcp;

import ibis.ipl.StaticProperties;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;
import ibis.util.IbisSocketFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Iterator;

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
		
		Set e = p.propertyNames();
		Iterator i = e.iterator();

		out.writeInt(p.size());
		
		// Send all properties.
		while (i.hasNext()) { 
			String key = (String) i.next();
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
