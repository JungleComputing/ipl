package ibis.impl.nameServer.tcp;

import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.StaticProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;

class PortTypeNameServer extends Thread implements Protocol {

	/**
	 * The <code>Sequencer</code> class provides a global numbering.
	 * This can be used, for instance, for global ordering of messages.
	 * A sender must then first obtain a sequence number from the sequencer,
	 * and tag the message with it. The receiver must then handle the messages
	 * in the "tag" order.
	 * <p>
	 * A Sequencer associates a numbering scheme with a name, so the user can
	 * associate different sequences with different names.
	 */
	private static class Sequencer {
	    private HashMap counters;

	    private static class LongObject {
		long val;
		LongObject(long v) {
		    val = v;
		}
		
		public String toString() {
			return "" + val;
		}
	    }

	    Sequencer() {
		counters = new HashMap();
	    }

	    /**
	     * Returns the next sequence number associated with the specified name.
	     * @param name the name of the sequence.
	     * @return the next sequence number
	     */
	    public synchronized long getSeqno(String name) {
		LongObject i = (LongObject) counters.get(name);
		if (i == null) {
		    i = new LongObject(ibis.ipl.ReadMessage.INITIAL_SEQNO);
		    counters.put(name, i);
		}
		return i.val++;
	    }
	    
	    public String toString() {
	    	return "" + counters;
	    }
	}

	private Hashtable portTypes;
	private ServerSocket serverSocket;

	private	DataInputStream in;
	private	DataOutputStream out;
	private Sequencer seq;

	PortTypeNameServer() throws IOException { 
		portTypes = new Hashtable();

		serverSocket = NameServerClient.socketFactory.createServerSocket(0, null, true);
		setName("PortType Name Server");
		seq = new Sequencer();
		start();
	} 

	int getPort() { 
		return serverSocket.getLocalPort();
	}

	private void handlePortTypeNew() throws IOException { 

		StaticProperties p = new StaticProperties();

		String name = in.readUTF();
		int size    = in.readInt();

		for (int i=0;i<size;i++) { 
			String key = in.readUTF();
			String value = in.readUTF();

			p.add(key, value);
		}

		StaticProperties temp = (StaticProperties) portTypes.get(name);

		if (temp == null) { 
			portTypes.put(name, p);
			out.writeByte(PORTTYPE_ACCEPTED);
		} else { 			
			if (temp.equals(p)) {
				out.writeByte(PORTTYPE_ACCEPTED);
			} else { 
				out.writeByte(PORTTYPE_REFUSED);
				// System.err.println("PortTypeNameServer: PortType " + name + " refused because of incompatible properties\n" + 
				//		   temp + "----\n" + p);				
			}
		}
	} 


	private void handleSeqno() throws IOException {
		String name = in.readUTF();

		long l = seq.getSeqno(name);
		out.writeLong(l);
		out.flush();
	}


	public void run() {
		
		Socket s;
		int opcode;

		while (true) {

			try {
				s = NameServerClient.socketFactory.accept(serverSocket);
			} catch (Exception e) {
				throw new IbisRuntimeException("PortTypeNameServer: got an error ", e);
			}

			try {
				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new DataInputStream(new BufferedInputStream(di));
				DummyOutputStream dout = new DummyOutputStream(s.getOutputStream());

				out = new DataOutputStream(new BufferedOutputStream(dout));

				opcode = in.readByte();

				switch (opcode) { 
				case (PORTTYPE_NEW): 
					handlePortTypeNew();
					break;
				case (PORTTYPE_EXIT):
					NameServerClient.socketFactory.close(in, out, s);
					return;
				case (SEQNO):
					handleSeqno();
					break;
				default: 
					System.err.println("PortTypeNameServer: got an illegal opcode " + opcode);					
				}
				NameServerClient.socketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in PortTypeNameServer.run " + e1);
				e1.printStackTrace();
				if (s != null) { 
					try { 
						s.close();
					} catch (IOException e2) { 
						// don't care.
					} 
				}
			}
		}
	}

	public String toString() {
		return "portypeserver(porttypes=" + portTypes + ",seq=" + seq + ")"; 
	}
}
