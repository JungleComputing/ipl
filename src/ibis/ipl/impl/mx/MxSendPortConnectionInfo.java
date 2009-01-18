package ibis.ipl.impl.mx;

import java.io.IOException;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.mx.channels.ScatteringOutputStream;
import ibis.ipl.impl.mx.channels.WriteChannel;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {
	
	ScatteringOutputStream mxsdos;
	WriteChannel connection;
	
	MxSendPortConnectionInfo(MxSendPort port, ReceivePortIdentifier target, WriteChannel connection) {
		super(port, target);
		mxsdos = port.scatteringStream;
		this.connection = connection;
		mxsdos.add(connection);
	}

	@Override
	public void closeConnection() throws IOException {
		try {
			mxsdos.remove(connection);
		} catch(IOException e) {
			//ignore
		}
		// FIXME DEBUG: fixing closing channels
		connection.close();
	}
}
