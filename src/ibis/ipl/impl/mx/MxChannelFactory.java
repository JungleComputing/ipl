package ibis.ipl.impl.mx;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

public class MxChannelFactory implements Runnable {

	static final int IBIS_FILTER = 0xdada0001;
	static final int CONNECT_PORT = 0;

	private static Logger logger = Logger.getLogger(MxChannelFactory.class);

	MxAddress address;
	int endpointId;

	IdManager<MxReadChannel> idm;
	private MxIbis ibis;
	private boolean listening = false;
	private boolean closed = false;

	public MxChannelFactory() {
		this.endpointId = JavaMx.newEndpoint(IBIS_FILTER);
		this.address = new MxAddress(JavaMx.getMyNicId(endpointId), JavaMx.getMyEndpointId(endpointId));
		ThreadPool.createNew(this, "MxChannelFactory");
		idm = new IdManager<MxReadChannel>();
	}

	/*	protected MxAddress getAddress(String hostname, int endpointId) {
		return new MxAddress(hostname, endpointId);
	}
	 */

	public MxWriteChannel connect(MxSendPort sp,
			ReceivePortIdentifier rpi, long timeoutMillis) throws IOException { //throws IOException  {
		if(closed) {
			throw new IOException("Endpoint is closed");
		}
		
		
		
		// TODO timeouts

		IbisIdentifier id = (ibis.ipl.impl.IbisIdentifier) rpi.ibisIdentifier();
		MxAddress target;
		try {
			target = MxAddress.fromBytes(id.getImplementationData());
		} catch (Exception e) {
			throw new PortMismatchException("Could not create MxAddress from ReceivePortIdentifier.", rpi, e);
		}
		//set up a channel to receive the response
		MxReadChannel rc = new MxReadChannel(this);

		MxId<MxReadChannel> channelId = idm.get();
		channelId.owner = rc;		// TODO Do we need this?
		rc.setReceivePort((short) 0);
		rc.setChannel(channelId.value);
		rc.setProtocol(Matching.CONNECT_REPLY);
		MxSimpleDataInputStream mxdis = new MxSimpleDataInputStream(rc, ByteOrder.BIG_ENDIAN);

		//setup the channel to send the request
		MxWriteChannel wc;
		try {
			wc = new MxUnreliableWriteChannel(this, target, IBIS_FILTER);
		} catch (IOException e) {
			mxdis.close();
			logger.error(e.getMessage());
			channelId.remove();
			return null;
		}
		MxSimpleDataOutputStream mxdos = new MxSimpleDataOutputStream(wc, ByteOrder.BIG_ENDIAN);
		DataOutputStream dos = new DataOutputStream(mxdos);

		//send the request
		wc.setConnection(CONNECT_PORT);
		wc.setProtocol(Matching.CONNECT);
		try {
			dos.writeUTF(rpi.name());
			sp.ident.writeTo(dos);
			sp.type.writeTo(dos);

			dos.writeInt(rc.getConnection());
			dos.flush();
		} catch (IOException e) {
			channelId.remove();
			//TODO Do I really need to close them explicitly? Garbage collection could also be suffice.
			rc.close();
			wc.close();
			throw(e);
		}

		//read the reply
		byte reply = mxdis.readByte();
		if (reply != ReceivePort.ACCEPTED) {
			wc.close();
			rc.close();
			// failure
			switch (reply) {
				case ReceivePort.ALREADY_CONNECTED:
					channelId.remove();
					throw new AlreadyConnectedException("Already connected to ReceivePort", rpi);
				case ReceivePort.TYPE_MISMATCH:
					channelId.remove();
					throw new PortMismatchException("PortTypes do not match", rpi);
				case ReceivePort.DISABLED:
				case ReceivePort.NOT_PRESENT:
					// TODO try again later?
					channelId.remove();
					throw new ConnectionRefusedException("ReceivePort not active", rpi);
				case ReceivePort.NO_MANY_TO_X:
					channelId.remove();
					throw new ConnectionRefusedException("ReceivePort already occupied", rpi);
				case ReceivePort.DENIED:
					channelId.remove();
					throw new ConnectionRefusedException("Receiver denied connection", rpi);
				default:
					channelId.remove();
					throw new ConnectionFailedException("Unknown response received", rpi);
			}
		}

		// Connection is accepted...

		// get the port number
		wc.setConnection(mxdis.readInt());
		wc.setProtocol(Matching.DATA);
		rc.close();
		channelId.remove();
		
		return wc;
	}

