package ibis.ipl.impl.mx;

import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

class MxReceivePortConnectionInfo extends
		ibis.ipl.impl.ReceivePortConnectionInfo implements 
		Identifiable<MxReceivePortConnectionInfo> {
	private static Logger logger = Logger.getLogger(MxReceivePortConnectionInfo.class);
	
	protected short channelId = 0;
	protected IdManager<MxReceivePortConnectionInfo> idm = null;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxSimpleDataInputStream dataIn) throws IOException {
		super(origin, rp, dataIn);
		try {
			rp.channelManager.insert(this);
		} catch (Exception e) {
			// TODO error: out of IDs
			e.printStackTrace();
		}
	}
	
	boolean poll() throws IOException {
		return ((MxSimpleDataInputStream)dataIn).available() > 0;
	}	
	
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
		if(poll()) { // message available
            message.setFinished(false);
            ((MxReceivePort)port).messageArrived(message);
		}
	}

	@Override
	public void close(Throwable e) {
		super.close(e);
		idm.remove(channelId);
	}

	public IdManager<MxReceivePortConnectionInfo> getIdManager() {
		return idm;
	}

	public short getIdentifier() {
		return channelId;
	}

	public void setIdManager(IdManager<MxReceivePortConnectionInfo> manager) {
		idm = manager;
		
	}

	public void setIdentifier(short id) {
		channelId = id;
	}

	
}
