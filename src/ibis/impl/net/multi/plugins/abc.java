package ibis.impl.net.multi.plugins;

import ibis.impl.net.NetIbisIdentifier;
import ibis.impl.net.multi.MultiPlugin;

import ibis.util.IPUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;



/**
 * Provide an example of multiprotocol's driver plugin.
 */
public final class abc implements MultiPlugin {
        public abc() {};

        //
        public String getSubContext(boolean 		isOutgoing,
                                    NetIbisIdentifier  	localId,
                                    NetIbisIdentifier  	remoteId,
                                    ObjectOutputStream	os,
                                    ObjectInputStream 	is) throws IOException {
                String subContext = null;

		InetAddress localHostAddr  = IPUtils.getLocalHostAddress();
		InetAddress remoteHostAddr = null;

		try {
			if (isOutgoing) {
				os.writeObject(localHostAddr);
				os.flush();
				remoteHostAddr = (InetAddress)is.readObject();
			} else {
				remoteHostAddr = (InetAddress)is.readObject();
				os.writeObject(localHostAddr);
				os.flush();
			}
		} catch (ClassNotFoundException e) {
			throw new Error("Cannot find class InetAddress", e);
		}

		if (localId.equals(remoteId)) {
			subContext = "process";
		} else {
			byte [] l = localHostAddr.getAddress();
			byte [] r = remoteHostAddr.getAddress();
			int n = 0;

			while (n < 4 && l[n] == r[n])
				n++;

			switch (n) {
			case 4:
				{
					subContext = "node";
					break;
				}

			case 3:
				{
					subContext = "net_c";
					break;
				}

			case 2:
				{
					subContext = "net_b";
					break;
				}

			case 1:
				{
					subContext = "net_a";
					break;
				}

			default:
				{
					subContext = "internet";
					break;
				}
			}
		}
System.err.println("abc plugin context " + subContext);

                return subContext;
        }
}

