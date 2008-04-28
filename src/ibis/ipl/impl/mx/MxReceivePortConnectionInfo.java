package ibis.ipl.impl.mx;

import ibis.io.DataInputStream;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.SendPortIdentifier;

import java.io.IOException;

class MxReceivePortConnectionInfo extends
		ibis.ipl.impl.ReceivePortConnectionInfo {

	MxReceivePortConnectionInfo(SendPortIdentifier origin,
			ReceivePort port, DataInputStream dataIn) throws IOException {
		super(origin, port, dataIn);
		// TODO Auto-generated constructor stub
	}

}
