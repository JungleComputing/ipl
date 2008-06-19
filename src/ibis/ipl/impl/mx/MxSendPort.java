package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;


public class MxSendPort extends SendPort {

	private static Logger logger = Logger.getLogger(MxSendPort.class);
	
	protected MxChannelFactory factory;
	private MxSimpleDataOutputStream simpleStream;
	
	MxSendPort(MxIbis ibis, PortType type, String name,
			SendPortDisconnectUpcall connectUpcall, Properties properties)
			throws IOException {
		super(ibis, type, name, connectUpcall, properties);
		factory = ibis.factory;
		// TODO Choose endianness dynamically
		
		simpleStream = new MxSimpleDataOutputStream(null, ByteOrder.nativeOrder());
		initStream(simpleStream); 
		// or something like this?
	}

	@Override
	protected void announceNewMessage() throws IOException {
		/* TODO deal with sequencing */
        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
	}

	@Override
	protected void closePort() throws IOException {
		/* 
		 * Send a msg with DISCONNECT to all connected receiveports
		 * Close out and dataOut streams
		 */
		// for all receiveports: disconnect(ReceivePortIdentifier)
		out.close();
		dataOut.close(); //throws an exception at MxSimpleDataOutputstream
		for(SendPortConnectionInfo spci: connections()) {
			try {
				spci.closeConnection();
			} catch(IOException e) {
				// TODO ignore for now
				// when an exception occurs at some connection, we still want to close the other connections
			}
		}
	}

	@Override
	protected MxSendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		/* 
		 * send CONNECT to receiveport
		 * negotiate port number
		 * construct the SendPortConnectionInfo
		 */
		// TODO timeouts
		MxSendPortConnectionInfo connectionInfo = new MxSendPortConnectionInfo(this, receiver, factory.connect(this, receiver, timeout));
		initStream(new MxSimpleDataOutputStream(connectionInfo.connection, ByteOrder.nativeOrder())); // or something like this
		// TODO mulit channel support
		return connectionInfo;
	}

	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		//TODO
		logger.debug("handleSendException", e);
	}

	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver,
			SendPortConnectionInfo c) throws IOException {
		
		/*
		 * send a DISCONNECT message
		 */
		// don't have one yet
	}
}
