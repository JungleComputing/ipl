package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.PortType;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReceivePortIdentifier;
import ibis.ipl.impl.SendPort;
import ibis.ipl.impl.SendPortConnectionInfo;
import ibis.ipl.impl.WriteMessage;


class MxSendPort extends SendPort {

	MxSendPort(Ibis ibis, PortType type, String name,
			SendPortDisconnectUpcall connectUpcall, Properties properties)
			throws IOException {
		super(ibis, type, name, connectUpcall, properties);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.SendPort#announceNewMessage()
	 */
	@Override
	protected void announceNewMessage() throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.SendPort#closePort()
	 */
	@Override
	protected void closePort() throws IOException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.SendPort#doConnect(ibis.ipl.impl.ReceivePortIdentifier, long, boolean)
	 */
	@Override
	protected SendPortConnectionInfo doConnect(ReceivePortIdentifier receiver,
			long timeout, boolean fillTimeout) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.SendPort#handleSendException(ibis.ipl.impl.WriteMessage, java.io.IOException)
	 */
	@Override
	protected void handleSendException(WriteMessage w, IOException e) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see ibis.ipl.impl.SendPort#sendDisconnectMessage(ibis.ipl.impl.ReceivePortIdentifier, ibis.ipl.impl.SendPortConnectionInfo)
	 */
	@Override
	protected void sendDisconnectMessage(ReceivePortIdentifier receiver,
			SendPortConnectionInfo c) throws IOException {
		// TODO Auto-generated method stub

	}

}
