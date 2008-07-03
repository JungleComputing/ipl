package ibis.ipl.impl.mx;

import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

class MxReceivePortConnectionInfo extends
		ReceivePortConnectionInfo implements 
		Identifiable<MxReceivePortConnectionInfo> {
	private static Logger logger = Logger.getLogger(MxReceivePortConnectionInfo.class);
	
	protected short channelId;
	protected IdManager<MxReceivePortConnectionInfo> channelManager;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxDataInputStream dataIn) throws IOException {
		super(origin, rp, dataIn);
	}
	
	boolean poll() throws IOException {
		return ((MxDataInputStream)dataIn).available() > 0;
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
		if	(((MxDataInputStream)dataIn).waitUntilAvailable(0) >= 0) { // message available
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

	
	/* (non-Javadoc)
	 * @see ibis.ipl.impl.ReceivePortConnectionInfo#upcallCalledFinish()
	 */
	@Override
	protected void upcallCalledFinish() {
		super.upcallCalledFinish();
		ThreadPool.createNew((MxReceivePort) port, "MxReceivePort Upcall Thread");
	}

	@Override
	public void close(Throwable e) {
		//TODO really synchronize this one?
		// note: dataIn closes the channel
		super.close(e);
	}

	public void senderClose() {
		//FIXME hack
		ReadChannel channel = ((MxSimpleDataInputStream)dataIn).channel; 
		
		if( channel instanceof MxLocalChannel) {
			close(null);
			return;
		} else {
			if(((MxReadChannel)channel).senderClose()) {
				//	no data left in channel
				close(null);
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

	protected void receivePortcloses() {
		// TODO maybe can be regarded as a hack?
		((MxSimpleDataInputStream)dataIn).channel.close();
	}
	
}
