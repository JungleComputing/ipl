package ibis.ipl.impl.mx;

import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;

import org.apache.log4j.Logger;

class MxReceivePortConnectionInfo extends
		ibis.ipl.impl.ReceivePortConnectionInfo {
	private static Logger logger = Logger.getLogger(MxReceivePortConnectionInfo.class);
	
	protected MxId<MxReceivePortConnectionInfo> channelId;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxSimpleDataInputStream dataIn) throws IOException {
		super(origin, rp, dataIn);
		this.channelId = rp.connectionManager.get();
		if(channelId == null) {
			throw new IOException("Could not get a connection ID");
		}
		//TODO: find a better place for this, and call it when new connections are formed
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
		channelId.remove();
	}

	
}
