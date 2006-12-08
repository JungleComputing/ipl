/* $Id$ */

package ibis.impl.nameServer;

import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.StaticProperties;

import java.io.IOException;
import java.util.Properties;

/** 
 * This class defines the API between an Ibis implementation and the nameserver.
 * This way, an Ibis implementation can dynamically load any nameserver
 * implementaton.
 */
public abstract class NameServer implements ibis.ipl.Registry {

    /** Call on exit of an ibis. */
    public abstract void leave() throws IOException;

    /** Used internally to initialize the nameserver **/
    protected abstract void init(Ibis ibis, boolean needsUpcalls)
            throws IOException, IbisConfigurationException;

    /** Method to obtain a sequence number */
    public abstract long getSeqno(String name) throws IOException;

    /** Method to load a nameserver implementation. **/
    public static NameServer loadNameServer(Ibis ibis) 
            throws IllegalArgumentException, IOException,
                            IbisConfigurationException {
        return loadNameServer(ibis, true);
    }

    /** Method to load a nameserver implementation. **/
    public static NameServer loadNameServer(Ibis ibis,
            boolean needsUpcalls) throws IllegalArgumentException,
                IOException, IbisConfigurationException {
        NameServer res = null;

        Properties p = System.getProperties();
        String nameServerName = p.getProperty(NSProps.s_impl);
        if (nameServerName == null) {
            // String rank = p.getProperty("ibis.pool.host_number");
            // if (rank == null || Integer.parseInt(rank) == 0) {
            //     System.err.println("property ibis.name_server.impl not set, "
            //             + "using TCP nameserver");
            // }
            nameServerName = "ibis.impl.nameServer.tcp.NameServerClient";
        }

        Class c;
        try {
            c = Class.forName(nameServerName);
        } catch (ClassNotFoundException t) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis name server " + t);
        }

        try {
            res = (NameServer) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis name server " + e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis name server " + e2);
        }

        res.init(ibis, needsUpcalls);
        return res;
    }
}
