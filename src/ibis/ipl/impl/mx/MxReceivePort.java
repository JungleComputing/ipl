/**
 * 
 */
package ibis.ipl.impl.mx;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.ReadMessage;
import ibis.ipl.impl.ReceivePort;
import ibis.ipl.impl.ReceivePortConnectionInfo;
import ibis.ipl.impl.SendPortIdentifier;

/**
 * @author Timo van Kessel
 *
 */
class MxReceivePort extends ReceivePort {

	/**
	 * @param ibis
	 * @param type
	 * @param name
	 * @param upcall
	 * @param connectUpcall
	 * @param properties
	 * @throws IOException
	 */
	MxReceivePort(Ibis ibis, PortType type, String name,
			MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
			Properties properties) throws IOException {
		super(ibis, type, name, upcall, connectUpcall, properties);
		// TODO Auto-generated constructor stub
	}
		
	/* ********************************************
	 * Methods that can be overridden optionally:
	 *********************************************/

	@Override
	public synchronized void addInfo(SendPortIdentifier id,
			ReceivePortConnectionInfo info) {
		// TODO Auto-generated method stub
		super.addInfo(id, info);
	}

	@Override
	public synchronized ReceivePortConnectionInfo[] connections() {
		// TODO Auto-generated method stub
		return super.connections();
	}

	@Override
	public void doUpcall(ReadMessage msg) {
		// TODO Auto-generated method stub
		super.doUpcall(msg);
	}

	@Override
	public synchronized ReceivePortConnectionInfo getInfo(SendPortIdentifier id) {
		// TODO Auto-generated method stub
		return super.getInfo(id);
	}

	@Override
	public ReadMessage getMessage(long timeout) throws IOException {
		// TODO Auto-generated method stub
		return super.getMessage(timeout);
	}

	@Override
	public void lostConnection(SendPortIdentifier id, Throwable e) {
		// TODO Auto-generated method stub
		super.lostConnection(id, e);
	}

	@Override
	public synchronized ReceivePortConnectionInfo removeInfo(
			SendPortIdentifier id) {
		// TODO Auto-generated method stub
		return super.removeInfo(id);
	}

	@Override
	public synchronized void closePort(long timeout) {
		// TODO Auto-generated method stub
		super.closePort(timeout);
	}

	@Override
	protected ReadMessage doPoll() throws IOException {
		// TODO Auto-generated method stub
		return super.doPoll();
	}

	@Override
	public synchronized void finishMessage(ReadMessage r, IOException e) {
		// TODO Auto-generated method stub
		super.finishMessage(r, e);
	}

	@Override
	public synchronized void finishMessage(ReadMessage r, long cnt) {
		// TODO Auto-generated method stub
		super.finishMessage(r, cnt);
	}

	@Override
	public void messageArrived(ReadMessage msg) {
		// TODO Auto-generated method stub
		super.messageArrived(msg);
	}
}
