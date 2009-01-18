package ibis.ipl.impl.mx.channels;

import ibis.util.ThreadPool;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

public class MxSocket implements Runnable {

	private static Logger logger = Logger.getLogger(MxSocket.class);

	private static final int IBIS_FILTER = 0xdada1313;
	private static final long POLL_FOR_CLOSE_INTERVAL = 500;
	protected static final int MAX_CONNECT_MSG_SIZE = 2048 + MxAddress.SIZE;
	// TODO limit on CONNECT message size, document this
	protected static final int MAX_CONNECT_REPLY_MSG_SIZE = MAX_CONNECT_MSG_SIZE - 5;

	// listenBuf - int - byte;

	public static boolean available() {
		return JavaMx.initialized;
	}

	private MxAddress myAddress;
	private HashMap<Short, ChannelManager> managers;
	private HashMap<String, WriteChannel> writeChannels;
	private int nextKey = 0;
	private MxListener listener = null;
	private boolean closed = false, closing = false;
	private int endpointNumber;
	private int sendEndpointNumber;
	private ByteBuffer listenBuf, connectBuf;
	private int listenHandle, connectHandle;

	public MxSocket(MxListener listener) throws MxException {
		if (listener == null) {
			throw new MxException("no listener");
		}
		this.listener = listener;
		if (JavaMx.initialized == false) {
			throw new MxException("could not initialize JavaMX");
		}
		endpointNumber = JavaMx.newEndpoint(IBIS_FILTER);
		sendEndpointNumber = JavaMx.newEndpoint(IBIS_FILTER);

		myAddress = new MxAddress(JavaMx.getMyNicId(endpointNumber), JavaMx
				.getMyEndpointId(endpointNumber));

		listenBuf = ByteBuffer.allocateDirect(MAX_CONNECT_MSG_SIZE).order(
				ByteOrder.BIG_ENDIAN);
		listenHandle = JavaMx.handles.getHandle();
		connectBuf = ByteBuffer.allocateDirect(MAX_CONNECT_MSG_SIZE).order(
				ByteOrder.BIG_ENDIAN);
		connectHandle = JavaMx.handles.getHandle();

		managers = new HashMap<Short, ChannelManager>();
		writeChannels = new HashMap<String, WriteChannel>();

		ThreadPool.createNew(this, "MxSocket " + endpointNumber + " - "
				+ sendEndpointNumber);
	}

	public synchronized ChannelManager createChannelManager() throws IOException {
		for (int i = 0; i < Short.MAX_VALUE; i++) {
			nextKey = (nextKey % Short.MAX_VALUE) + 1; // 0 will not be used
			if (!managers.containsKey(nextKey)) {
				ChannelManager manager = new ChannelManager(this,
						(short) nextKey);
				managers.put((short) nextKey, manager);
				if (logger.isDebugEnabled()) {
					logger.debug("ChannelManager " + nextKey  + " created");
				}
				return manager;
			}
		}
		throw new MxException("maximum number of connectionManagers reached");
	}

	protected synchronized Connection connect(ChannelManager channelManager,
			MxAddress target, byte[] descriptor) throws MxException {
		// TODO catch exceptions, forward them
		int msgSize;

		connectBuf.clear();
		try {
			connectBuf.put(myAddress.toBytes());
			connectBuf.put(descriptor);
		} catch (BufferOverflowException e) {
			throw new MxException("descriptor too long.");
		}

		int link = JavaMx.links.getLink();
		if (JavaMx.connect(sendEndpointNumber, link, target.nicId, target.endpointId,
				IBIS_FILTER) == false) {
			// TODO exception, target not found
			JavaMx.links.releaseLink(link);
			throw new MxException("error");
		}

		// send request
		connectBuf.flip();
		JavaMx.sendSynchronous(connectBuf, connectBuf.position(), connectBuf
				.remaining(), sendEndpointNumber, link, connectHandle,
				Matching.PROTOCOL_CONNECT);
		msgSize = JavaMx.wait(sendEndpointNumber, connectHandle); //TODO timeout
		if (msgSize < 0) {
			// FIXME error
			JavaMx.disconnect(link);
			JavaMx.links.releaseLink(link);
			throw new MxException("error");
		}
		
		// read reply
		connectBuf.clear();
		JavaMx.recv(connectBuf, connectBuf.position(), connectBuf.remaining(),
				endpointNumber, connectHandle, Matching.PROTOCOL_CONNECT_REPLY);
		msgSize = JavaMx.wait(endpointNumber, connectHandle); //TODO timeout
		if (msgSize < 0) {
			// FIXME error
			JavaMx.disconnect(link);
			JavaMx.links.releaseLink(link);
			throw new MxException("error");
		}
		connectBuf.limit(msgSize);
		byte reply = connectBuf.get();
		byte[] replymsg;
		switch (reply) {
		case Connection.ACCEPT:
			long matchData = Matching.construct(Matching.PROTOCOL_DATA,
					connectBuf.getInt());
			WriteChannel wc = new WriteChannel(this, sendEndpointNumber, link,
					matchData, target);
			addWriteChannel(wc);
			replymsg = new byte[connectBuf.remaining()];
			connectBuf.get(replymsg);
			return new Connection(wc, reply, replymsg);
		case Connection.REJECT:
			// TODO clean up
			JavaMx.disconnect(link);
			JavaMx.links.releaseLink(link);
			replymsg = new byte[connectBuf.remaining()];
			connectBuf.get(replymsg);
			return new Connection(null, reply, replymsg);
		default:
			JavaMx.disconnect(link);
			JavaMx.links.releaseLink(link);
			throw new MxException("invalid reply");
		}
	}

