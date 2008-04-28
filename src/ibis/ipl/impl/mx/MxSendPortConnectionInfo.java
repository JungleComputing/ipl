package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {

	MxSendPortConnectionInfo(SendPort port, ReceivePortIdentifier target) {
		super(port, target);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void closeConnection() throws IOException {
		// TODO Auto-generated method stub

	}

}
