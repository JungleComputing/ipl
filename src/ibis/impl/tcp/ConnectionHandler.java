// problem: blocking receive does not work.
// solution:
// A receiveport has a single slot for a message.
// The connectionhandler reads the opcode, and notifies receiveport.
// In a blocking receive, a wait is done until the slot is filled.

package ibis.impl.tcp;

import ibis.ipl.IbisError;

import ibis.io.*;

import ibis.util.*;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

final class ConnectionHandler implements Runnable, TcpProtocol { //, Config {
	private static boolean DEBUG = false;

	private TcpReceivePort port;
	private InputStream input;
	private final DummyInputStream dummy_sun;
	private final BufferedArrayInputStream dummy_ibis;
	TcpSendPortIdentifier origin;
	private SerializationInputStream in;
	TcpReadMessage m;
	volatile boolean iMustDie = false;
	TcpIbis ibis;

	ConnectionHandler(TcpIbis ibis, 
					     TcpSendPortIdentifier origin, 
					     TcpReceivePort port, 
					     InputStream input) throws IOException {
		this.port   = port;
		this.origin = origin;
		this.input = input;
		this.ibis = ibis;

		switch(port.type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
				// always make a new BufferedInputStream: the one in JVM1.4 
				// has side-effect in close()
				dummy_sun = new DummyInputStream(new BufferedInputStream(input, 60*1024));
				dummy_ibis = null;
			break;
		case TcpPortType.SERIALIZATION_IBIS:
				// maybe the dummy should be on the outside ??? Jason
				dummy_ibis = new BufferedArrayInputStream(new DummyInputStream(input));
				dummy_sun = null;
			break;
		default:
			throw new IbisError("EEK: serialization type unknown");
		}

		createNewStream();
	}

	private void createNewStream() throws IOException {
		switch(port.type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
			in = new SunSerializationInputStream(dummy_sun);
			break;
		case TcpPortType.SERIALIZATION_IBIS:
			in = new IbisSerializationInputStream(dummy_ibis);
			break;
		default:
			throw new IbisError("EEK: serialization type unknown");
		}

		m = new TcpReadMessage(port, in, origin, this);
	}

	private void close(Exception e) {
		try {
			if(in != null) {
				in.close();
			}
		} catch (Exception z) {
			// Ignore.
		}
		in = null;
//		ibis.tcpPortHandler.killSocket(origin, input); // not correct.
		ibis.tcpPortHandler.releaseInput(origin, input);
		if(!iMustDie) { // if we came in through a forcedClose, the port already knows that we are gone.
			port.leave(this, e);
		}
	}

	/* called by forcedClose */
	void die() {
		iMustDie = true;
	}

//	static int msgCounter = 0;
	public void run() {
		byte opcode = -1;

		try {
			while (!iMustDie) {
				if(DEBUG) {
					System.err.println("handler " + this + " for port: " + port.name + " woke up");
				}
				opcode = in.readByte();
				if(iMustDie) {	/* in this case, a forcedClose was done, and my port is gone... */
					close(null);
					return;
				}

				if(DEBUG) {
					System.err.println("handler " + this + " for port: " + port.name + ", READ BYTE " + opcode);
				}

				switch (opcode) {
				case NEW_RECEIVER:
					if (DEBUG) {
						TcpReceivePortIdentifier x = (TcpReceivePortIdentifier) in.readObject();
						System.out.println(port.name + ": Got a NEW_RECEIVER " + x + " from " + origin);	
					}
					in.close();
					createNewStream();
					break;
				case NEW_MESSAGE:
					if (DEBUG) {
						System.err.println("handler " + this + " GOT a new MESSAGE " + m + " on port " + port.name);
					}

					if(port.setMessage(m)) {
						// The port created a new reader thread, I must exit.
						return;
					}

					// If the upcall did not release the message, cool, 
					// no need to create a new thread, we are the reader.
					break;
				case CLOSE_CONNECTION:
					if (DEBUG) { 
						System.out.println(port.name + ": Got a FREE from " + origin);	
					}
					close(null);
					return;
				default:
					throw new IbisError(port.name + " EEK TcpReceivePort: " +
									   "run: got illegal opcode: " + opcode +
									   " from: " + origin);
				}
			}
		} catch (IOException e) {
			if(DEBUG) {
				System.err.println("ConnectionHandler.run : " + port.name +
						   " Caught exception " + e);
				System.err.println("I am connected to " + origin);
				e.printStackTrace();
			}

			close(e);
		} catch (Throwable t) {
			close(null);
			throw new IbisError(t);
		}
	}
}
