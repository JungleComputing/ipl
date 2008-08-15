package ibis.ipl.impl.mx;

import ibis.ipl.AlreadyConnectedException;
import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

public class MxChannelFactory implements Runnable {
	/* TODO maybe we should have some collection with all active ReadChannels 
	 * and WriteChannels here, instead of using the SendPorts to locate them, 
	 * or make a separate ChannelManager for that.
	 */ 
	
	static final int IBIS_FILTER = 0xdada0001;
	static final short CONNECT_REPLY_PORT = 0;
	static final int FACTORY_CONNECTION = 0;

	private static Logger logger = Logger.getLogger(MxChannelFactory.class);

	MxAddress address;
	int endpointId;

	IdManager<Identifier> idm;
	private MxIbis ibis;
	private boolean listening = false;
	private boolean closed = false;
	
	private int disconnectMessageSize = 0;
	private ByteBuffer disconnectMessageBuffer = ByteBuffer.allocateDirect(disconnectMessageSize);

	public MxChannelFactory(MxIbis ibis) throws MxException {
		if(JavaMx.initialized == false) {
			throw new MxException("could not initialize JavaMX");
		}
		this.ibis = ibis;
		this.endpointId = JavaMx.newEndpoint(IBIS_FILTER);
		this.address = new MxAddress(JavaMx.getMyNicId(endpointId), JavaMx.getMyEndpointId(endpointId));
		ThreadPool.createNew(this, "MxChannelFactory");
		idm = new IdManager<Identifier>();
	}

