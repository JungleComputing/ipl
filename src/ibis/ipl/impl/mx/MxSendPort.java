package ibis.ipl.impl.mx;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Properties;

import org.apache.log4j.Logger;

import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;

public class MxSendPort extends SendPort {

	private static Logger logger = Logger.getLogger(MxSendPort.class);
	
	protected MxChannelFactory factory;
	protected MxScatteringDataOutputStream scatteringStream;
	private boolean reliable;
	
	MxSendPort(MxIbis ibis, PortType type, String name,
			SendPortDisconnectUpcall connectUpcall, Properties properties, boolean reliable)
			throws IOException {
		super(ibis, type, name, connectUpcall, properties);
		factory = ibis.factory;
		// TODO Choose endianness dynamically
		
		scatteringStream = new MxScatteringDataOutputStream();
		this.reliable = reliable;
		initStream(scatteringStream); 
		// or something like this?
	}

	@Override
	protected void announceNewMessage() throws IOException {
		/* TODO deal with sequencing */
		logger.debug("announcing message");
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
	}

	@Override
	protected MxSendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		/* 
		 * send CONNECT to receiveport
		 * negotiate port number
		 * construct the SendPortConnectionInfo
		 */
		logger.debug("connecting...");
		MxSendPortConnectionInfo connectionInfo = new MxSendPortConnectionInfo(this, receiver, factory.connect(this, receiver, timeout, fillTimeout, reliable));
		logger.debug("connected!");
		initStream(scatteringStream); // or something like this
		logger.debug("new stream initialized");
		// TODO multi channel support. call above will go wrong in that case. MXSDOS should support multiple channels?
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
