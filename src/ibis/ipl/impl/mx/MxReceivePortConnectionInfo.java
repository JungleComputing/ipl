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
	boolean portClosed = false;

	private MxChannelFactory factory;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxDataInputStream dataIn, MxChannelFactory factory) throws IOException {
		super(origin, rp, dataIn);
		this.factory = factory;
	}
	
	boolean poll() throws IOException {
		return ((MxDataInputStream)dataIn).available() > 0;
	}	
	
	//blocking
	void receive() throws IOException {
		logger.debug("receiving");
		if (in == null) {
            newStream();
        }
		if	(((MxDataInputStream)dataIn).waitUntilAvailable(0) >= 0) { // message available
            message.setFinished(false);
            ((MxReceivePort)port).messageArrived(message);
		} else {
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
		logger.debug("close()");
		// note: dataIn closes the channel
		super.close(e);
	}

	public void senderClose() {
		//FIXME hack
		ReadChannel channel = ((MxDataInputStreamImpl)dataIn).channel; 
		if(channel instanceof MxLocalChannel) {
			logger.debug("sender closes local channel at receiver");
			close(null);
			return;
		} else {
			logger.debug("sender closes remote channel at receiver");
			if(((MxReadChannel)channel).senderClose()) {
				//	no data left in channel
				close(null);
				return;
			}
		}
		
		/*
		//TODO I suppose this will work, but it is not nice
		synchronized(this) {
			if(portClosed) {
				// receiveport also closed
				close(null);
			}
		}*/		
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
		logger.debug("receivePortcloses()");
		// TODO maybe can be regarded as a hack?
		//((MxSimpleDataInputStream)dataIn).channel.close();
		synchronized(this) {
			portClosed = true;
		}
		if(((MxDataInputStreamImpl)dataIn).channel instanceof MxLocalChannel) {
			logger.debug("Receiver closes local channel at receiver");
			close(null);
		} else {
			factory.sendCloseMessage(this);
			//	((MxBufferedDataInputStreamImpl)dataIn).channel.close();
		}
	}
	
}
