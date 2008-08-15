package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {
	
	MxScatteringDataOutputStream mxsdos;
	WriteChannel connection;
	
	MxSendPortConnectionInfo(MxSendPort port, ReceivePortIdentifier target, WriteChannel connection) {
		super(port, target);
		mxsdos = port.scatteringStream;
		this.connection = connection;
		mxsdos.add(connection);
	}

	@Override
	public void closeConnection() throws IOException {
		mxsdos.remove(connection);
		// FIXME DEBUG: fixing closing channels
		connection.close();
	}
}
