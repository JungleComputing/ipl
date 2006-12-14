/* $Id: Registry.java 4880 2006-12-08 09:06:32Z ceriel $ */

package ibis.impl;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.StaticProperties;

import java.io.IOException;
import java.util.Properties;

/** 
 * This class defines the API between an Ibis implementation and the nameserver.
 * This way, an Ibis implementation can dynamically load any nameserver
 * implementaton.
 */
public abstract class Registry implements ibis.ipl.Registry {

    /** Call on exit of an ibis. */
    public abstract void leave() throws IOException;

    /** Used internally to initialize the nameserver **/
    public abstract IbisIdentifier init(Ibis ibis, boolean needsUpcalls,
            byte[] data) throws IOException, IbisConfigurationException;

    /** Method to obtain a sequence number */
    public abstract long getSeqno(String name) throws IOException;

    /** Method to load a nameserver implementation. **/
    public static Registry loadRegistry(Ibis ibis)
            throws IllegalArgumentException {
        Registry res = null;
        // TODO: fix properties
        String nameServerName = System.getProperty("ibis.name_server.impl");
        if (nameServerName == null) {
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
            res = (Registry) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis name server " + e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis name server " + e2);
        }

        return res;
    }
}
