package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {

	MxWriteChannel connection;
//	private MxAddress address;
	
	MxSendPortConnectionInfo(MxSendPort port, ReceivePortIdentifier target, MxWriteChannel connection) {
		super(port, target);
		this.connection = connection;
		// TODO generate an MxAddress from target (Why do we need an MxAddress?)
	}

	@Override
	public void closeConnection() throws IOException {
		// TODO Do not send disconnect message: done at MxSendChannel
		connection.close();
	}
}
