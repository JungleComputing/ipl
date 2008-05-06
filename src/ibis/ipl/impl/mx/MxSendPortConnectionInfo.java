package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {

	private boolean connected;
	MxWriteChannel connection;
//	private MxAddress address;
	
	MxSendPortConnectionInfo(MxSendPort port, ReceivePortIdentifier target) {
		super(port, target);
		connected = false;
		connection = null;
		// TODO generate an MxAddress from target (Why do we need an MxAddress?)
	}

	@Override
	public void closeConnection() throws IOException {
		// TODO Auto-generated method stub

		//Do not send disconnect message: already done at MxSendPort
		connected = false;
		connection = null;
	}
	
	boolean connect() throws IOException {
		// get MxAddress and connect
		connection = ((MxSendPort)port).factory.connect((MxSendPort)port, target, 0);
		
		//TODO the rest
		connected = true;
		return connected;
	}	

}