	public WriteChannel connect(MxSendPort sp,
			ReceivePortIdentifier rpi, long timeoutMillis, boolean fillTimeout, boolean reliable) throws IOException {		
		MxReadChannel rc = null;
		MxDataInputStream mxdis = null;
		MxWriteChannel wc = null;
		MxDataOutputStream mxdos = null;
		DataOutputStream dos = null;
		byte reply = 0;
		Identifier channelId = new Identifier();
		long deadline = 0;

		if(closed) {
			throw new IOException("Endpoint is closed");
		}

		logger.debug("Connecting...");
		
		IbisIdentifier id = (ibis.ipl.impl.IbisIdentifier) rpi.ibisIdentifier();
		if(id.equals(ibis.ident)) {
			/* 
			 * Crap, the ReceivePort is on the same Ibis and an MX Endpoint seems to dislike sending messages to itself.
			 * We have to build a local loop for this connection ourselves...
			 */
			return createLocalChannel(sp, (MxReceivePort)(ibis.findReceivePort(rpi.name())), timeoutMillis, fillTimeout);		
		}
		
		if(timeoutMillis > 0) {
			deadline = System.currentTimeMillis() + timeoutMillis;
		}

		MxAddress target;
		try {
			target = MxAddress.fromBytes(id.getImplementationData());
		} catch (Exception e) {
			throw new PortMismatchException("Could not create MxAddress from ReceivePortIdentifier.", rpi, e);
		}
		
		while(deadline == 0 || System.currentTimeMillis() < deadline) {
			//set up a channel to receive the response
			rc = new MxReadChannel(this);
	
			try {
				idm.insert(channelId);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			rc.matchData = Matching.construct(Matching.PROTOCOL_CONNECT_REPLY, CONNECT_REPLY_PORT, channelId.getIdentifier());
			
			mxdis = new MxDataInputStreamImpl(rc);
	
			//setup the channel to send the request
			
			try {
				if(reliable) {
					wc = new MxReliableWriteChannel(this, target, IBIS_FILTER);
				} else {
					wc = new MxUnreliableWriteChannel(this, target, IBIS_FILTER);
				}
			} catch (IOException e) {
				mxdis.close();
				logger.error(e.getMessage());
				idm.remove(channelId.getIdentifier());
				throw(e);
			}
			
			wc.matchData = Matching.construct(Matching.PROTOCOL_CONNECT, FACTORY_CONNECTION);
			logger.debug("MatchData is for connection request is " + Long.toHexString(wc.matchData));
			mxdos = new MxScatteringDataOutputStream(wc); //wc is big endian here
			dos = new DataOutputStream(mxdos);
	
			//send the request
			try {
				dos.writeUTF(rpi.name());
				sp.ident.writeTo(dos);
				sp.type.writeTo(dos);
				// The sender chooses the byte order, because the sender may want to send the same buffer to multiple receivers

				dos.writeInt(Matching.getConnection(rc.matchData)); // the connection to write the answer to
				
				logger.debug("request created...");

				dos.flush();
				mxdos.finish();
				logger.debug("request sent!");
			} catch (IOException e) {
				idm.remove(channelId.getIdentifier());
				rc.close();
				wc.close();
				throw(e);
			}
	
			//read the reply
			if(deadline > 0) {
				logger.debug("waiting for reply with timeout...");
				if(mxdis.waitUntilAvailable(System.currentTimeMillis() -  deadline) < 0) {
					//FIXME breaks the handshake when the other side is just plain slow
					//	no message arrived in time;
					rc.close();
					wc.close();
					idm.remove(channelId.getIdentifier());
					throw new ConnectionTimedOutException("Connection request timed out",rpi);
				}
			} else { //no timeouts
				logger.debug("waiting for reply...");
				if(mxdis.waitUntilAvailable(0) < 0) {
					rc.close();
					wc.close();
					idm.remove(channelId.getIdentifier());
					// what happened, no data?
					throw new ConnectionTimedOutException("Connection request timed out",rpi);
				}
			}
			logger.debug("reading reply...");
			reply = mxdis.readByte();
			if (reply != ReceivePort.ACCEPTED) {
				logger.debug("connection creation failed");
				// failure
				wc.close();
				rc.close();
				switch (reply) {
					case ReceivePort.ALREADY_CONNECTED:
						idm.remove(channelId.getIdentifier());
						logger.debug("Connection rejected: ALREADY_CONNECTED");
						throw new AlreadyConnectedException("Already connected to ReceivePort", rpi);
					case ReceivePort.TYPE_MISMATCH:
						idm.remove(channelId.getIdentifier());
						logger.debug("Connection rejected: TYPE_MISMATCH");
						throw new PortMismatchException("PortTypes do not match", rpi);
					case ReceivePort.DISABLED:
						logger.debug("Connection rejected: DISABLED");
					case ReceivePort.NOT_PRESENT:
						logger.debug("Connection rejected: NOT_PRESENT");
						if (fillTimeout) {
							// port not enabled, wait a while and try again
							try {
								Thread.sleep(100);
							} catch (Exception e) {
		                    // 	IGNORE
							}
			                rc.close();
			                idm.remove(channelId.getIdentifier());               
			                continue;
						} else { // !fillTimeout
							idm.remove(channelId.getIdentifier());
							logger.debug("Connection rejected: timeout occured");
							throw new ConnectionRefusedException("ReceivePort not active", rpi);
						}
					case ReceivePort.NO_MANY_TO_X:
						logger.debug("Connection rejected: NO_MANY_TO_X");
						idm.remove(channelId.getIdentifier());
						throw new ConnectionRefusedException("ReceivePort already occupied", rpi);
					case ReceivePort.DENIED:
						logger.debug("Connection rejected: DENIED");
						idm.remove(channelId.getIdentifier());
						throw new ConnectionRefusedException("Receiver denied connection", rpi);
					default:
						logger.debug("Connection rejected: Unknown reason");
						idm.remove(channelId.getIdentifier());
						throw new ConnectionFailedException("Unknown response received", rpi);
				}
			}
	
			// Connection is accepted...
			if (logger.isInfoEnabled()) {
				logger.info("connection created");
			}
			// get the connection
			wc.matchData = Matching.setConnection(wc.matchData, mxdis.readInt());
			wc.matchData = Matching.setProtocol(wc.matchData, Matching.PROTOCOL_DATA);
			
			rc.close();
			idm.remove(channelId.getIdentifier());
			logger.debug("connect finished successfully");
			return wc;
		}
		throw new ConnectionTimedOutException("Connection request timed out",rpi);
	}

	private WriteChannel createLocalChannel(MxSendPort sp,
			MxReceivePort rp, long timeoutMillis, boolean fillTimeout) throws IOException {
		@SuppressWarnings("unused")
		MxReceivePortConnectionInfo info;
		MxLocalChannel channel = null;
		MxDataInputStream mxdis;
		
		logger.debug("creating local channel");
		
		long deadline = 0;
		if(timeoutMillis > 0) {
			deadline = System.currentTimeMillis() + timeoutMillis;
		}
		
		while(deadline == 0 || System.currentTimeMillis() < deadline) {
			byte reply = rp.connectionAllowed(sp.ident, sp.type);
			if(reply == ReceivePort.ACCEPTED) {
				channel = new MxLocalChannel(); 
				mxdis = new MxDataInputStreamImpl(channel);
				info = new MxReceivePortConnectionInfo(sp.ident, rp, mxdis, this);
				return channel;
			} else {
				logger.debug("connection creation failed");
				switch (reply) {
					case ReceivePort.ALREADY_CONNECTED:
						throw new AlreadyConnectedException("Already connected to ReceivePort " + rp.name(), rp.ident);
					case ReceivePort.TYPE_MISMATCH:
						throw new PortMismatchException("PortTypes do not match", rp.ident);
					case ReceivePort.DISABLED:
					case ReceivePort.NOT_PRESENT:
						if (fillTimeout) {
							// port not enabled, wait a while and try again
							try {
								Thread.sleep(100);
							} catch (Exception e) {
		                    // 	IGNORE
							}
			                continue;
						} else { // !fillTimeout
							throw new ConnectionRefusedException("ReceivePort not active", rp.ident);
						}
					case ReceivePort.NO_MANY_TO_X:
						throw new ConnectionRefusedException("ReceivePort already occupied", rp.ident);
					case ReceivePort.DENIED:
						throw new ConnectionRefusedException("Receiver denied connection", rp.ident);
					default:
						throw new ConnectionFailedException("Unknown response received", rp.ident);
				}
			}
		}
		throw new ConnectionTimedOutException("Connection request timed out",rp.identifier());
	}

	public void close() {
		synchronized(this) {
			if(listening) {
				listening = false;
				JavaMx.wakeup(endpointId);
				try {
					wait();
				} catch (InterruptedException e) {
					// ignore
				}
			} else {
				JavaMx.wakeup(endpointId);
			}
			
		}
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
		MxDataInputStream mxdis;
		DataInputStream dis;
		MxReceivePort port = null;

		MxWriteChannel wc = null;
		MxDataOutputStream mxdos;

		String name;
		PortType capabilities = null;	

/*		if (logger.isDebugEnabled()) {
			logger.debug("Listening at " + address.toString());
		}*/
		
		//setup the channel to read the request
		rc = new MxReadChannel(this);
		rc.matchData = Matching.setConnection(rc.matchData, FACTORY_CONNECTION);
		rc.matchData = Matching.setProtocol(rc.matchData, Matching.PROTOCOL_CONNECT);

		
		// TODO When multiple connection requests arrive at the same time, message can be mixed up for requests consisting of multiple MX messages
		mxdis = new MxDataInputStreamImpl(rc);
		dis = new DataInputStream(mxdis);
		MxAddress replyAddress;
		byte reply;

		if(closed) {
			throw new IOException("Endpoint is closed");
		}
		
		try {
			//read the request

			//logger.debug("reading request");

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
						mxdis = new MxDataInputStreamImpl(rc); 
						
						info = new MxReceivePortConnectionInfo(spi, port, mxdis, this);
						// setup the Matching properties of the ReadChannel
						rc.matchData = Matching.construct(Matching.PROTOCOL_DATA, port.getIdentifier(), info.getIdentifier());
						// channel is now connected to the ReceivePort

					} catch (IOException e) {
						//TODO proper handling
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
			rc.setSender(replyAddress);
			// setup the channel to send the reply
			try {
				wc = new MxUnreliableWriteChannel(this, replyAddress, IBIS_FILTER);
				wc.matchData = Matching.construct(Matching.PROTOCOL_CONNECT_REPLY, dis.readInt());
			} catch (IOException e) {
				logger.error(e.getMessage());
				//Throw an exception? No, we don't want to 'kill' the listening thread?
				dis.close();
				mxdis.close();
				rc.close();
				return false;
			}
			
			mxdos = new MxScatteringDataOutputStream(wc);
			//write the reply
			mxdos.writeByte(reply);

			if (reply == ReceivePort.ACCEPTED) {
				// also write port number
				mxdos.writeInt(Matching.getConnection(rc.matchData));
			}
			mxdos.flush();
			mxdos.finish();

			if (reply != ReceivePort.ACCEPTED) {
				dis.close();
				mxdis.close();
				rc.close();
				if (logger.isInfoEnabled()) {
					logger.info("receiveport rejected connection: " + reply);
				}
				wc.close();
				return false;
			}	
			if (logger.isInfoEnabled()) {
				logger.info("connection created");
			}
			wc.close();
			return true;
		} catch (IOException e1) {
			// General Java IO and MX errors, they should not happen. We ignore them for now
			if(port != null) {
				port.close(10); // release resources
			}
			if (logger.isDebugEnabled()) {
				logger.debug(e1.getMessage());
			}
			if(wc != null) {
				wc.close();
			}
			return false;
		}
	}

	public void run() {
		// a "Listen" thread
		listening = true;
		long matching = Matching.NONE;
		long protocol = Matching.NONE;
		
		while(true) {
			synchronized(this) {
				if(!listening) {
					logger.debug("ChannelFactory stops listening");
					notifyAll();
					return;
				}
			}
			
			matching = JavaMx.waitForMessage(endpointId, 0, Matching.FACTORY_THREAD_MATCH, Matching.FACTORY_THREAD_MASK);
			if (logger.isDebugEnabled()) {
				logger.debug("New control message arrived:" + Long.toHexString(matching));
			}
			
			if(matching == Matching.NONE) {
				logger.debug("waitForMessage() did not deliver a message!");
				//no message arrived
				continue;
			}
			protocol = Matching.getProtocol(matching);

			if(protocol == Matching.PROTOCOL_DISCONNECT) {
				// remote SendPort closes
				senderClosedConnection(matching);
			} else if(protocol == Matching.PROTOCOL_CLOSE) {
				//remote ReceivePort closes
				receiverClosedConnection(matching);
			} else if(protocol == Matching.PROTOCOL_CONNECT) {
				try {
					listen();
				} catch (IOException e) {
					// Endpoint is closed, so stop listening
					listening = false;
				}
			} else {
				//we should not handle these messages here, do nothing
				//FIXME read it to prevent a deadlock?
				logger.info("Unknown control message arrived!");
				//System.exit(1);
			}
		}		
	}

	protected void sendDisconnectMessage(MxWriteChannel channel) {
		logger.debug("sendDisconnectMessage()");
		// uses the send link of the requesting channel
		long closeMatchData = Matching.setProtocol(channel.matchData, Matching.PROTOCOL_DISCONNECT);
		int closeHandle = JavaMx.handles.getHandle();
		//FIXME prevent buffer allocation
		JavaMx.send(disconnectMessageBuffer, 0, disconnectMessageSize, endpointId, channel.link, closeHandle, closeMatchData);
		int result = -1;
		try {
			result = JavaMx.wait(endpointId, closeHandle, 1000);
		} catch (MxException e1) {
			//stop trying to receive the message
			logger.warn("Error sending the disconnect signal: " + e1.getMessage());
		}
		if(result == -1) {
			// error waiting for the message completion, ignore here
			//TODO can we also cancel a send operation? Research this
			if(JavaMx.cancel(endpointId, closeHandle) == false) {
				// cancel failed, because request is already finished
				// we have to complete the request by calling 'test' or 'wait'
				try {
					JavaMx.test(endpointId, closeHandle);
				} catch (MxException e) {
					// TODO ignore it for now
				}
			}
		}
		JavaMx.handles.releaseHandle(closeHandle);
	}
	
	private void senderClosedConnection(long matchData) {
		logger.debug("senderClosedConnection()");
		//receive the message
		//TODO avoid buffer creation and handle use?
		int handle = JavaMx.handles.getHandle();
		
		try {
			JavaMx.recv(disconnectMessageBuffer, 0, disconnectMessageSize, endpointId, handle, matchData);
			JavaMx.wait(endpointId, handle);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JavaMx.handles.releaseHandle(handle);

		// lookup the corresponding channel
		ReadChannel rc = findReceiveConnection(matchData);
		
		if(rc != null) {
			if (rc instanceof MxReadChannel) {
				// FIXME DEBUG: fixing closing channels
				((MxReadChannel)rc).senderClose();
			} else { // we have a local channel
				// FIXME DEBUG: fixing closing channels
				rc.close();
			}
		}
		logger.debug("senderClosedConnection() finished");
	}
	
	protected void sendCloseMessage(MxReadChannel rc) {
		MxWriteChannel wc = null;

		if(closed) {
			// TODO throw new IOException("Endpoint is closed"); ??
			return;
		}
		
		// now locate the remote sendport
		MxAddress target= rc.getSender();
		if (target == null) {
			// we don't know to who we need to send the message
			return;
		}
		
		//setup the channel to send the close message		
		try {
			//TODO prevent channel creation, do communication myself
			wc = new MxUnreliableWriteChannel(this, target, IBIS_FILTER);
		} catch (IOException e) {
			logger.error(e.getMessage());
			// TODO just stop here, do not crash on failing to close a connection
			return;
		}		
		long matchData = Matching.construct(Matching.PROTOCOL_CLOSE, Matching.getConnection(rc.matchData));
			
		//send the message
		int closeHandle = JavaMx.handles.getHandle();
		JavaMx.send(disconnectMessageBuffer, 0, disconnectMessageSize, endpointId, wc.link, closeHandle, matchData);
		int result = -1;
		try {
			result = JavaMx.wait(endpointId, closeHandle, 1000);
		} catch (MxException e1) {
			//stop trying to receive the message
			logger.warn("Error sending the close signal: " + e1.getMessage());
		}
		if(result == -1) {
			// error waiting for the message completion, ignore here
			//TODO can we also cancel a send operation? Research this
			if(JavaMx.cancel(endpointId, closeHandle) == false) {
				// cancel failed, because request is already finished
				// we have to complete the request by calling 'test' or 'wait'
				try {
					JavaMx.test(endpointId, closeHandle);
				} catch (MxException e) {
					// TODO ignore it for now
				}
			}
		}
		JavaMx.handles.releaseHandle(closeHandle);
		// FIXME DEBUG: fixing closing channels
		wc.close();
	}
	
	private void receiverClosedConnection(long matchData) {
		int handle = JavaMx.handles.getHandle();
		
		try {
			JavaMx.recv(disconnectMessageBuffer, 0, disconnectMessageSize, endpointId, handle, matchData);
			JavaMx.wait(endpointId, handle);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JavaMx.handles.releaseHandle(handle);

		MxWriteChannel wc = null;
		// FIXME find the associated writeChannel and close it
		if (wc != null) {
			wc.receiverClosed();
		}
	}
	
	private ReadChannel findReceiveConnection(long matchData) {
		if(Matching.getReceivePort(matchData) == 0) {
			//it is a connection to the factory
			return null;
		}
		MxReceivePort rp = ibis.receivePortManager.find(Matching.getReceivePort(matchData));
		if(rp == null) {
			// unknown rp
			return null;
		}
		MxReceivePortConnectionInfo info = rp.channelManager.find(Matching.getChannel(matchData));
		if (info == null) {
			return null;
		}
		return ((MxDataInputStreamImpl)info.dataIn).channel;	
	}

}
