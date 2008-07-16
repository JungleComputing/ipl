package ibis.ipl.impl.mx;

import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

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
		
		scatteringStream = new MxScatteringDataOutputStream();
		this.reliable = reliable;
		initStream(scatteringStream); 
		// or something like this?
	}

	@Override
	protected void announceNewMessage() throws IOException {
		//logger.debug("announcing message");
        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
	}

	@Override
	protected void closePort() throws IOException {
		/* 
		 * Close out and dataOut streams
		 */
		out.close();
		dataOut.close(); // sends a disconnect message to the Channelfactory of the receiver at the channel level
	}

	@Override
	protected MxSendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		logger.debug("connecting...");
		MxSendPortConnectionInfo connectionInfo = new MxSendPortConnectionInfo(this, receiver, factory.connect(this, receiver, timeout, fillTimeout, reliable));
		logger.debug("connected!");
		initStream(scatteringStream);
		return connectionInfo;
	}

	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		//TODO something?
		logger.debug("handleSendException", e);
	}

	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver,
			SendPortConnectionInfo c) throws IOException {
		/*
		 * send a DISCONNECT message
		 */
		// TODO don't have one (yet)
	}
}
