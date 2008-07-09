package ibis.ipl.impl.mx;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortConnectionInfo;

class MxSendPortConnectionInfo extends SendPortConnectionInfo {

	private static Logger logger = Logger.getLogger(MxSendPortConnectionInfo.class);
	
	MxScatteringBufferedDataOutputStream mxsdos;
	WriteChannel connection;
//	private MxAddress address;
	
	MxSendPortConnectionInfo(MxSendPort port, ReceivePortIdentifier target, WriteChannel connection) {
		super(port, target);
		mxsdos = port.scatteringStream;
		this.connection = connection;
		mxsdos.add(connection);
		//logger.debug("connection added");
	}

	@Override
	public void closeConnection() throws IOException {
		mxsdos.remove(connection);
		connection.close();
	}
}
