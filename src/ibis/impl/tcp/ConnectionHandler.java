// problem: blocking receive does not work.
// solution:
// A receiveport has a single slot for a message.
// The connectionhandler reads the opcode, and notifies receiveport.
// In a blocking receive, a wait is done until the slot is filled.

package ibis.impl.tcp;

import ibis.io.BufferedArrayInputStream;
import ibis.io.DataSerializationInputStream;
import ibis.io.DummyInputStream;
import ibis.io.IbisSerializationInputStream;
import ibis.io.NoSerializationInputStream;
import ibis.io.SerializationInputStream;
import ibis.io.SunSerializationInputStream;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIOException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

final class ConnectionHandler implements Runnable, TcpProtocol { //, Config {
	private static boolean DEBUG = false;

	private TcpReceivePort port;
	private InputStream input;
	final DummyInputStream dummy;
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
				dummy = new DummyInputStream(new BufferedInputStream(input, 60*1024));
				dummy_ibis = null;
			break;
		case TcpPortType.SERIALIZATION_NONE:
				// always make a new BufferedInputStream: the one in JVM1.4 
				// has side-effect in close()
				dummy = new DummyInputStream(new BufferedInputStream(input, 60*1024));
				dummy_ibis = null;
			break;
		case TcpPortType.SERIALIZATION_IBIS:
		case TcpPortType.SERIALIZATION_DATA:
				// maybe the dummy should be on the outside ??? Jason
				dummy = new DummyInputStream(input);
				dummy_ibis = new BufferedArrayInputStream(dummy);
			break;
		default:
			throw new IbisError("EEK: serialization type unknown");
		}

		createNewStream();
	}

	private void createNewStream() throws IOException {
		switch(port.type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
			in = new SunSerializationInputStream(dummy);
			break;
		case TcpPortType.SERIALIZATION_NONE:
			in = new NoSerializationInputStream(dummy);
			break;
		case TcpPortType.SERIALIZATION_IBIS:
			in = new IbisSerializationInputStream(dummy_ibis);
			break;
		case TcpPortType.SERIALIZATION_DATA:
			in = new DataSerializationInputStream(dummy_ibis);
			break;
		default:
			throw new IbisError("EEK: serialization type unknown");
		}

		m = new TcpReadMessage(port, in, origin, this);
	}

	void close(Exception e) {
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
		if(!iMustDie) { // if we came in through a forced close, the port already knows that we are gone.
			port.leave(this, e);
		}
	}

	/* called by forced close */
	void die() {
		iMustDie = true;
	}

//	static int msgCounter = 0;
	public void run() {
		try {
			reader();
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


	void reader() throws IOException {
		byte opcode = -1;

		while (!iMustDie) {
			if(DEBUG) {
				System.err.println("handler " + this + " for port: " + port.name + " woke up");
			}
			opcode = in.readByte();
			if(iMustDie) {	/* in this case, a forced close was done, and my port is gone... */
				close(null);
				return;
			}

			if(DEBUG) {
				System.err.println("handler " + this + " for port: " + port.name + ", READ BYTE " + opcode);
			}

			switch (opcode) {
			case NEW_RECEIVER:
				if (DEBUG) {
					try {
					    TcpReceivePortIdentifier x = (TcpReceivePortIdentifier) in.readObject();
					    System.out.println(port.name + ": Got a NEW_RECEIVER " + x + " from " + origin);	
					} catch(ClassNotFoundException e) {
					    throw new IbisIOException(e);
					}
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
					// Also when there is no separate connectionhandler thread.
					return;
				}

				// If the upcall did not release the message, cool, 
				// no need to create a new thread, we are the reader.
				break;
			case CLOSE_ALL_CONNECTIONS:
				if (DEBUG) { 
					System.out.println(port.name + ": Got a FREE from " + origin);	
				}
				close(null);
				return;
			case CLOSE_ONE_CONNECTION:
				TcpReceivePortIdentifier identifier;
				//the identifier of the receiveport which whould disconnect is coming next
				switch(port.type.serializationType) {
				case TcpPortType.SERIALIZATION_SUN:
				case TcpPortType.SERIALIZATION_IBIS:
					try {
					    identifier = (TcpReceivePortIdentifier) in.readObject();
					    if(identifier.equals(port.identifier())) {
						if (DEBUG) {
							System.out.println(port.name + ": got a disconnect from: " + origin);
						}
						close(null);
						return;
					    }
					} catch(ClassNotFoundException e)  {
						if (DEBUG) {
							System.out.println(port.name + ": disconnect from: " + origin + 
							 " failed");
						}
					}
					//someone else is closing down, just reset the stream
					in.close();
					createNewStream();
					break;
				}
			default:
				throw new IbisError(port.name + " EEK TcpReceivePort: " +
								   "run: got illegal opcode: " + opcode +
								   " from: " + origin);
			}
		}
		return;
	}
}
