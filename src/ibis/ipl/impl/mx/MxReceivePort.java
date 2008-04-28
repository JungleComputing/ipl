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
import ibis.ipl.impl.ReceivePort;

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

}
