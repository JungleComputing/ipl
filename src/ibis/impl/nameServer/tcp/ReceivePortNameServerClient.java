package ibis.impl.nameServer.tcp;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ConnectionRefusedException;

import ibis.util.*;

import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

class ReceivePortNameServerClient implements Protocol {

	InetAddress server;
	int port;
	InetAddress localAddress;
	private static final boolean VERBOSE = false;

	ReceivePortNameServerClient(InetAddress localAddress, InetAddress server, int port) {
		this.server = server;
		this.port = port;
		this.localAddress = localAddress;
	} 

	public ReceivePortIdentifier lookup(String name, long timeout) throws IOException {

		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		ReceivePortIdentifier id = null;
		int result;

		s = IbisSocketFactory.createSocket(server, port, localAddress, timeout);

		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		// request a new Port.
		out.writeByte(PORT_LOOKUP);
		out.writeUTF(name);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));
		result = in.readByte();

		switch (result) {
		case PORT_UNKNOWN:
			if (VERBOSE) {
			    System.err.println("Port " + name + ": PORT_UNKNOWN");
			}
			break;
		case PORT_KNOWN:
			try {
				id = (ReceivePortIdentifier) in.readObject();
			} catch (ClassNotFoundException e) {
				throw new IOException("Unmarshall fails " + e);
			}
			break;
		default:
			throw new StreamCorruptedException("Registry: lookup got illegal opcode " + result);
		}

		IbisSocketFactory.close(in, out, s);

		return id;
	}

	public ReceivePortIdentifier [] query(IbisIdentifier ident)  throws IOException, ClassNotFoundException {
		/* not implemented yet */
		return null;
	}

	public void bind(String name, ReceivePort port) throws IOException {
		bind(name, port.identifier());
	}

	//gosia
	public void bind(String name, ReceivePortIdentifier id) throws IOException {
		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		int result;

		if (VERBOSE) {
		    System.err.println(this + ": bind \"" + name + "\" to " + id);
		}

		s = IbisSocketFactory.createSocket(server, this.port, localAddress, 0 /* retry */);

		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		// request a new Port.
		out.writeByte(PORT_NEW);
		out.writeUTF(name);
		out.writeObject(id);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));
		result = in.readByte();

		IbisSocketFactory.close(in, out, s);

		switch (result) {
		case PORT_REFUSED:
			throw new ConnectionRefusedException("Port name \"" + name + "\" is not unique!");
		case PORT_ACCEPTED:
			break;
		default:
			throw new StreamCorruptedException("Registry: bind got illegal opcode " + result);
		}

		if (VERBOSE) {
		    System.err.println(this + ": bound \"" + name + "\" to " + id);
		}

	}

	public void rebind(String name, ReceivePortIdentifier id) throws IOException {
		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		int result;

		if (VERBOSE) {
		    System.err.println(this + ": rebind \"" + name + "\" to " + id);
		}

		s = IbisSocketFactory.createSocket(server, this.port, localAddress, 0 /* retry */);

		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		// request a rebind
		out.writeByte(PORT_REBIND);
		out.writeUTF(name);
		out.writeObject(id);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));
		result = in.readByte();

		IbisSocketFactory.close(in, out, s);

		switch (result) {
		case PORT_ACCEPTED:
			break;
		default:
			throw new StreamCorruptedException("Registry: bind got illegal opcode " + result);
		}

		if (VERBOSE) {
		    System.err.println(this + ": rebound \"" + name + "\" to " + id);
		}
	}
	//end gosia

	public void unbind(String name) throws IOException {

		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		ReceivePortIdentifier id;
		int result;

		s = IbisSocketFactory.createSocket(server, this.port, localAddress, 0 /* retry */);

		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		// request a new Port.
		out.writeByte(PORT_FREE);
		out.writeUTF(name);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));

		byte temp = in.readByte();

		IbisSocketFactory.close(in, out, s);
	}

	//gosia
	public String[] list(String pattern) throws IOException {
		Socket s = null;
		ObjectOutputStream out;
		ObjectInputStream in;
		String[] result;
		
		s = IbisSocketFactory.createSocket(server, this.port, localAddress, 0 /* retry */);

		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		out = new ObjectOutputStream(new BufferedOutputStream(dos));

		// request a list of names.
		out.writeByte(PORT_LIST);
		out.writeUTF(pattern);
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		in  = new ObjectInputStream(new BufferedInputStream(di));

		byte num = in.readByte();

		result = new String[num];
		for (int i=0; i<num; i++) {
		    result[i] = in.readUTF();
		}

		IbisSocketFactory.close(in, out, s);

		return result;

	}
	//end gosia
}
