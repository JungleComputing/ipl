package ibis.ipl.impl.mx;

import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

class MxReceivePortConnectionInfo extends
		ReceivePortConnectionInfo implements 
		Identifiable<MxReceivePortConnectionInfo> {
	private static Logger logger = Logger.getLogger(MxReceivePortConnectionInfo.class);
	
	protected short channelId;
	protected IdManager<MxReceivePortConnectionInfo> channelManager;
	boolean senderClosed = false;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxSimpleDataInputStream dataIn) throws IOException {
		super(origin, rp, dataIn);
		setIdManager(rp.channelManager); 
		try {
			channelId = channelManager.insert(this);
		} catch (Exception e) {
			// TODO error: out of IDs
			//e.printStackTrace();
			throw new IOException("Out of channel IDs");
		}
	}
	
	boolean poll() throws IOException {
		return ((MxSimpleDataInputStream)dataIn).available() > 0;
	}	
	
	//blocking
	void receive() throws IOException {
		if (logger.isDebugEnabled()) {
			logger.debug("receiving");
		}
		if (in == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("newstream");
			}
            newStream();
        }
		// TODO implement
		if	(((MxSimpleDataInputStream)dataIn).WaitUntilAvailable(0) >= 0) { // message available
			if (logger.isDebugEnabled()) {
				logger.debug("message found!");
			}
            message.setFinished(false);
            ((MxReceivePort)port).messageArrived(message);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("no message found!");
			}
			throw new IOException("Error polling for message");
		}
	}

	@Override
	public void close(Throwable e) {
		super.close(e);
		channelManager.remove(channelId);
	}

	public void senderClose() {
		//FIXME Maybe some messages are still waiting for reception in the channel
		//FIXME Do we have to register this channel as closed at the ReceivePort when it is really closed?
		synchronized(this) {
			senderClosed = true;
		}
		if(((MxSimpleDataInputStream)dataIn).channel.senderClose()) {
			//no data left in channel
			close(null);
		}
	}
	
	public void receiverClose() {
		// When the sender has already closed, we have to close too	
		synchronized(this) {
			// only close when the sender is closed already, other wise, wait for the sender
			if(senderClosed == true) {
				close(null);
			 } else {
				 ((MxSimpleDataInputStream)dataIn).channel.receiverClose();
			 }
		}
	}
	
	public IdManager<MxReceivePortConnectionInfo> getIdManager() {
		return channelManager;
	}

	public short getIdentifier() {
		return channelId;
	}

	public void setIdManager(IdManager<MxReceivePortConnectionInfo> manager) {
		channelManager = manager;
		
	}

	public void setIdentifier(short id) {
		channelId = id;
	}
	
}
