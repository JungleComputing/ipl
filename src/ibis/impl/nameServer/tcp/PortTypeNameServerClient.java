package ibis.impl.nameServer.tcp;

import ibis.ipl.StaticProperties;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

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
		DataOutputStream out;
		DataInputStream in;
		int result, type;

		s = NameServerClient.socketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new DataOutputStream(new BufferedOutputStream(dos, 4096));

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

		in = new DataInputStream(new DummyInputStream(s.getInputStream()));
		result = in.readByte();

		NameServerClient.socketFactory.close(in, out, s);

		switch (result) { 
		case PORTTYPE_ACCEPTED: return true;
		case PORTTYPE_REFUSED: return false;
		default:
			throw new StreamCorruptedException("PortTypeNameServer: got illegal opcode");
		}
	}

	public long getSeqno(String name) throws IOException { 
		Socket s = NameServerClient.socketFactory.createSocket(server, port, localAddress, 0 /* retry */);
		
		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(dos));

		out.writeByte(SEQNO);
		out.writeUTF(name);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		DataInputStream in  = new DataInputStream(new BufferedInputStream(di));
		
		long temp = in.readLong();

		NameServerClient.socketFactory.close(in, out, s);

		return temp;
	} 

}
