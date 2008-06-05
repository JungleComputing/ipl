package ibis.ipl.impl.mx;

import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;

class MxReceivePortConnectionInfo extends
		ibis.ipl.impl.ReceivePortConnectionInfo {
	
	protected MxId<MxReceivePortConnectionInfo> channelId;
	
	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			MxReceivePort rp, MxSimpleDataInputStream dataIn) throws IOException {
		super(origin, rp, dataIn);
		this.channelId = rp.connectionManager.get();
		if(channelId == null) {
			throw new IOException("Could not get a connection ID");
		}
		//TODO: find a better place for this, and call it when new connections are formed
		newStream();
	}
	
	boolean poll() throws IOException {
		return ((MxSimpleDataInputStream)dataIn).available() > 0;
	}	
	
	void receive() throws IOException {
		dataIn.readByte();
	}

	@Override
	public void close(Throwable e) {
		super.close(e);
		channelId.remove();
	}

	
}