	public void run() {
		// TODO create a thread for listening and control messages

		while (!closed) {
			long matching = JavaMx.waitForMessage(endpointNumber,
					POLL_FOR_CLOSE_INTERVAL, Matching.ENDPOINT_TRAFFIC,
					Matching.ENDPOINT_THREAD_TRAFFIC_MASK);

			if (matching == Matching.NONE) {
				// no message arrived, timeout
				continue;
			}
			long protocol = Matching.getProtocol(matching);

			if (protocol == Matching.PROTOCOL_DISCONNECT) {
				if (logger.isDebugEnabled()) {
					logger.debug("DISCONNECT message received");
				}
				// remote SendPort closes
				senderClosedConnection(matching);
			} else if (protocol == Matching.PROTOCOL_CLOSE) {
					if (logger.isDebugEnabled()) {
					logger.debug("CLOSE message received");
				}
				// remote ReceivePort closes
				receiverClosedConnection(matching);
			} else if (protocol == Matching.PROTOCOL_CONNECT) {
				if (logger.isDebugEnabled()) {
					logger.debug("CONNECT message received");
				}
				if (closing) {
					// TODO reject the request and continue
				}
				try {
					listen(matching);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				if (logger.isDebugEnabled()) {
					logger.debug("Unknown message received");
				}
				// unknown control message arrived
				// FIXME read it to prevent a deadlock?
			}
		}
	}

	private void receiverClosedConnection(long matchData) {
		listenBuf.clear();
		try {
			JavaMx.recv(listenBuf, listenBuf.position(), listenBuf.remaining(),
					endpointNumber, listenHandle, matchData);
			int size = JavaMx.wait(endpointNumber, listenHandle);
			if (size < 0) {
				return; // error
			}
			listenBuf.limit(listenBuf.position() + size);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			// should not go wrong, the message is already waiting for us
			e.printStackTrace();
			return;
		}
		MxAddress sender = MxAddress.fromByteBuffer(listenBuf);
		WriteChannel wc = getWriteChannel(WriteChannel.createString(sender,
				Matching.getPort(matchData)));

		if (wc != null) {
			wc.receiverClosed();
		}
	}

	private void senderClosedConnection(long matchData) {
		ChannelManager cm;
		try {
			JavaMx.recv(null, 0, 0, endpointNumber, listenHandle, matchData);
			JavaMx.wait(endpointNumber, listenHandle);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			// should not go wrong, the message is already waiting for us
			e.printStackTrace();
		}
		cm = managers.get(Matching.getReceiveManager(matchData));
		if (cm == null) {
			return; // bogus message, discard is
		}
		cm.senderClosedConnection(Matching.getChannel(matchData));
	}

	private void listen(long matchData) throws IOException {
		// TODO catch exceptions
		ConnectionRequest request = null;

		listenBuf.clear();
		int msgSize = 0;
		try {
			JavaMx.recv(listenBuf, listenBuf.position(), listenBuf.remaining(),
					endpointNumber, listenHandle, matchData);
			msgSize = JavaMx.wait(endpointNumber, listenHandle);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new MxException("error");
		}
		if (msgSize < 0) {
			// TODO error
			throw new MxException("error");
		}
		listenBuf.limit(msgSize);
		MxAddress source = MxAddress.fromByteBuffer(listenBuf);
		if (source == null) {
			return;
		}
		byte[] descriptor = new byte[listenBuf.remaining()];
		listenBuf.get(descriptor);
		request = new ConnectionRequest(source, descriptor);

		listener.newConnection(request);
		// TODO read request
		switch (request.status) {
		case ConnectionRequest.ACCEPTED:
			// accept() already sent the reply message
			return;
		case ConnectionRequest.PENDING:
			// user did not accept it, so we reject it
			request.reject();
		case ConnectionRequest.REJECTED:
			// TODO send(Reply);
			int link = JavaMx.links.getLink();
			if (!JavaMx.connect(sendEndpointNumber, link, source.nicId,
					source.endpointId, IBIS_FILTER, 1000)) {
				// error, stop
				JavaMx.links.releaseLink(link);
				return;
			}
			listenBuf.clear();
			listenBuf.put(Connection.REJECT);
			listenBuf.put(request.replyMessage, 0, request.msgSize);
			listenBuf.flip();
			JavaMx.sendSynchronous(listenBuf, listenBuf.position(), listenBuf
					.remaining(), sendEndpointNumber, link, listenHandle,
					Matching.PROTOCOL_CONNECT_REPLY);
			msgSize = JavaMx.wait(sendEndpointNumber, listenHandle, 1000);
			if (msgSize < 0) {
				// timeout
				JavaMx.cancel(sendEndpointNumber, listenHandle); // TODO
			}
			JavaMx.links.releaseLink(link);
			listenBuf.clear();
			return;
		}
	}

	protected boolean accept(ChannelManager cm, ConnectionRequest request,
			long matchInfo) {
		// TODO check listenBuf
		int link = JavaMx.links.getLink();
		if (!JavaMx.connect(sendEndpointNumber, link, request.getSourceAddress().nicId,
				request.getSourceAddress().endpointId, IBIS_FILTER, 1000)) {
			// error, stop
			JavaMx.links.releaseLink(link);
			// TODO EXCEPTION
			return false;
		}

		listenBuf.clear();
		listenBuf.put(Connection.ACCEPT);
		listenBuf.putInt(Matching.getPort(matchInfo));
		listenBuf.put(request.replyMessage, 0, request.msgSize);
		listenBuf.flip();
		JavaMx.sendSynchronous(listenBuf, listenBuf.position(), listenBuf
				.remaining(), sendEndpointNumber, link, listenHandle,
				Matching.PROTOCOL_CONNECT_REPLY);
		int msgSize = -1;
		try {
			msgSize = JavaMx.wait(sendEndpointNumber, listenHandle, 1000);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (msgSize < 0) {
			// TODO timeout
			try {
				cm.getChannel(Matching.getChannel(matchInfo)).close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JavaMx.cancel(sendEndpointNumber, listenHandle); // TODO
			JavaMx.links.releaseLink(link);
			return false;
		}
		JavaMx.links.releaseLink(link);
		return true;
	}

	public synchronized void close() {
		closing = true;
		// TODO check for channelManagers and listen thread to finish??
		JavaMx.handles.releaseHandle(listenHandle);
		JavaMx.handles.releaseHandle(connectHandle);
		closed = true;
	}

	protected int endpointNumber() {
		return endpointNumber;
	}

	protected synchronized void addWriteChannel(WriteChannel wc) {
		writeChannels.put(wc.toString(), wc);
	}

	protected synchronized WriteChannel removeWriteChannel(String identifier) {
		return writeChannels.remove(identifier);
	}

	protected synchronized WriteChannel getWriteChannel(String identifier) {
		return writeChannels.get(identifier);
	}

	protected synchronized void sendCloseMessage(ReadChannel channel) {
		MxAddress target = channel.getSource();

		int link = JavaMx.links.getLink();
		if (JavaMx.connect(sendEndpointNumber, link, target.nicId, target.endpointId,
				IBIS_FILTER) == false) {
			// TODO exception, target not found
			JavaMx.links.releaseLink(link);
			return;
		}

		connectBuf.clear();
		connectBuf.put(myAddress.toBytes());
		connectBuf.flip();
		try {
			JavaMx.sendSynchronous(connectBuf, connectBuf.position(),
					connectBuf.remaining(), sendEndpointNumber, link, connectHandle,
					Matching.construct(Matching.PROTOCOL_CLOSE, channel
							.getPort()));

			JavaMx.wait(sendEndpointNumber, connectHandle, 1000);
		} catch (MxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JavaMx.disconnect(link);
		JavaMx.links.releaseLink(link);

	}

	public MxAddress getMyAddress() {
		return myAddress;
	}

}
