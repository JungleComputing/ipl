package ibis.ipl.impl.mx;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.Conversion;
import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.ipl.impl.WriteMessage;
import ibis.ipl.impl.mx.channels.ScatteringOutputStream;
import ibis.ipl.impl.mx.channels.WriteChannel;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MxSendPort extends SendPort implements MxProtocol {

	private static Logger logger = Logger.getLogger(MxSendPort.class);
	
	protected ScatteringOutputStream scatteringStream;
	private BufferedArrayOutputStream bufferedStream;

	
	MxSendPort(MxIbis ibis, PortType type, String name,
			SendPortDisconnectUpcall connectUpcall, Properties properties)
			throws IOException {
		super(ibis, type, name, connectUpcall, properties);
		
		scatteringStream = new ScatteringOutputStream();
		bufferedStream = new BufferedArrayOutputStream(scatteringStream, 4096);
		initStream(bufferedStream); 
		// TODO or something like this?
	}

    SendPortIdentifier getIdent() {
        return ident;
    }
	
	@Override
	protected void announceNewMessage() throws IOException {
		//logger.debug("announcing message");
		out.writeByte(NEW_MESSAGE);
        if (type.hasCapability(PortType.COMMUNICATION_NUMBERED)) {
            out.writeLong(ibis.registry().getSequenceNumber(name));
        }
	}

	@Override
	protected void closePort() throws IOException {
		// taken from TcpIbis
		//logger.debug("Closing SendPort...");
		try {
            out.writeByte(CLOSE_ALL_CONNECTIONS);
    		out.close();
    		dataOut.close();
    	} catch (Throwable e) {
            // ignored
        }
        out = null;
        //logger.debug("SendPort Closed!");
	}

	@Override
	protected MxSendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		//logger.debug("connecting...");
		//bufferedStream.flush();
		WriteChannel wc = 
    		((MxIbis) ibis).connect(this, receiver, (int) timeout,
                fillTimeout);
		MxSendPortConnectionInfo spci = new MxSendPortConnectionInfo(this, receiver, wc);
		
		//logger.debug("connected!");
		if (out != null) {
            out.writeByte(NEW_RECEIVER);
        }
		initStream(bufferedStream);
		return spci;
	}

	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		//TODO something?
		//logger.debug("handleSendException", e);
	}

	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver,
			SendPortConnectionInfo c) throws IOException {		
		// taken from TcpIbis
		out.writeByte(CLOSE_ONE_CONNECTION);
        byte[] receiverBytes = receiver.toBytes();
        byte[] receiverLength = new byte[Conversion.INT_SIZE];
        Conversion.defaultConversion.int2byte(receiverBytes.length,
                receiverLength, 0);
        out.writeArray(receiverLength);
        out.writeArray(receiverBytes);
        out.flush();
	}
}
