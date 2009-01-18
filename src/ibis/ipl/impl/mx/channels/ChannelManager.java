package ibis.ipl.impl.mx.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ChannelManager {
	private static Logger logger = Logger.getLogger(ChannelManager.class);
	
	private HashMap<Short, ReadChannel> readChannels;
	private Collection<ReadChannel> channelCollection;
	private int nextKey = 0;

	private MxSocket mxSocket;
	private short managerId = 0;
	private Object owner = null;
	private long selectMatch, selectMask;
	private ByteBuffer selectBuffer = ByteBuffer.allocateDirect(Long.SIZE/8).order(ByteOrder.nativeOrder());

	protected ChannelManager(MxSocket e, short managerId) {
		mxSocket = e;
		this.managerId = managerId;
		selectMatch = Matching.construct(Matching.PROTOCOL_DATA, managerId,
				(short) 0);
		selectMask = ~Matching.CHANNEL_MASK;
		readChannels = new HashMap<Short, ReadChannel>();
		channelCollection = readChannels.values();
		if (logger.isDebugEnabled()) {
			logger.debug("ChannelManager " + managerId + " started");
		}
	}

	public Connection connect(MxAddress target, byte[] descriptor)
			throws MxException {
		// TODO catch exception and handle it
		return mxSocket.connect(this, target, descriptor);
	}

	public ReadChannel select(long timeout) throws IOException {
		/*synchronized(this) {
			for(ReadChannel rc: channelCollection) { 
				if(rc.containsData()) { 
					return rc; 
				}
			}
		}*/

		selectBuffer.clear();
		int msgSize = JavaMx.select(mxSocket.endpointNumber(), timeout, selectMatch, selectMask, selectBuffer);
		if(msgSize < 0) {
			return null;
		}
		long matchData = selectBuffer.getLong();
		ReadChannel rc = getChannel(Matching.getChannel(matchData));
		rc.isSelected(msgSize);
		return rc;
	}

	public ReadChannel accept(ConnectionRequest request) {
		long matchData;
		try {
			matchData = addConnection(request.getSourceAddress());
		} catch (MxException e) {
			// TODO handle this
			request.reject();
			return null;
		}

		// TODO check of het echt kan
		if (mxSocket.accept(this, request, matchData) == true) {
			request.accept();
		} else {
			request.reject();
		}
		return getChannel(Matching.getChannel(matchData));
	}

	/**
	 * @return the owner
	 */
	public Object getOwner() {
		return owner;
	}

	/**
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(Object owner) {
		this.owner = owner;
	}

	protected synchronized long addConnection(MxAddress source)
			throws MxException {
		for (int i = 0; i < Short.MAX_VALUE; i++) {
			nextKey = (nextKey % Short.MAX_VALUE) + 1; // 0 will not be used
			if (!readChannels.containsKey(nextKey)) {
				long matchData = Matching.construct(Matching.PROTOCOL_DATA,
						managerId, (short) nextKey);
				ReadChannel channel = new ReadChannel(this, source, mxSocket
						.endpointNumber(), matchData);
				readChannels.put((short) nextKey, channel);
				// listener.newConnectionRequest(channel);
				return matchData;
			}
		}
		throw new MxException("maximum number of connections reached");
	}

	protected synchronized ReadChannel getChannel(short channel) {
		return readChannels.get(channel);
	}

	protected synchronized void readChannelCloses(short channelId) {
		// should only be called by the readChannels
		readChannels.remove(channelId);
	}

	protected void senderClosedConnection(short channel) {
		// should only be called by the MXSocket
		ReadChannel rc = readChannels.get(channel);
		if (rc == null) {
			return;
		}
		rc.senderClosedConnection();
	}

	protected void sendCloseMessage(ReadChannel channel) {
		mxSocket.sendCloseMessage(channel);
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}