	public void close() {
		listening = false;
		JavaMx.closeEndpoint(endpointId);
		closed = true;
	}

	/**
	 * @return true when a connection is established
	 * @throws IOException 
	 */
	public boolean listen() throws IOException {
		MxReadChannel rc;
		SendPortIdentifier spi = null;
		ReceivePortIdentifier rpi = null;
		@SuppressWarnings("unused")
		MxReceivePortConnectionInfo info;
		MxSimpleDataInputStream mxdis;
		DataInputStream dis;
		MxReceivePort port = null;

		MxWriteChannel wc;
		MxSimpleDataOutputStream mxdos;

		String name;
		PortType capabilities = null;	

		//setup the channel to read the request
		rc = new MxReadChannel(this);
		rc.matchData = Matching.PROTOCOL_MASK; // port and connection is 0 now
		rc.setProtocol(Matching.CONNECT);
		// FIXME When multiple connection requests arrive at the same time, message can be mixed up for requests consisting of multiple MX messages
		mxdis = new MxSimpleDataInputStream(rc, ByteOrder.BIG_ENDIAN);
		dis = new DataInputStream(mxdis);
		MxAddress replyAddress;
		byte reply;

		if(closed) {
			throw new IOException("Endpoint is closed");
		}
		
		try {
			//read the request
			name = dis.readUTF();

			spi = new SendPortIdentifier(dis);
			capabilities = new PortType(dis);
			
			// Check whether connection is allowed
			rpi = new ReceivePortIdentifier(name, ibis.ident);
			port = (MxReceivePort) ibis.findReceivePort(name);

			if (port == null) {
				logger.error("could not find receiveport, connection denied");
				reply = ReceivePort.NOT_PRESENT;
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("giving new connection to receiveport " + rpi);
				}

				// register connection with ReceivePort
				reply = port.connectionAllowed(spi, capabilities);

				if (reply == ReceivePort.ACCEPTED) {
					try {
						info = new MxReceivePortConnectionInfo(spi, port, mxdis);

						// setup the Matching properties of the ReadChannel
						rc.setReceivePort(port.portId.value);
						rc.setChannel(info.channelId.value);
						rc.setProtocol(Matching.DATA);
						// channel is now connected to the ReceivePort

					} catch (IOException e) {
						e.printStackTrace();
						reply = ReceivePort.DENIED;
					}
				}				
			}

			// get the address of the reply sendport
			replyAddress = MxAddress.fromBytes(((ibis.ipl.impl.IbisIdentifier)spi.ibisIdentifier()).getImplementationData());
			if(replyAddress == null) {
				//send sent an invalid mx address, so we cannot send a reply.
				rc.close();
				return false;
			}

			// setup the channel to send the reply
			try {
				wc = new MxUnreliableWriteChannel(this, replyAddress, IBIS_FILTER);
				wc.setConnection(dis.readInt()); // set the port to send the reply to
				wc.setProtocol(Matching.CONNECT_REPLY);
			} catch (IOException e) {
				logger.error(e.getMessage());
				//Throw an exception? No, we don't want to 'kill' the listening thread?
				dis.close();
				rc.close();
				return false;
			}
			
			mxdos = new MxSimpleDataOutputStream(wc, ByteOrder.BIG_ENDIAN);
			//write the reply
			mxdos.writeByte(reply);

			if (reply == ReceivePort.ACCEPTED) {
				// also write port number					

				mxdis.resetBytesRead();
				mxdos.writeInt(rc.getConnection());
			}
			mxdos.flush();
			mxdos.finish();

			if (reply != ReceivePort.ACCEPTED) {
				rc.close();
				dis.close();
				if (logger.isInfoEnabled()) {
					logger.info("receiveport rejected connection");
				}
				return false;
			}	
			return true;
		} catch (IOException e1) {
			// General Java IO and MX errors, they should not happen. We ignore them for now
			if(port != null) {
				port.close(10); // release resources
			}
			logger.debug(e1.getMessage());
			return false;
		}
	}

	public void run() {
		// a "Listen" thread
		listening = true;
		while(listening) {	
			try {
				listen();
			} catch (IOException e) {
				// Endpoint is closed, so stop listening
				listening = false;
			}
		}
	}

}
