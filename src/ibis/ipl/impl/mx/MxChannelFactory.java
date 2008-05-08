package ibis.ipl.impl.mx;

import ibis.ipl.PortMismatchException;
import ibis.ipl.PortType;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class MxChannelFactory implements Runnable {

	static final int IBIS_FILTER = 0xdada0001;
	
	private static Logger logger = Logger.getLogger(MxChannelFactory.class);

	MxAddress address;
	int handlerId;
	
	private MxIbis ibis;
	private boolean listening = false;

	public MxChannelFactory() {
		this.handlerId = JavaMx.newHandler(IBIS_FILTER);
		this.address = new MxAddress(JavaMx.getMyNicId(handlerId), JavaMx.getMyEndpointId(handlerId));
		ThreadPool.createNew(this, "MxChannelFactory");
	}

	/*	protected MxAddress getAddress(String hostname, int endpointId) {
		return new MxAddress(hostname, endpointId);
	}
	 */

	public MxWriteChannel connect(MxSendPort sp,
			ReceivePortIdentifier rpi, long timeoutMillis) throws IOException  {
		/*TODO:
		 * - send a "connect" message
		 * - read reply
		 * - make a writechannel if connection succesful
		 */

		IbisIdentifier id = (ibis.ipl.impl.IbisIdentifier) rpi.ibisIdentifier();
		MxAddress target;
		try {
			target = MxAddress.fromBytes(id.getImplementationData());
		} catch (Exception e) {
			throw new PortMismatchException("Could not create MxAddress from ReceivePortIdentifier.", rpi, e);
		}
		MxWriteChannel channel = new MxUnreliableWriteChannel(this, target, IBIS_FILTER);
		MxDataOutputStream mxdos = new MxDataOutputStream(channel);

		channel.setPort(0);
		channel.setProtocol(Matching.CONNECT);
		// FIXME When multiple connection requests arrive at the same time, message can be mixed up for requests consisting of multiple MX messages
		DataOutputStream d = new DataOutputStream(mxdos);
		d.writeUTF(rpi.name());
		sp.ident.writeTo(d);
		sp.type.writeTo(d);
		//TODO: write port number to be used to send response
		d.flush();

		return null;

	}

	public boolean close() {
		// TODO: close all channels
		// and stop listen thread
		listening = false;
		return JavaMx.closeHandler(handlerId);
	}

	/* interface benodigd voor send/receiveports */

	public void listen() {
		//TODO do listening in a thread?
	}

	public boolean close(MxWriteChannel conn) {
		//TODO: niet hier, maar channels zichzelf laten sluiten?
		return false;
	}

	public void run() {
		// a "Listen" thread
		listening = true;
		MxReadChannel channel;
		SendPortIdentifier spi = null;
		ReceivePortIdentifier rpi = null;
		MxReceivePortConnectionInfo info;
		MxDataInputStream mxdis;
		DataInputStream d;
		MxReceivePort port;

		String name;
		PortType capabilities = null;		

		while(listening) {
			/*TODO:
			 * read and check sender identifier, targeted ReceivePort, etc
			 * create a channel and the ReceiveportConnectionInfo
			 * Attach to the ReceivePort
			 */
			channel = new MxReadChannel(this);
			channel.setPort(0);
			channel.setProtocol(Matching.CONNECT);
			// FIXME When multiple connection requests arrive at the same time, message can be mixed up for requests consisting of multiple MX messages

			mxdis = new MxDataInputStream(channel);
			d = new DataInputStream(mxdis);

			try {
				name = d.readUTF();

				spi = new SendPortIdentifier(d);
				capabilities = new PortType(d);
				rpi = new ReceivePortIdentifier(name, ibis.ident);
				port = (MxReceivePort) ibis.findReceivePort(name);
				//TODO read the port number for sending the response message

				if (port == null) {
					logger.error("could not find receiveport, connection denied");
					//TODO
					//accumulator.writeByte(ReceivePort.DENIED);
					//accumulator.flush();
					//channel.close();
					return;
				}

				if (logger.isDebugEnabled()) {
					logger.debug("giving new connection to receiveport " + rpi);
				}

				// register connection with ReceivePort
				byte reply = port.connectionAllowed(spi, capabilities);

				if (reply == ReceivePort.ACCEPTED) {
					channel.setProtocol(Matching.DATA);
					//TODO: assign a port number
					channel.setPort(1); //TODO: find a mechanism for this
					mxdis.resetBytesRead();
					try {
						info = new MxReceivePortConnectionInfo(spi, port, mxdis);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						reply = ReceivePort.DENIED;
						// TODO clean up mess
					} // channel is now connected to the ReceivePort	
				}
				// TODO send reply
				//accumulator.writeByte(reply);
				//accumulator.flush();

				if (reply != ReceivePort.ACCEPTED) {
					//TODO clean up mess
					channel.close();
					if (logger.isInfoEnabled()) {
						logger.info("receiveport rejected connection");
					}
					return;
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
}
