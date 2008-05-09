package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;


public class MxSendPort extends SendPort {

	protected MxChannelFactory factory;
	
	MxSendPort(MxIbis ibis, PortType type, String name,
			SendPortDisconnectUpcall connectUpcall, Properties properties)
			throws IOException {
		super(ibis, type, name, connectUpcall, properties);
		factory = ibis.factory;
		// TODO Auto-generated constructor stub
		
		initStream(new MxDataOutputStream(null)); // or something like this
	}

	@Override
	protected void announceNewMessage() throws IOException {
		// TODO Auto-generated method stub
		/* 
		 * deal with sequencing
		 * Don't think it is needed to announce a message
		 */
        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
	}

	@Override
	protected void closePort() throws IOException {
		// TODO
		/* 
		 * Send a msg with DISCONNECT to all connected receiveports
		 * Close out and dataOut streams
		 */
		// for all receiveports: disconnect(ReceivePortIdentifier)
		// TODO: maybe do something smarter to improve performance later
		
	}

	@Override
	protected MxSendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		/* 
		 * send CONNECT to receiveport
		 * negotiate port number
		 * construct the SendPortConnectionInfo
		 */
		MxSendPortConnectionInfo connectionInfo = new MxSendPortConnectionInfo(this, receiver);
		connectionInfo.connect();
		return connectionInfo;
	}

	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver,
			SendPortConnectionInfo c) throws IOException {
		// TODO Auto-generated method stub
		/*
		 * send a DISCONNECT message
		 */
		
	}
}
